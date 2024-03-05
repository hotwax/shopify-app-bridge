/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package co.hotwax.shopify.app

import groovy.transform.CompileStatic
import org.moqui.impl.context.ExecutionContextImpl;
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import org.moqui.impl.context.ExecutionContextFactoryImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.io.IOUtils
import org.moqui.impl.context.ContextJavaUtil

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/** shopify webhook HMAC verification  */
@CompileStatic
class ShopifyWebhookFilter implements Filter {
    protected final static Logger logger = LoggerFactory.getLogger(ShopifyWebhookFilter.class)

    protected FilterConfig filterConfig = null

    ShopifyWebhookFilter() { super() }

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig
    }

    @Override
    void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (!(req instanceof HttpServletRequest) || !(resp instanceof HttpServletResponse)) {
            chain.doFilter(req, resp); return
        }

        HttpServletRequest request = (HttpServletRequest) req
        HttpServletResponse response = (HttpServletResponse) resp

        ServletContext servletContext = req.getServletContext()

        ExecutionContextFactoryImpl ecfi = (ExecutionContextFactoryImpl) servletContext.getAttribute("executionContextFactory")
        // check for and cleanly handle when executionContextFactory is not in place in ServletContext attr
        if (ecfi == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "System is initializing, try again soon.")
            return
        }

        try {
            // Verify the incoming webhook request
            verifyIncomingWebhook(request, response, ecfi.getEci())
            chain.doFilter(req, resp)
        } catch(Throwable t) {
            logger.error("Error occurred in Shopify Webhook verification", t)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in Shopify webhook verification: ${t.toString()}")
        }
    }

    @Override void destroy() { }

    void verifyIncomingWebhook(HttpServletRequest request, HttpServletResponse response, ExecutionContextImpl ec) {

        String hmac = request.getHeader("X-Shopify-Hmac-SHA256")
        String shopDomain = request.getHeader("X-Shopify-Shop-Domain")
        String webhookTopic = request.getHeader("X-Shopify-Topic")
        String webhookId = request.getHeader("X-Shopify-Webhook-Id")

        String requestBody = IOUtils.toString(request.getReader());

        if (requestBody.length() == 0) {
            logger.warn("The request body for webhook ${webhookTopic} is empty for Shopify ${shopDomain}, cannot verify webhook")
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The Request Body is empty for Shopify webhook")
            return
        }
        request.setAttribute("webhookPayload", ContextJavaUtil.jacksonMapper.readValue(requestBody, Map.class))

        String pathInfo = request.getRequestURI();
        int startIndex = pathInfo.indexOf("webhook/") + "webhook/".length()
        int endIndex = pathInfo.indexOf("/", startIndex)
        if (startIndex < 0 || endIndex < 0) {
            logger.warn("Invalid webhook received. ClientId is missing in the URL ${pathInfo}")
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid webhook received. ClientId is missing in the URL ${pathInfo}")
            return
        }

        String clientId = pathInfo.substring(startIndex, endIndex)
        EntityList shopifyApps = ec.entityFacade.find("co.hotwax.shopify.app.ShopifyApp")
                .condition("clientId", clientId).useCache(true)
                .disableAuthz().list()
        if (!shopifyApps) {
            logger.warn("No Shopify app configured for clientId ${clientId}")
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No Shopify app configured for clientId ${clientId}")
            return
        }
        EntityValue shopifyApp = shopifyApps.get(0)
        Map result = ec.serviceFacade.sync().name("co.hotwax.shopify.app.ShopifyAdminServices.verify#Hmac")
                .parameters([message:requestBody, hmac:hmac, sharedSecret:shopifyApp.clientSecret])
                .disableAuthz().call()
        if (!result.validSignature) {
            logger.warn("The webhook ${webhookTopic} HMAC header did not match with the computed HMAC for Shopify ${shopDomain}")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "HMAC verification failed for Shopify ${shopDomain} for webhook ${webhookTopic}")
            return
        }
    }
}
