package com.example.download.service;

import com.example.download.domain.Directory;
import com.example.download.domain.FileEntity;
import com.example.download.exception.DirectoryNotFoundException;
import com.example.download.repository.DirectoryRepository;
import com.example.download.repository.FileRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectoryQueryService {

    private static final Logger log = LoggerFactory.getLogger(DirectoryQueryService.class);

    private final DirectoryRepository directoryRepository;
    private final FileRepository fileRepository;

    public DirectoryQueryService(DirectoryRepository directoryRepository, FileRepository fileRepository) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
    }

    /**
     * 요청 받은 디렉토리 pk 조회
     * @param dirSeq
     * @return
     */
    @Transactional(readOnly = true)
    public Directory getRootDirectory(Long dirSeq) {
        try {
            Directory directory = directoryRepository.findById(dirSeq)
                    .orElseThrow(() -> new DirectoryNotFoundException(dirSeq));
            log.info("Loaded root directory dirSeq={} dirName={}", directory.getDirSeq(), directory.getDirName());
            return directory;
        } catch (DirectoryNotFoundException exception) {
            log.warn("Directory not found dirSeq={}", dirSeq);
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to load root directory dirSeq={}", dirSeq, exception);
            throw exception;
        }
    }

    /**
     * 요청 받은 디렉토리의 하위 디렉토리 조회
     * @param rootDirSeq
     * @return
     */
    @Transactional(readOnly = true)
    public List<Directory> findAllSubDirectories(Long rootDirSeq) {
        try {
            Directory root = getRootDirectory(rootDirSeq);
            List<Directory> result = new ArrayList<>();
            Set<Long> visited = new HashSet<>();
            traverseDirectories(root, result, visited);
            log.info("Loaded {} directories recursively for root dirSeq={}", result.size(), rootDirSeq);
            return result;
        } catch (Exception exception) {
            log.error("Failed to traverse subdirectories for dirSeq={}", rootDirSeq, exception);
            throw exception;
        }
    }

    /**
     * 요청받은 디렉토리 하위의 모든 파일 조회
     * 쿼리로 개선할 여지가 있음
     * @param directories
     * @return
     */
    @Transactional(readOnly = true)
    public List<FileEntity> findAllFilesByDirectories(List<Directory> directories) {
        try {
            List<Long> dirSeqs = directories.stream()
                    .map(Directory::getDirSeq)
                    .toList();
            List<FileEntity> files = fileRepository.findAllByParent_DirSeqIn(dirSeqs);
            log.info("Found {} files", files.size());
            return files;
        } catch (Exception exception) {
            log.error("Failed to load files for directories", exception);
            throw exception;
        }
    }

    /**
     * 현재 디렉토리 기준으로 모든 파일의 전체 경로 반환
     * @param directory
     * @param filename
     * @return
     */
    @Transactional(readOnly = true)
    public String buildFullPath(Directory directory, String filename) {
        try {
            List<String> segments = new ArrayList<>();
            Directory current = directory;
            log.info("Building full path for directory={} dirName={} filename={}", directory.getDirSeq(), directory.getDirName(), filename);

            if (current.getDirName() != null && !current.getDirName().isBlank()) {
                String sanitizedDirName = sanitizePathSegment(current.getDirName());
                if (!sanitizedDirName.isBlank()) {
                    segments.add(sanitizedDirName);
                }
            }

            Collections.reverse(segments);

            String safeFilename = sanitizePathSegment(filename == null || filename.isBlank() ? "unknown-file" : filename);
            if (safeFilename.isBlank()) {
                safeFilename = "unknown-file";
            }
            String[] fileName = safeFilename.split("_");
            segments.add(fileName[fileName.length - 1]);

            String join = String.join("/", segments);
            log.info("Generated full path={}", join);

            return join;
        } catch (Exception exception) {
            log.error("Failed to build full path for filename={}", filename, exception);
            return sanitizeFallbackFilename(filename);
        }
    }

    /**
     * 재귀를 사용해서 요청받은 dirSeq 하위의 파일과 그 하위의 디렉토리 조회
     * @param current
     * @param result
     * @param visited
     */
    private void traverseDirectories(Directory current, List<Directory> result, Set<Long> visited) {
        if (current == null) {
            return;
        }

        Long currentDirSeq = current.getDirSeq();
        if (currentDirSeq != null && !visited.add(currentDirSeq)) {
            log.warn("Skipping already visited directory dirSeq={}", currentDirSeq);
            return;
        }

        result.add(current);

        try {
            List<Directory> children = directoryRepository.findAllByParent_DirSeq(current.getDirSeq());
            for (Directory child : children) {
                traverseDirectories(child, result, visited);
            }
        } catch (Exception exception) {
            log.error("Failed to load child directories for dirSeq={}", current.getDirSeq(), exception);
            throw exception;
        }
    }

    //파일 이름에 허용되지 않는 문자 제거
    private String sanitizePathSegment(String value) {
        log.info("Sanitizing path segment={}", value);
        if (value == null) {
            return "";
        }
        return value.replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "")
                .replace("/", "_");
    }

    private String sanitizeFallbackFilename(String filename) {
        String sanitized = sanitizePathSegment(filename == null || filename.isBlank() ? "unknown-file" : filename);
        return sanitized.isBlank() ? "unknown-file" : sanitized;
    }
}
