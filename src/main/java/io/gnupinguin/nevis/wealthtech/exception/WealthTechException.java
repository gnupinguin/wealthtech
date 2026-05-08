package io.gnupinguin.nevis.wealthtech.exception;

public abstract class WealthTechException extends RuntimeException {

    protected WealthTechException(String message) {
        super(message);
    }

    protected WealthTechException(String message, Throwable cause) {
        super(message, cause);
    }

}
