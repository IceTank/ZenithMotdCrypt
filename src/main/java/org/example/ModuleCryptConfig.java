package org.example;

/**
 * Example configuration POJO.
 *
 * Configurations are saved and loaded to JSON files
 *
 * All fields should be public and mutable.
 *
 * Fields to static inner classes generate nested JSON objects.
 */
public class ModuleCryptConfig {
    public boolean motdEncryption = true;

    public final MotdEncryptionConfig encryptionConfig = new MotdEncryptionConfig();
    public static class MotdEncryptionConfig {
        public String password = "example-password";
    }
}
