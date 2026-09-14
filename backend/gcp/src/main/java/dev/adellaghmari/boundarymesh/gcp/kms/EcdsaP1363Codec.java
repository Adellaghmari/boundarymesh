package dev.adellaghmari.boundarymesh.gcp.kms;

import java.util.Arrays;

/**
 * Cloud KMS {@code EC_SIGN_P256_SHA256} returns ASN.1 DER ECDSA. BoundaryMesh attestations store
 * IEEE P1363 (r || s, 32+32 bytes). This codec is the only conversion point.
 */
public final class EcdsaP1363Codec {

    static final int P256_COORDINATE_BYTES = 32;
    static final int P1363_LENGTH = P256_COORDINATE_BYTES * 2;

    private EcdsaP1363Codec() {}

    public static byte[] derToP1363(byte[] der) {
        if (der == null || der.length < 8 || der[0] != 0x30) {
            throw new IllegalArgumentException("ECDSA DER signature is required");
        }
        // P-256 DER is at most 72 bytes; all lengths must use the minimal short form.
        if (der.length > 72 || (der[1] & 0xff) != der.length - 2) {
            throw new IllegalArgumentException("invalid ECDSA DER sequence length");
        }
        byte[] p1363 = new byte[P1363_LENGTH];
        int offset = copyInteger(der, 2, p1363, 0);
        offset = copyInteger(der, offset, p1363, P256_COORDINATE_BYTES);
        if (offset != der.length) {
            throw new IllegalArgumentException("trailing ECDSA DER bytes");
        }
        return p1363;
    }

    public static byte[] p1363ToDer(byte[] p1363) {
        if (p1363 == null || p1363.length != P1363_LENGTH) {
            throw new IllegalArgumentException("ECDSA P1363 signature must be 64 bytes");
        }
        byte[] r = derInteger(Arrays.copyOfRange(p1363, 0, P256_COORDINATE_BYTES));
        byte[] s = derInteger(Arrays.copyOfRange(p1363, P256_COORDINATE_BYTES, P1363_LENGTH));
        int seqLen = r.length + s.length;
        byte[] der = new byte[2 + seqLen];
        der[0] = 0x30;
        der[1] = (byte) seqLen;
        System.arraycopy(r, 0, der, 2, r.length);
        System.arraycopy(s, 0, der, 2 + r.length, s.length);
        return der;
    }

    private static int copyInteger(byte[] der, int offset, byte[] target, int targetOffset) {
        if (offset + 2 > der.length || der[offset] != 0x02) {
            throw new IllegalArgumentException("ECDSA DER INTEGER missing");
        }
        int length = der[offset + 1] & 0xff;
        int start = offset + 2;
        int end = start + length;
        if (length == 0 || length > 33 || end > der.length || (der[start] & 0x80) != 0) {
            throw new IllegalArgumentException("invalid ECDSA DER positive INTEGER");
        }
        if (der[start] == 0) {
            if (length == 1 || (der[start + 1] & 0x80) == 0) {
                throw new IllegalArgumentException("zero or non-minimal ECDSA DER INTEGER");
            }
            start++;
        }
        int magnitudeLength = end - start;
        if (magnitudeLength > P256_COORDINATE_BYTES) {
            throw new IllegalArgumentException("ECDSA coordinate exceeds P-256");
        }
        System.arraycopy(der, start, target, targetOffset + P256_COORDINATE_BYTES - magnitudeLength, magnitudeLength);
        return end;
    }

    private static byte[] derInteger(byte[] unsigned) {
        byte[] value = stripLeadingZeros(unsigned);
        boolean pad = value.length == 0 || (value[0] & 0x80) != 0;
        int body = value.length + (pad ? 1 : 0);
        byte[] encoded = new byte[2 + body];
        encoded[0] = 0x02;
        encoded[1] = (byte) body;
        if (pad) {
            encoded[2] = 0x00;
            System.arraycopy(value, 0, encoded, 3, value.length);
        } else {
            System.arraycopy(value, 0, encoded, 2, value.length);
        }
        return encoded;
    }

    private static byte[] stripLeadingZeros(byte[] value) {
        int i = 0;
        while (i < value.length - 1 && value[i] == 0) {
            i++;
        }
        return Arrays.copyOfRange(value, i, value.length);
    }
}
