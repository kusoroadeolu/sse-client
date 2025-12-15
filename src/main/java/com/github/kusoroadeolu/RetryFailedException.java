package com.github.kusoroadeolu;

public class RetryFailedException extends RuntimeException {
    public RetryFailedException(String message) {
        super(message);
    }

    public RetryFailedException() {
        super();
    }

    public RetryFailedException(Throwable cause) {
        super(cause);
    }
}
