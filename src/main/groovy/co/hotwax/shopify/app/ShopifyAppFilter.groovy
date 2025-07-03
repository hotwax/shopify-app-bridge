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
import org.moqui.util.WebUtilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.stream.Collectors

/** shopify APP HMAC verification  */
@CompileStatic
class ShopifyAppFilter implements Filter {
    protected final static Logger logger = LoggerFactory.getLogger(ShopifyAppFilter.class)

    protected FilterConfig filterConfig = null

    ShopifyAppFilter() { super() }

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
            /*
                The HMAC verification procedure for authorization code grant is different from the procedure for verifying webhooks.
             */
            boolean validRequest = verifyShopifyAppRequest (request, ecfi.getEci())
            if (!validRequest) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "HMAC verification failed.")
                return
            }
            chain.doFilter(req, resp)
        } catch(Throwable t) {
            logger.error("Error occurred in Shopify Webhook verification", t)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in Shopify webhook verification: ${t.toString()}")
        }
    }

    @Override void destroy() { }

    boolean verifyShopifyAppRequest(HttpServletRequest request, ExecutionContextImpl ec) {
        //Map<String, Object> requestParamMap = new HashMap(ec.web?.getRequestParameters())
        Map<String, Object> requestParamMap = new TreeMap(WebUtilities.simplifyRequestParameters(request, false))
        String hmac = requestParamMap.remove("hmac");
        String clientId = requestParamMap.remove("clientId");
        String message = org.moqui.util.RestClient.parametersMapToString(requestParamMap)
        logger.info("App verification request received for clientId ${clientId} for shop ${requestParamMap.shop} with HMAC ${hmac} and message ${message}.")
        boolean validSignature = false;

        EntityList shopifyApps = ec.entityFacade.find("co.hotwax.shopify.app.ShopifyApp")
                .condition("clientId", clientId)
                .disableAuthz().list()
        if (shopifyApps.size() > 0) {
            EntityValue shopifyApp = shopifyApps.get(0)
            Map result = ec.serviceFacade.sync().name("co.hotwax.shopify.app.ShopifyAdminServices.verify#Hmac")
                    .parameters([message:message, hmac:hmac, sharedSecret:shopifyApp.clientSecret, digest: 'Hex'])
                    .disableAuthz().call()
            // As per shopify recommendation storing the oldClientSecret
            /*
                Don't delete your old client secret until you've requested new access tokens for every token stored by your app. Users might not be able to open your app if you delete a client secret that still has tokens associated with it.
             */
            if (!result.validSignature && shopifyApp.oldClientSecret) {
                result = ec.serviceFacade.sync().name("co.hotwax.shopify.app.ShopifyAdminServices.verify#Hmac")
                        .parameters([message:message, hmac:hmac, sharedSecret:shopifyApp.oldClientSecret, digest: 'Hex'])
                        .disableAuthz().call()
            }

            if (result.validSignature) {
                request.setAttribute("appId", shopifyApp.appId)
            } else {
                logger.error("HMAC ${hmac} verification failed for clientId ${clientId} and message ${message}")
            }
            validSignature=  result.validSignature
        }
        return validSignature;
    }
}
