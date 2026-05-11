package io.gnupinguin.nevis.wealthtech.exception;

public class ServiceUnavailableException extends WealthTechException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
