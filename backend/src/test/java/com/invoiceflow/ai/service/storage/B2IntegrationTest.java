package com.invoiceflow.ai.service.storage;

import com.invoiceflow.ai.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test that verifies real Backblaze B2 connectivity.
 *
 * Only runs when B2_ENDPOINT environment variable is set, meaning
 * real credentials are available. Skipped in CI/CD without credentials.
 *
 * Performs: upload → verify exists → generate presigned URL → delete → verify deleted.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "B2_ENDPOINT", matches = ".+")
class B2IntegrationTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private StorageProperties storageProperties;

    @Test
    void uploadVerifyAndDeleteRealObject() {
        // Small test PDF-like content
        byte[] testContent = "%PDF-1.4 InvoiceFlow AI B2 integration test".getBytes();
        String contentType = "application/pdf";
        UUID testUserId = UUID.randomUUID();

        // Step 1: Upload
        FileUploadResult result = fileStorageService.upload(testContent, contentType, testUserId);
        String objectKey = result.objectKey();

        System.out.println("[B2 Integration] Uploaded object: " + objectKey);

        assertThat(objectKey).startsWith("invoices/" + testUserId + "/");
        assertThat(objectKey).endsWith(".pdf");

        // Step 2: Verify object exists in B2
        HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(objectKey)
                .build());

        System.out.println("[B2 Integration] Object verified, size: " + headResponse.contentLength() + " bytes");

        assertThat(headResponse.contentLength()).isEqualTo(testContent.length);
        assertThat(headResponse.contentType()).isEqualTo("application/pdf");

        // Step 3: Generate presigned URL
        String presignedUrl = fileStorageService.generatePresignedUrl(objectKey);

        System.out.println("[B2 Integration] Presigned URL generated (length: " + presignedUrl.length() + " chars)");

        assertThat(presignedUrl).startsWith("https://");
        assertThat(presignedUrl).contains(storageProperties.bucket());

        // Step 4: Delete
        fileStorageService.delete(objectKey);

        System.out.println("[B2 Integration] Object deleted: " + objectKey);

        // Step 5: Verify deletion — headObject should throw NoSuchKeyException
        assertThatThrownBy(() -> s3Client.headObject(HeadObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(objectKey)
                .build()))
                .isInstanceOf(NoSuchKeyException.class);

        System.out.println("[B2 Integration] Deletion verified — object no longer exists");
        System.out.println("[B2 Integration] ALL CHECKS PASSED");
    }
}
