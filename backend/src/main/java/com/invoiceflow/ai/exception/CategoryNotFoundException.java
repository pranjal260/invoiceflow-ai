package com.invoiceflow.ai.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID categoryId) {
        super("Category with ID '" + categoryId + "' was not found or is not accessible.");
    }
}
