package com.swrve.reactnative;

class SwrvePluginErrorCodes {
    static final String CREATE_INSTANCE_FAILED = "CREATE_INSTANCE_FAILED";
    static final String INVALID_ARGUMENT = "CREATE_INSTANCE_FAILED";
    static final String EXCEPTION = "EXCEPTION";

    static String swrveResponseCode(int swrveError) {
        return "SWRVE_RESPONSE " + swrveError;
    }
}