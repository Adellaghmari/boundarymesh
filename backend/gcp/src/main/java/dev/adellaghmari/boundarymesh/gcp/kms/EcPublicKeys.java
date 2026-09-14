package dev.adellaghmari.boundarymesh.gcp.kms;

import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Parses Cloud KMS SubjectPublicKeyInfo PEM. Public keys are non-secret. */
final class EcPublicKeys {

    private EcPublicKeys() {}

    static PublicKey fromPem(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("KMS public key PEM is required");
        }
        String body = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(body);
            PublicKey parsed = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec expected = parameters.getParameterSpec(ECParameterSpec.class);
            if (!(parsed instanceof ECPublicKey ec)
                    || !ec.getParams().getCurve().equals(expected.getCurve())
                    || !ec.getParams().getGenerator().equals(expected.getGenerator())
                    || !ec.getParams().getOrder().equals(expected.getOrder())
                    || ec.getParams().getCofactor() != expected.getCofactor()) {
                throw new IllegalArgumentException("KMS public key must use NIST P-256");
            }
            return parsed;
        } catch (Exception ex) {
            throw CloudKmsAccessException.wrap(ex);
        }
    }

    static String toPem(PublicKey publicKey) {
        String encoded = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n";
    }

    static boolean looksLikePem(String pem) {
        if (pem == null) {
            return false;
        }
        String upper = pem.toUpperCase();
        return upper.contains("BEGIN PUBLIC KEY") && !upper.contains("BEGIN PRIVATE");
    }
}
