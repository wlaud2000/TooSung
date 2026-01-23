package com.project.toosung_back.global.s3.service;

import com.project.toosung_back.global.s3.dto.S3DTO;
import com.project.toosung_back.global.s3.exception.S3ErrorCode;
import com.project.toosung_back.global.s3.exception.S3Exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(10);

    public S3DTO.PresignedUrlResponse generatePresignedUrl(String directory, String fileName) {
        String extension = extractExtension(fileName).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new S3Exception(S3ErrorCode.INVALID_FILE_TYPE);
        }

        String key = directory + "/" + UUID.randomUUID() + extension;
        String contentType = getContentType(extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_DURATION)
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String fileUrl = getFileUrl(key);

        return S3DTO.PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .fileUrl(fileUrl)
                .build();
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String fileName = extractFileName(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.warn("S3 파일 삭제 실패: {}", e.getMessage());
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private String getContentType(String extension) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    private String extractFileName(String fileUrl) {
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
        return fileUrl.replace(prefix, "");
    }
}
