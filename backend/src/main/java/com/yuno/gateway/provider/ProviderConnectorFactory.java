package com.yuno.gateway.provider;

import com.yuno.gateway.enums.ProviderCode;
import com.yuno.gateway.exception.ProviderUnavailableException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for resolving provider connectors by code.
 * 
 * Uses Spring's dependency injection to auto-discover all ProviderConnector
 * implementations and register them by their provider code.
 * 
 * This pattern makes adding new providers trivial:
 * 1. Implement ProviderConnector
 * 2. Annotate with @Component
 * 3. Factory auto-discovers it
 */
@Component
public class ProviderConnectorFactory {

    private final Map<ProviderCode, ProviderConnector> connectorMap;

    public ProviderConnectorFactory(List<ProviderConnector> connectors) {
        this.connectorMap = new EnumMap<>(ProviderCode.class);
        connectors.forEach(c -> connectorMap.put(c.getProviderCode(), c));
    }

    /**
     * Get connector for a specific provider.
     * 
     * @param code The provider code
     * @return The matching connector
     * @throws ProviderUnavailableException if no connector is registered for the code
     */
    public ProviderConnector getConnector(ProviderCode code) {
        ProviderConnector connector = connectorMap.get(code);
        if (connector == null) {
            throw new ProviderUnavailableException("No connector registered for provider: " + code);
        }
        return connector;
    }

    /**
     * Check if a connector exists and is healthy.
     */
    public boolean isProviderAvailable(ProviderCode code) {
        ProviderConnector connector = connectorMap.get(code);
        return connector != null && connector.isHealthy();
    }
}
