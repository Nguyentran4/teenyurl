package com.example.demo.exception;

public class AliasAlreadyExistsException extends RuntimeException {
    public AliasAlreadyExistsException(String alias) {
        super("Alias is already taken: " + alias);
    }
}
