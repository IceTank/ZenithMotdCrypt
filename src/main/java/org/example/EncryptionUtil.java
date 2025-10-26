package org.example;


import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/*
 * @author IceTank
 * @since 25.10.2025
 */
public class EncryptionUtil {
    // Tunables
    private static final int SALT_LEN = 16;         // bytes
    private static final int IV_LEN = 12;           // bytes (96-bit recommended for GCM)
    private static final int TAG_BITS = 128;        // auth tag length
    private static final int PBKDF2_ITERS = 100_000;
    private static final int KEY_BITS = 256;        // 128 or 256 (256 needs modern JRE)

    private static final SecureRandom RNG = new SecureRandom();

    public static String encrypt(String plaintext, String password) throws Exception {
        byte[] salt = randomBytes(SALT_LEN);
        byte[] key = deriveKey(password, salt);

        byte[] iv = randomBytes(IV_LEN);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, iv)
        );
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Pack: [salt | iv | ciphertext+tag]
        ByteBuffer out = ByteBuffer.allocate(salt.length + iv.length + ciphertext.length);
        out.put(salt).put(iv).put(ciphertext);
        byte[] packed = out.array();

        return Base64.getEncoder().encodeToString(packed);
    }

    public static String decrypt(String token, String password) throws Exception {
        byte[] packed = Base64.getDecoder().decode(token);

        if (packed.length < SALT_LEN + IV_LEN + 1) {
            throw new IllegalArgumentException("Ciphertext too short");
        }

        ByteBuffer buf = ByteBuffer.wrap(packed);
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        buf.get(salt).get(iv);
        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);

        byte[] key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_BITS, iv)
        );
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private static byte[] deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERS, KEY_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }

    public static String byteToBase64(byte[] iconPng) {
        return Base64.getEncoder().encodeToString(iconPng);
    }
}
