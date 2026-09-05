package com.invoiceflow.ai.exception;

public class DuplicateInvoiceNumberException extends RuntimeException {

    public DuplicateInvoiceNumberException(String invoiceNumber) {
        super("An invoice with number '" + invoiceNumber + "' already exists for this user.");
    }
}
