package com.example.download.service;

import com.example.download.domain.Directory;
import com.example.download.domain.FileEntity;
import com.example.download.storage.MinioStorageService;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ZipDownloadService {

    private static final Logger log = LoggerFactory.getLogger(ZipDownloadService.class);
    private static final int BUFFER_SIZE = 8 * 1024;

    private final DirectoryQueryService directoryQueryService;
    private final MinioStorageService minioStorageService;

    public ZipDownloadService(
            DirectoryQueryService directoryQueryService,
            MinioStorageService minioStorageService
    ) {
        this.directoryQueryService = directoryQueryService;
        this.minioStorageService = minioStorageService;
    }

    public void streamZip(Long dirSeq, OutputStream outputStream) throws IOException {
        //디렉토리 조회
        Directory rootDirectory = directoryQueryService.getRootDirectory(dirSeq);
        log.info("After loading directory dirSeq={} dirName={}", rootDirectory.getDirSeq(), rootDirectory.getDirName());

        //dirSeq 하위의 모든 디렉토리 조회
        List<Directory> directories = directoryQueryService.findAllSubDirectories(dirSeq);
        log.info("After loading subdirectories count={} dirSeq={}", directories.size(), dirSeq);

        //dirSeq 하위의 디렉토리의 파일을 포함한 모든 파일 조회
        List<FileEntity> files = directoryQueryService.findAllFilesByDirectories(directories);
        log.info("After loading files count={} dirSeq={}", files.size(), dirSeq);

        Set<String> directoryEntries = new HashSet<>();
        Set<String> fileEntries = new HashSet<>();

        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER_SIZE);
             ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream)) {

            //압축 안함
            zipOutputStream.setLevel(Deflater.NO_COMPRESSION);

            for (FileEntity file : files) {
                String fullPath = directoryQueryService
                        .buildFullPath(file.getParent(), file.getStorageKey());
                String normalizedPath = normalizeEntryPath(fullPath);

                if (!fileEntries.add(normalizedPath)) {
                    log.warn("Duplicate ZIP file entry detected. use storageKey path={}", normalizedPath);
                    continue;
                }

                writeDirectoryEntries(zipOutputStream, normalizedPath, directoryEntries);
                streamFileSafely(zipOutputStream, file, normalizedPath);
            }

            zipOutputStream.finish();
            zipOutputStream.flush();
            bufferedOutputStream.flush();
            log.info("ZIP stream finished successfully dirSeq={}", dirSeq);
        }
    }

    private void streamFileSafely(ZipOutputStream zipOutputStream, FileEntity file, String fullPath) {
        try {
            streamFile(zipOutputStream, file, fullPath);
        } catch (Exception exception) {
            log.error("Skipping file after streaming failure storageKey={} path={}", file.getStorageKey(), fullPath, exception);
        }
    }

    private void streamFile(ZipOutputStream zipOutputStream, FileEntity file, String fullPath) {
        long fileSize = minioStorageService.getObjectSize(file.getStorageKey());
        log.info("Writing file: {} size={}", fullPath, fileSize);

        boolean entryOpened = false;
        long totalBytesWritten = 0L;

        try (InputStream inputStream = openMinioStream(file.getStorageKey())) {
            if (inputStream == null) {
                log.warn("MinIO returned null stream for storageKey={}. Skipping file", file.getStorageKey());
                return;
            }

            log.info("Entry start path={}", fullPath);
            zipOutputStream.putNextEntry(new ZipEntry(fullPath));
            entryOpened = true;

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, len);
                totalBytesWritten += len;
            }

            log.info("Bytes written file={} bytes={}", fullPath, totalBytesWritten);
        } catch (FileNotFoundException | NoSuchFileException exception) {
            log.warn("MinIO object not found for storageKey={}. Skipping file", file.getStorageKey(), exception);
        } catch (IOException exception) {
            log.error("Failed to stream file: {}", file.getStorageKey(), exception);
        } catch (Exception exception) {
            log.error("Unexpected failure while streaming file: {}", file.getStorageKey(), exception);
        } finally {
            if (entryOpened) {
                try {
                    zipOutputStream.closeEntry();
                    log.info("Entry end path={}", fullPath);
                } catch (IOException exception) {
                    log.error("Failed to close ZIP entry path={}", fullPath, exception);
                }
            }
        }
    }

    private void writeDirectoryEntries(
            ZipOutputStream zipOutputStream,
            String fullPath,
            Set<String> directoryEntries
    ) throws IOException {
        int searchIndex = 0;
        log.info("Searching for directory entries for storageKey={}", fullPath);
        while (true) {
            int slashIndex = fullPath.indexOf('/', searchIndex);
            if (slashIndex < 0) {
                return;
            }

            String directoryPath = fullPath.substring(0, slashIndex + 1);
            if (directoryEntries.add(directoryPath)) {
                log.info("Directory entry start path={}", directoryPath);
                zipOutputStream.putNextEntry(new ZipEntry(directoryPath));
                zipOutputStream.closeEntry();
                log.info("Directory entry end path={}", directoryPath);
            }
            searchIndex = slashIndex + 1;
        }
    }

    private InputStream openMinioStream(String storageKey) throws IOException {
        try {
            log.info("Opening MinIO stream storageKey={}", storageKey);
            return minioStorageService.getObject(storageKey);
        } catch (Exception exception) {
            log.error("Unexpected MinIO access failure storageKey={}", storageKey, exception);
            throw new IOException("Failed to open MinIO stream for " + storageKey, exception);
        }
    }

    private String normalizeEntryPath(String entryName) {
        String normalized = entryName == null ? "" : entryName.replace('\\', '/');
        normalized = normalized.replaceAll("^/+", "").replaceAll("/{2,}", "/");
        return normalized.isBlank() ? "unknown-file" : normalized;
    }
}
