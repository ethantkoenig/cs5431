package server.utils;

/**
 * Utilities for validating inputs
 */
public final class ValidateUtils {

    // Disallow instances of this class
    private ValidateUtils() {
    }

    public static final int MIN_USERNAME_LENGTH = 6;
    public static final int MAX_USERNAME_LENGTH = 24;
    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MAX_PASSWORD_LENGTH = 24;


    private static boolean validLength(String str, int minLen, int maxLen) {
        return str.length() >= minLen && str.length() <= maxLen;
    }

    public static boolean isAlphanumeric(String str) {
        return str.matches("[A-Za-z0-9]*");
    }

    public static boolean containsNumber(String str) {
        return str.matches(".*[0-9].*");
    }

    public static boolean containsLowercase(String str) {
        return str.matches(".*[a-z].*$");
    }

    public static boolean containsUpperCase(String str) {
        return str.matches(".*[A-Z].*$");
    }

    public static boolean containsEmail(String str) {
        return str.matches(
                "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
    }

    /**
     * @return whether the given username is valid
     */
    public static boolean validUsername(String username) {
        return validLength(username, MIN_USERNAME_LENGTH, MAX_USERNAME_LENGTH)
                && isAlphanumeric(username);
    }

    /**
     * @return whether the given password is valid
     */
    public static boolean validPassword(String password) {
        return validLength(password, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH)
                && containsNumber(password)
                && containsLowercase(password)
                && containsUpperCase(password);
    }

    /**
     * @return whether the given email is valid
     */
    public static boolean validEmail(String email) {
        return containsEmail(email);
    }
}
