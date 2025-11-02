package com.datacrump.trino.okta;

import java.util.Set;

import io.trino.spi.Plugin;
import io.trino.spi.security.GroupProviderFactory;

public class OktaGroupProviderPlugin implements Plugin {
    @Override
    public Iterable<GroupProviderFactory> getGroupProviderFactories() {
        return Set.of(new OktaGroupProviderFactory());
    }
}

