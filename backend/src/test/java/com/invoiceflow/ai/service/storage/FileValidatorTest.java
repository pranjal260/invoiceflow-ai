package com.invoiceflow.ai.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.invoiceflow.ai.exception.EmptyFileException;
import com.invoiceflow.ai.exception.FileSizeLimitExceededException;
import com.invoiceflow.ai.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator();
    }

    @Test
    void acceptsValidPdf() {
        byte[] content = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
        fileValidator.validate(content, "application/pdf");
    }

    @Test
    void acceptsValidJpeg() {
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8};
        fileValidator.validate(content, "image/jpeg");
    }

    @Test
    void acceptsValidPng() {
        byte[] content = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        fileValidator.validate(content, "image/png");
    }

    @Test
    void rejectsUnsupportedContentType() {
        byte[] content = new byte[]{0x01};
        assertThatThrownBy(() -> fileValidator.validate(content, "text/plain"))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("text/plain");
    }

    @Test
    void rejectsNullContentType() {
        byte[] content = new byte[]{0x01};
        assertThatThrownBy(() -> fileValidator.validate(content, null))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> fileValidator.validate(new byte[0], "application/pdf"))
                .isInstanceOf(EmptyFileException.class);
    }

    @Test
    void rejectsNullFileBytes() {
        assertThatThrownBy(() -> fileValidator.validate(null, "application/pdf"))
                .isInstanceOf(EmptyFileException.class);
    }

    @Test
    void rejectsOversizedFile() {
        byte[] oversized = new byte[(int) FileValidator.MAX_FILE_SIZE + 1];
        assertThatThrownBy(() -> fileValidator.validate(oversized, "application/pdf"))
                .isInstanceOf(FileSizeLimitExceededException.class)
                .hasMessageContaining(String.valueOf(FileValidator.MAX_FILE_SIZE));
    }

    @Test
    void acceptsFileAtExactSizeLimit() {
        byte[] exactLimit = new byte[(int) FileValidator.MAX_FILE_SIZE];
        fileValidator.validate(exactLimit, "application/pdf");
    }

    @Test
    void derivesExtensionForPdf() {
        assertThat(fileValidator.extensionForContentType("application/pdf")).isEqualTo("pdf");
    }

    @Test
    void derivesExtensionForJpeg() {
        assertThat(fileValidator.extensionForContentType("image/jpeg")).isEqualTo("jpg");
    }

    @Test
    void derivesExtensionForPng() {
        assertThat(fileValidator.extensionForContentType("image/png")).isEqualTo("png");
    }

    @Test
    void rejectsExtensionForUnsupportedType() {
        assertThatThrownBy(() -> fileValidator.extensionForContentType("application/zip"))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }
}
