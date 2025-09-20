package br.corp.shortener.exceptions;

public class DuplicateCodeException extends RuntimeException {
    public DuplicateCodeException(String code) {
        super("Code already in use: " + code);
    }
}