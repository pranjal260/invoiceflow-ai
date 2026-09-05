package com.invoiceflow.ai.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID invoiceId) {
        super("No document attached to invoice " + invoiceId + ".");
    }
}
