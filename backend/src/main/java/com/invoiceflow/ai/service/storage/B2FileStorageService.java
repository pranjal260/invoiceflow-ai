package com.invoiceflow.ai.service.storage;

import com.invoiceflow.ai.config.StorageProperties;
import com.invoiceflow.ai.exception.FileStorageException;
import java.time.Duration;
import java.util.UUID;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Backblaze B2 implementation of {@link FileStorageService} using the S3-compatible API.
 *
 * All provider-specific details are contained in this class. Business logic interacts
 * only through the {@link FileStorageService} interface.
 *
 * Object keys follow the pattern: invoices/{userId}/{uuid}.{extension}
 * where userId and uuid are server-generated, and extension is derived from the
 * validated MIME type — never from a client-supplied filename.
 */
public class B2FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileValidator fileValidator;
    private final String bucket;
    private final Duration presignedUrlExpiration;

    public B2FileStorageService(S3Client s3Client, S3Presigner s3Presigner, FileValidator fileValidator,
                                StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.fileValidator = fileValidator;
        this.bucket = properties.bucket();
        this.presignedUrlExpiration = Duration.ofMinutes(properties.presignedUrlExpirationMinutes());
    }

    @Override
    public FileUploadResult upload(byte[] fileBytes, String contentType, UUID userId) {
        fileValidator.validate(fileBytes, contentType);

        String extension = fileValidator.extensionForContentType(contentType);
        String objectKey = buildObjectKey(userId, extension);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to upload file.", e);
        }

        return new FileUploadResult(objectKey);
    }

    @Override
    public String generatePresignedUrl(String objectKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignedUrlExpiration)
                .getObjectRequest(getRequest)
                .build();

        try {
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to generate file access URL.", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to delete file.", e);
        }
    }

    @Override
    public FileDownloadResult download(String objectKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getRequest);
            return new FileDownloadResult(responseBytes.asByteArray(), responseBytes.response().contentType());
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to download file.", e);
        }
    }

    String buildObjectKey(UUID userId, String extension) {
        return "invoices/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }
}
