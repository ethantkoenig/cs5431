package utils;

import java.security.SecureRandom;

/**
 * Various system-wide constants and configurations. These "constants" should be
 * modified only in tests.
 */
public final class Config {

    private static int PBKDF2_COST = 12;
    private static int HASH_GOAL = 2;
    private static String MAIL_SMTP_HOST = "smtp.gmail.com";
    private static String MAIL_FROM = "yaccoin5431@gmail.com";
    private static String MAIL_PASSWORD = "HAfCt/9^"; //TODO: make environment variable. This is just easier for testing.
    private static SecureRandom SECURE_RANDOM = new SecureRandom();

    // Disallow instances of this class
    private Config() {
    }

    public static int pbkdf2Cost() {
        return PBKDF2_COST;
    }

    public static void setPbkdf2Cost(int cost) {
        PBKDF2_COST = cost;
    }

    public static int hashGoal() {
        return HASH_GOAL;
    }

    public static void setHashGoal(int goal) {
        HASH_GOAL = goal;
    }

    public static String getMailSmtpHost() {
        return MAIL_SMTP_HOST;
    }

    public static void setMailSmtpHost(String mailSmtpHost) {
        MAIL_SMTP_HOST = mailSmtpHost;
    }

    public static String getMailFrom() {
        return MAIL_FROM;
    }

    public static void setMailFrom(String mailFrom) {
        MAIL_FROM = mailFrom;
    }

    public static String getMailPassword() {
        return MAIL_PASSWORD;
    }

    public static void setMailPassword(String mailPassword) {
        MAIL_PASSWORD = mailPassword;
    }

    public static SecureRandom secureRandom() {
        return SECURE_RANDOM;
    }

    public static void setSecureRandom(SecureRandom secureRandom) {
        SECURE_RANDOM = secureRandom;
    }
}
