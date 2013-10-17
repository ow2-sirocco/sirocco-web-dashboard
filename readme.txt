Sirocco dashboard
*****************

Vaadin-based dashboard for the Sirocco multi-cloud manager


Note:

Server push only works with Glassfish 3/4 provided the comet support is enabled:

	asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.comet-support-enabled="true"

WebSockets does not work on GF4 currently

See https://vaadin.com/wiki/-/wiki/Main/Working%20around%20push%20issues
