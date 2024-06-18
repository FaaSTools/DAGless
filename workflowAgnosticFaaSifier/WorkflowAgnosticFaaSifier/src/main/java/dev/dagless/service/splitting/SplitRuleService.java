package dev.dagless.service.splitting;

import dev.dagless.model.config.Config;
import dev.dagless.model.splitting.SplitRule;
import spoon.reflect.code.*;

import java.util.logging.Logger;

public class SplitRuleService {

    private final Logger logger = Logger.getLogger(SplitRuleService.class.getName());
    private final Config config;

    public SplitRuleService(Config config) {
        this.config = config;
    }

    public boolean isSplit(CtStatement statement) {
        if (statement instanceof CtForEach) {
            return config.getSplitRules().contains(SplitRule.FOR_EACH);
        }

        if (statement instanceof CtAssignment<?, ?> assignment) {
            logger.fine("Found assignment:" + assignment);
            if (assignment.getAssignment() instanceof CtInvocation<?> invocation) {
                return isSplit(invocation);
            }
        }
        if (statement instanceof CtLocalVariable<?> variableDeclaration) {
            logger.fine("Found variable:" + variableDeclaration);
            if (variableDeclaration.getAssignment() instanceof CtInvocation<?> invocation) {
                return isSplit(invocation);
            }

        } else if (statement instanceof CtInvocation<?> invocation) {
            logger.fine("Found invocation:" + invocation);
            if (invocation.toString().startsWith("java.")) {
                logger.fine("Found system method invocation:" + invocation);
                return config.getSplitRules().contains(SplitRule.SYSTEM_METHOD_INVOCATION);
            } else {
                logger.fine("Found custom method invocation:" + invocation);
                return config.getSplitRules().contains(SplitRule.CUSTOM_METHOD_INVOCATION);
            }
        } else {
            return false;
        }
        return false;
    }

}
