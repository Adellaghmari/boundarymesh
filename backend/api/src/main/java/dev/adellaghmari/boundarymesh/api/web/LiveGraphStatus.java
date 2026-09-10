package dev.adellaghmari.boundarymesh.api.web;

/** Distinguishes configured live Graph mode from a provider-verified live pull. */
public final class LiveGraphStatus {

    public static final String CONTRACT_HARNESS = "CONTRACT_HARNESS";
    public static final String LIVE_CONFIGURED = "LIVE_CONFIGURED";
    public static final String LIVE_VERIFIED = "LIVE_VERIFIED";

    private LiveGraphStatus() {}

    public static boolean providerSuccess(int status) {
        return status >= 200 && status < 300;
    }

    public static String pullMode(boolean liveConfigured, int providerStatus) {
        if (!liveConfigured) {
            return CONTRACT_HARNESS;
        }
        return providerSuccess(providerStatus) ? LIVE_VERIFIED : LIVE_CONFIGURED;
    }

    public static boolean pullLive(boolean liveConfigured, int providerStatus) {
        return liveConfigured && providerSuccess(providerStatus);
    }

    public static String connectorMode(boolean liveConfigured) {
        return liveConfigured ? LIVE_CONFIGURED : CONTRACT_HARNESS;
    }
}
