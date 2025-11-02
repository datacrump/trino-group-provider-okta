package com.datacrump.trino.okta;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.trino.spi.security.GroupProvider;
import io.trino.spi.security.GroupProviderFactory;

public class OktaGroupProviderFactory implements GroupProviderFactory {

    @Override
    public String getName() {
        return "group-provider-okta";
    }

    @Override
    public GroupProvider create(Map<String, String> config) {
        try {
            String oktaDomain = config.get("okta.domain");
            String clientId = config.get("okta.client.id");
            String groupPatternString = config.getOrDefault("okta.group.pattern", ".*");
            List<String> scopes = Arrays.asList(config.getOrDefault("okta.scopes", "okta.users.read").split(","));
            String privateKeyPath = config.get("okta.private.key.path");

            return new OktaGroupProvider(oktaDomain, clientId, scopes, privateKeyPath, groupPatternString);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OktaGroupProvider", e);
        }
    }
}
