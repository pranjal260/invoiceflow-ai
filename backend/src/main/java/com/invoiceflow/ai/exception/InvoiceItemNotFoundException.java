package com.invoiceflow.ai.exception;

import java.util.UUID;

public class InvoiceItemNotFoundException extends RuntimeException {

    public InvoiceItemNotFoundException(UUID invoiceItemId) {
        super("Invoice item with ID '" + invoiceItemId + "' was not found or is not accessible.");
    }
}
