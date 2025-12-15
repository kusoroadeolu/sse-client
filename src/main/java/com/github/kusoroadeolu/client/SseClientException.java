package com.github.kusoroadeolu.client;

public class SseClientException extends RuntimeException {
    public SseClientException(String message) {
        super(message);
    }


    public SseClientException(Throwable cause) {
        super(cause);
    }
}
