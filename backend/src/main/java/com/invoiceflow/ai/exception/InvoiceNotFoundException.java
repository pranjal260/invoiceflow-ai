package com.invoiceflow.ai.exception;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super("Invoice with ID '" + invoiceId + "' was not found or is not accessible.");
    }
}
