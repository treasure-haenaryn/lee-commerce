package com.github.haenaryn.catalog.interfaces;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("API Key가 올바르지 않다");
    }
}
