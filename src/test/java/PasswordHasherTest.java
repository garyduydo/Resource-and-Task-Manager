import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import security.PasswordHasher;

public class PasswordHasherTest {

    @Test
    public void determinism_sameSaltSamePassword_sameHash() {
        byte[] salt = PasswordHasher.generateSalt();

        byte[] h1 = PasswordHasher.deriveKey(
                "Secret123!".toCharArray(), salt,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);

        byte[] h2 = PasswordHasher.deriveKey(
                "Secret123!".toCharArray(), salt,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);

        assertTrue(PasswordHasher.constantTimeEquals(h1, h2));
    }

    @Test
    public void saltMatters_samePasswordDifferentSalts_differentHashes() {
        byte[] salt1 = PasswordHasher.generateSalt();
        byte[] salt2 = PasswordHasher.generateSalt();

        byte[] h1 = PasswordHasher.deriveKey(
                "SamePass".toCharArray(), salt1,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);

        byte[] h2 = PasswordHasher.deriveKey(
                "SamePass".toCharArray(), salt2,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);

        assertFalse(PasswordHasher.constantTimeEquals(h1, h2));
    }

    @Test
    public void base64_roundTrip_isLossless() {
        byte[] salt = PasswordHasher.generateSalt();
        String txt = PasswordHasher.toBase64(salt);
        byte[] back = PasswordHasher.fromBase64(txt);
        assertArrayEquals(salt, back);
    }

    @Test
    public void keyLength_controlsOutputSize_andValue() {
        byte[] salt = PasswordHasher.generateSalt();

        byte[] h256 = PasswordHasher.deriveKey("Kangaroo!".toCharArray(), salt,
                PasswordHasher.DEFAULT_ITERATIONS, 256);
        byte[] h128 = PasswordHasher.deriveKey("Kangaroo!".toCharArray(), salt,
                PasswordHasher.DEFAULT_ITERATIONS, 128);

        assertEquals(32, h256.length); // 256 bits = 32 bytes
        assertEquals(16, h128.length); // 128 bits = 16 bytes
        assertFalse(PasswordHasher.constantTimeEquals(h256, h128));
    }

    @Test
    public void iterations_affectOutput() {
        byte[] salt = PasswordHasher.generateSalt();
        char[] pw = "IterationCheck!".toCharArray();

        byte[] low  = PasswordHasher.deriveKey(pw, salt, 20_000, 256);
        byte[] high = PasswordHasher.deriveKey(pw, salt, 120_000, 256);

        assertFalse(PasswordHasher.constantTimeEquals(low, high));
    }

    @Test
    public void salts_areCorrectSize_andRarelyRepeat() {
        for (int i = 0; i < 5; i++) {
            byte[] s1 = PasswordHasher.generateSalt();
            byte[] s2 = PasswordHasher.generateSalt();
            assertEquals(PasswordHasher.DEFAULT_SALT_BYTES, s1.length);
            assertEquals(PasswordHasher.DEFAULT_SALT_BYTES, s2.length);
            assertFalse(PasswordHasher.constantTimeEquals(s1, s2));
        }
    }

    @Test
    public void constantTimeEquals_behaviour_equalDifferentLengthMismatch() {
        byte[] a = new byte[] {1, 2, 3, 4};
        byte[] b = new byte[] {1, 2, 3, 4};
        byte[] c = new byte[] {1, 2, 9, 4};
        byte[] d = new byte[] {1, 2, 3};

        assertTrue(PasswordHasher.constantTimeEquals(a, b));
        assertFalse(PasswordHasher.constantTimeEquals(a, c));
        assertFalse(PasswordHasher.constantTimeEquals(a, d));
    }

    @Test
    public void edgePasswords_unicodeAndEmpty_areHashed() {
        byte[] salt = PasswordHasher.generateSalt();

        byte[] unicodeHash = PasswordHasher.deriveKey("Pāss".toCharArray(), salt,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);
        assertEquals(32, unicodeHash.length);

        byte[] emptyHash = PasswordHasher.deriveKey("".toCharArray(), salt,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);
        assertEquals(32, emptyHash.length);
    }

    @Test
    public void deriveKey_nullSalt_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () ->
            PasswordHasher.deriveKey("pw".toCharArray(), null,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS));
    }

    @Test
    public void deriveKey_nonPositiveIterations_throwsRuntimeException() {
        byte[] salt = PasswordHasher.generateSalt();
        assertThrows(RuntimeException.class, () ->
            PasswordHasher.deriveKey("pw".toCharArray(), salt, 0, PasswordHasher.DEFAULT_KEY_LENGTH_BITS));
        assertThrows(RuntimeException.class, () ->
            PasswordHasher.deriveKey("pw".toCharArray(), salt, -1, PasswordHasher.DEFAULT_KEY_LENGTH_BITS));
    }

    @Test
    public void deriveKey_nonPositiveKeyLength_throwsRuntimeException() {
        byte[] salt = PasswordHasher.generateSalt();
        assertThrows(RuntimeException.class, () ->
            PasswordHasher.deriveKey("pw".toCharArray(), salt, PasswordHasher.DEFAULT_ITERATIONS, 0));
        assertThrows(RuntimeException.class, () ->
            PasswordHasher.deriveKey("pw".toCharArray(), salt, PasswordHasher.DEFAULT_ITERATIONS, -128));
    }

    @Test
    public void deriveKey_nullPassword_behavesLikeEmptyPassword() {
        byte[] salt = PasswordHasher.generateSalt();

        byte[] hNull  = PasswordHasher.deriveKey(null,            salt,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);
        byte[] hEmpty = PasswordHasher.deriveKey("".toCharArray(), salt,
                PasswordHasher.DEFAULT_ITERATIONS, PasswordHasher.DEFAULT_KEY_LENGTH_BITS);

        // On this JDK, null acts like empty: ensure hashes match
        assertArrayEquals(hEmpty, hNull);
    }
}
