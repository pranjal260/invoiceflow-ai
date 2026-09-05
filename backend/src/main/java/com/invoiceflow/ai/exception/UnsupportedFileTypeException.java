package com.invoiceflow.ai.exception;

public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String contentType) {
        super("Unsupported file type: " + contentType + ". Allowed types: PDF, JPEG, PNG.");
    }
}
