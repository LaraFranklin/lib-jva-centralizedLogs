package com.organization.lib.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the centralized logging library.
 * <p>
 * All properties use the prefix {@code centralized-logs}. Example
 * configuration:
 * 
 * <pre>
 * centralized-logs.service-name=my-service
 * centralized-logs.environment=prod
 * centralized-logs.broker-url=tcp://localhost:61616
 * centralized-logs.user=artemis
 * centralized-logs.password=artemis
 * centralized-logs.queue.logs=log.send
 * centralized-logs.queue.errors=log.errors
 * centralized-logs.interceptor.enabled=true
 * centralized-logs.interceptor.exclude-paths=/actuator/**,/health
 * centralized-logs.sender.enabled=true
 * centralized-logs.body-max-size=10000
 * </pre>
 *
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "centralized-logs")
public class CentralizedLogsProperties {

    /** Name of the microservice using this library. */
    private String serviceName = "unknown-service";

    /** Execution environment (dev, qa, prod). */
    private String environment = "unknown";

    /** URL of the ActiveMQ Artemis broker. */
    private String brokerUrl = "tcp://localhost:61616";

    /** Username for the Artemis broker connection. */
    private String user = "artemis";

    /** Password for the Artemis broker connection. */
    private String password = "artemis";

    /** Queue configuration for log and error messages. */
    private Queue queue = new Queue();

    /** Interceptor configuration. */
    private Interceptor interceptor = new Interceptor();

    /** Sender configuration. */
    private Sender sender = new Sender();

    /**
     * Maximum size in characters for request/response body capture. Bodies
     * exceeding this limit are truncated.
     */
    private int bodyMaxSize = 10_000;

    /**
     * Queue names for log routing.
     */
    @Getter
    @Setter
    public static class Queue {
        /** Queue name for successful request logs. */
        private String logs = "log.send";

        /** Queue name for error request logs. */
        private String errors = "log.errors";
    }

    /**
     * Interceptor activation and path filtering settings.
     */
    @Getter
    @Setter
    public static class Interceptor {
        /** Whether the HTTP interceptor is enabled. */
        private boolean enabled = true;

        /**
         * List of path patterns to exclude from interception (e.g., health checks,
         * actuator endpoints).
         */
        private List<String> excludePaths = List.of("/actuator/**", "/health", "/swagger-ui/**", "/v3/api-docs/**");
    }

    /**
     * Controls whether messages are actually sent to Artemis.
     */
    @Getter
    @Setter
    public static class Sender {
        /**
         * Whether sending messages to Artemis is enabled. When false, messages are
         * logged but not sent.
         */
        private boolean enabled = true;
    }
}
