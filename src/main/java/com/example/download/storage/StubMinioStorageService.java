package com.example.download.storage;

import com.example.download.storage.config.MinioProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class StubMinioStorageService implements MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(StubMinioStorageService.class);

    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    public StubMinioStorageService(MinioProperties minioProperties, MinioClient minioClient) {
        this.minioProperties = minioProperties;
        this.minioClient = minioClient;
    }

    /**
     * 스토리지 키에 매핑된 파일 조회
     * @param storageKey
     * @return
     */
    @Override
    public InputStream getObject(String storageKey) {
        try {
            log.info("Get object from minio storage: {}", storageKey);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(storageKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object from MinIO: " + storageKey, e);
        }
    }

    /**
     * 스토리지 키에 매핑된 파일의 사이즈 반환
     * @param storageKey
     * @return
     */
    @Override
    public long getObjectSize(String storageKey) {
        log.info("Get object size from minio storage: {}", storageKey);
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(storageKey)
                            .build()
            );

            return stat.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object size from MinIO: " + storageKey, e);
        }
    }
}
