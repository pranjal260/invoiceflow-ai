package com.invoiceflow.ai.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.config.StorageProperties;
import com.invoiceflow.ai.exception.FileStorageException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class B2FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private B2FileStorageService storageService;

    private static final String BUCKET = "test-bucket";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        FileValidator fileValidator = new FileValidator();
        StorageProperties properties = new StorageProperties(
                "https://s3.us-west-004.backblazeb2.com",
                "us-west-004",
                BUCKET,
                "test-key-id",
                "test-secret-key",
                15
        );
        storageService = new B2FileStorageService(s3Client, s3Presigner, fileValidator, properties);
    }

    @Test
    void uploadStoresFileWithCorrectContentType() {
        byte[] content = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        FileUploadResult result = storageService.upload(content, "application/pdf", USER_ID);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest captured = captor.getValue();
        assertThat(captured.bucket()).isEqualTo(BUCKET);
        assertThat(captured.contentType()).isEqualTo("application/pdf");
        assertThat(result.objectKey()).isNotNull();
    }

    @Test
    void uploadGeneratesObjectKeyWithCorrectFormat() {
        byte[] content = new byte[]{0x25};
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        FileUploadResult result = storageService.upload(content, "application/pdf", USER_ID);

        assertThat(result.objectKey()).startsWith("invoices/" + USER_ID + "/");
        assertThat(result.objectKey()).endsWith(".pdf");
    }

    @Test
    void uploadGeneratesJpgExtensionForJpeg() {
        byte[] content = new byte[]{(byte) 0xFF};
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        FileUploadResult result = storageService.upload(content, "image/jpeg", USER_ID);

        assertThat(result.objectKey()).endsWith(".jpg");
    }

    @Test
    void uploadGeneratesPngExtensionForPng() {
        byte[] content = new byte[]{(byte) 0x89};
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        FileUploadResult result = storageService.upload(content, "image/png", USER_ID);

        assertThat(result.objectKey()).endsWith(".png");
    }

    @Test
    void uploadGeneratesUniqueKeysForSameUser() {
        byte[] content = new byte[]{0x25};
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        FileUploadResult result1 = storageService.upload(content, "application/pdf", USER_ID);
        FileUploadResult result2 = storageService.upload(content, "application/pdf", USER_ID);

        assertThat(result1.objectKey()).isNotEqualTo(result2.objectKey());
    }

    @Test
    void uploadWrapsS3ExceptionIntoFileStorageException() {
        byte[] content = new byte[]{0x25};
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 failure").build());

        assertThatThrownBy(() -> storageService.upload(content, "application/pdf", USER_ID))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Failed to upload file.")
                .hasCauseInstanceOf(S3Exception.class);
    }

    @Test
    void deleteCallsS3WithCorrectBucketAndKey() {
        String objectKey = "invoices/" + USER_ID + "/test.pdf";

        storageService.delete(objectKey);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest captured = captor.getValue();
        assertThat(captured.bucket()).isEqualTo(BUCKET);
        assertThat(captured.key()).isEqualTo(objectKey);
    }

    @Test
    void deleteWrapsS3ExceptionIntoFileStorageException() {
        String objectKey = "invoices/" + USER_ID + "/test.pdf";
        doThrow(S3Exception.builder().message("Delete failed").build())
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertThatThrownBy(() -> storageService.delete(objectKey))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Failed to delete file.")
                .hasCauseInstanceOf(S3Exception.class);
    }

    @Test
    void generatePresignedUrlReturnsUrlString() throws MalformedURLException {
        String objectKey = "invoices/" + USER_ID + "/test.pdf";
        URL expectedUrl = URI.create("https://s3.us-west-004.backblazeb2.com/test-bucket/test.pdf?signed=true").toURL();
        when(presignedGetObjectRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        String url = storageService.generatePresignedUrl(objectKey);

        assertThat(url).isEqualTo(expectedUrl.toString());
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void generatePresignedUrlWrapsS3ExceptionIntoFileStorageException() {
        String objectKey = "invoices/" + USER_ID + "/test.pdf";
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(S3Exception.builder().message("Presign failed").build());

        assertThatThrownBy(() -> storageService.generatePresignedUrl(objectKey))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Failed to generate file access URL.")
                .hasCauseInstanceOf(S3Exception.class);
    }

    @Test
    void objectKeyUsesServerGeneratedUuidNotClientInput() {
        String objectKey = storageService.buildObjectKey(USER_ID, "pdf");

        assertThat(objectKey).startsWith("invoices/" + USER_ID + "/");
        // Extract the UUID portion between the last "/" and ".pdf"
        String fileName = objectKey.substring(objectKey.lastIndexOf("/") + 1, objectKey.lastIndexOf("."));
        assertThat(UUID.fromString(fileName)).isNotNull();
    }

    @Test
    void downloadReturnsBytesAndContentType() {
        String objectKey = "invoices/" + USER_ID + "/test.pdf";
        byte[] content = new byte[]{0x25, 0x50, 0x44, 0x46};
        GetObjectResponse getResponse = GetObjectResponse.builder().contentType("application/pdf").build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(getResponse, content);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        FileDownloadResult result = storageService.download(objectKey);

        assertThat(result.content()).isEqualTo(content);
        assertThat(result.contentType()).isEqualTo("application/pdf");

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo(objectKey);
    }

    @Test
    void downloadWrapsS3ExceptionIntoFileStorageException() {
        String objectKey = "invoices/" + USER_ID + "/test.pdf";
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Download failed").build());

        assertThatThrownBy(() -> storageService.download(objectKey))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Failed to download file.")
                .hasCauseInstanceOf(S3Exception.class);
    }
}
