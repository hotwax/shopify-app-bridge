# Pre deployment steps:

### Drop Shopify app history table – schema has changed

`DROP TABLE Shopify_App_History`
`delete from Shopify_App_Access_Scope where thru_date is not null`
