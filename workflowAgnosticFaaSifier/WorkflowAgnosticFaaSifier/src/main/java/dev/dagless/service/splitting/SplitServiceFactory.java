package dev.dagless.service.splitting;

import dev.dagless.model.config.Config;

public class SplitServiceFactory {

    public static AbstractSplitService createSplitService(Config config) {
        if (config.getSplitRules() != null && config.getConfigSplitFunctions() == null) {
            return new RuleBasedSplitService(config);
        } else if (config.getSplitRules() == null && config.getConfigSplitFunctions() != null) {
            return new ConfigBasedSplitService(config);
        } else {
            throw new IllegalArgumentException("Config must contain either split rules or split functions");
        }
    }
}
