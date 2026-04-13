package com.example.download.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_seq")
    private Long fileSeq;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_dir_seq", nullable = false)
    private Directory parent;

    protected FileEntity() {
    }

    public FileEntity(Long fileSeq, String storageKey, String originalFilename, Directory parent) {
        this.fileSeq = fileSeq;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.parent = parent;
    }

    public Long getFileSeq() {
        return fileSeq;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public Directory getParent() {
        return parent;
    }
}
