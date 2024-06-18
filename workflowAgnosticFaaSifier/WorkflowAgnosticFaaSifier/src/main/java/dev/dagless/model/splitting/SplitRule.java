package dev.dagless.model.splitting;

public enum SplitRule {
    CUSTOM_METHOD_INVOCATION, // splits function at custom method invocations
    SYSTEM_METHOD_INVOCATION, // splits function at system method invocations
    FOR_EACH, // splits function at for each loops
}
