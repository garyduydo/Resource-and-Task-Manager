package security;

import javax.crypto.SecretKeyFactory;     
import javax.crypto.spec.PBEKeySpec;      
import java.security.MessageDigest;       
import java.security.SecureRandom;      
import java.util.Base64;                

/**
 * PasswordHasher
 * Helper for password hashing and verification.
 * Uses PBKDF2 (slow hashing) + per-password random salt.
 */
public final class PasswordHasher {
    private PasswordHasher() {} // static helper only

    public static final String DEFAULT_ALGO = "PBKDF2WithHmacSHA256"; // PBKDF2 algorithm to use.
    public static final int DEFAULT_ITERATIONS = 120000; //iterations to run (higher = slower = safer)
    public static final int DEFAULT_KEY_LENGTH_BITS = 256; //Size of the derived key in bits (256 = 32 bytes)
    public static final int DEFAULT_SALT_BYTES = 16; //Size of the per-password random salt in bytes.

    /** Generate a new cryptographically-strong random salt. */
    public static byte[] generateSalt() {
        byte[] salt = new byte[DEFAULT_SALT_BYTES]; // 16 bytes
        new SecureRandom().nextBytes(salt); // cryptographically-strong randomness
        return salt;
    }

    /**
     * Derive a hash (aka "derived key") from a password using PBKDF2.
     *
     * @param passwordChars characters of the password (char[] so callers could wipe it later if desired)
     * @param salt          per-password random salt (bytes)
     * @param iterations    number of PBKDF2 iterations (work factor)
     * @param keyLenBits    output size in bits (e.g., 256)
     * @return raw bytes of the derived key (this is your password hash)
     */
    public static byte[] deriveKey(char[] passwordChars, byte[] salt, int iterations, int keyLenBits) {
        try {
            // PBEKeySpec inputs: password, salt, iteration count, and desired key length.
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, keyLenBits);

            // SecretKeyFactory with our algorithm name runs PBKDF2 and returns the derived key bytes.
            SecretKeyFactory factory = SecretKeyFactory.getInstance(DEFAULT_ALGO);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 deriveKey failed", e);
        }
    }

    /**  Convert raw bytes to text. */
    public static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** Decode a Base64 string back into raw bytes. */
    public static byte[] fromBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Constant-time equality check for two byte arrays.
     * This avoids tiny timing differences that could leak information.
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}


