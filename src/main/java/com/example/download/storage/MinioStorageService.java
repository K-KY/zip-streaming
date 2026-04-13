package com.example.download.storage;

import java.io.InputStream;

public interface MinioStorageService {

    InputStream getObject(String storageKey);

    default long getObjectSize(String storageKey) {
        return -1L;
    }
}
