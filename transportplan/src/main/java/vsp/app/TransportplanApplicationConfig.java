package vsp.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Transportplan.
 * External API calls now go through externalRest via gRPC.
 * gRPC client configuration is handled by grpc-client-spring-boot-starter
 * via application.properties (grpc.client.externalrest.* and grpc.client.displaymanager.*)
 */
@Configuration
@ConfigurationProperties(prefix = "vsp")
public class TransportplanApplicationConfig {

    // gRPC connections are configured in application.properties
    // No additional config needed at this time

}
