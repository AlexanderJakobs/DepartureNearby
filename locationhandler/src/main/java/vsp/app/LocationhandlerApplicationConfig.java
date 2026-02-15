package vsp.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Locationhandler.
 * External API calls now go through externalRest via gRPC.
 */
@Configuration
@ConfigurationProperties(prefix = "vsp")
public class LocationhandlerApplicationConfig {

    // gRPC client configuration is handled by grpc-client-spring-boot-starter
    // via application.properties (grpc.client.externalrest.* and grpc.client.transportplan.*)

}
