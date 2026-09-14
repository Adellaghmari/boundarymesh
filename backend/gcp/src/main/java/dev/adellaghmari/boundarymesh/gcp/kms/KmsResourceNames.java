package dev.adellaghmari.boundarymesh.gcp.kms;

final class KmsResourceNames {

    private KmsResourceNames() {}

    static String keyRing(String projectId, String location, String keyRing) {
        return "projects/" + projectId + "/locations/" + location + "/keyRings/" + keyRing;
    }

    static String cryptoKey(String projectId, String location, String keyRing, String cryptoKey) {
        return keyRing(projectId, location, keyRing) + "/cryptoKeys/" + cryptoKey;
    }

    static String cryptoKeyVersion(
            String projectId, String location, String keyRing, String cryptoKey, String version) {
        return cryptoKey(projectId, location, keyRing, cryptoKey) + "/cryptoKeyVersions/" + version;
    }
}
