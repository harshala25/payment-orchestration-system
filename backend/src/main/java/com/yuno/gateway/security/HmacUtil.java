package com.yuno.gateway.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * HMAC-SHA256 utility for request signing and verification.
 * 
 * Used to ensure request integrity: the client signs the request body
 * with a shared secret, and the server verifies the signature.
 * This prevents tampering with payment data in transit.
 */
public final class HmacUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacUtil() {
        // Utility class
    }

    /**
     * Generate HMAC-SHA256 signature for the given data.
     *
     * @param data   The data to sign (typically request body)
     * @param secret The shared secret key
     * @return Base64-encoded HMAC signature
     */
    public static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    /**
     * Verify an HMAC signature against expected data.
     *
     * @param data      The original data
     * @param signature The signature to verify
     * @param secret    The shared secret
     * @return true if the signature matches
     */
    public static boolean verify(String data, String signature, String secret) {
        String computed = sign(data, secret);
        return computed.equals(signature);
    }
}
