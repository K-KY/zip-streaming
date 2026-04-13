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
@Table(name = "directory")
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dir_seq")
    private Long dirSeq;

    @Column(name = "dir_name", nullable = false, length = 255)
    private String dirName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_dir_seq")
    private Directory parent;

    protected Directory() {
    }

    public Directory(Long dirSeq, String dirName, Directory parent) {
        this.dirSeq = dirSeq;
        this.dirName = dirName;
        this.parent = parent;
    }

    public Long getDirSeq() {
        return dirSeq;
    }

    public String getDirName() {
        return dirName;
    }

    public Directory getParent() {
        return parent;
    }
}
