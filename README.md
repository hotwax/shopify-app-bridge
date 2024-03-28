### Shopify OMS Connector

The Shopify OMS Connector facilitates integration between Shopify shops and the OMS (Order Management System) during app installation on Shopify shops. 

#### Installation Process

1. **Shop Selection**: When a user clicks on the install button for the Shopify app, they will be prompted to choose their shop.

2. **Installation Request Verification**: After selecting the shop, the app verifies the installation request. If the request is valid, the user will be redirected to an OAuth page to install the app on the Shopify shop.

3. **Authorization Code Retrieval**: Upon successful installation, the user will be redirected back to the app with an authorization code. The Shopify OMS Connector uses this code to obtain an access token.

4. **OMS Instance Configuration**: Upon opening the app, the user must enter the OMS instance address and a valid token. After successful verification, the user can access the OMS for the configured shop.

5. **Automatic Redirection**: Subsequent openings of the app, with a configured HotWax instance, will automatically redirect the user to the OMS instance.

#### Repository Information

- **Repository Link**: [Shopify OMS Connector Repository](https://github.com/hotwax/shopify-app-bridge)

Feel free to explore the repository for more details and updates.
