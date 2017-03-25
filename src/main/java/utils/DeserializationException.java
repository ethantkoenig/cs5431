package utils;

public final class DeserializationException extends Exception {
    public final String message;

    public DeserializationException(String message) {
        this.message = message;
    }
}
