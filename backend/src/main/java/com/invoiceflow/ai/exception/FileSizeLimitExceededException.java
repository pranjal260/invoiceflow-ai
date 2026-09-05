package com.invoiceflow.ai.exception;

public class FileSizeLimitExceededException extends RuntimeException {

    public FileSizeLimitExceededException(long fileSize, long maxSize) {
        super("File size " + fileSize + " bytes exceeds maximum allowed size of " + maxSize + " bytes.");
    }
}
