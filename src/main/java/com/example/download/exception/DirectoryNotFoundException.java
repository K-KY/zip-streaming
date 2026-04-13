package com.example.download.exception;

public class DirectoryNotFoundException extends RuntimeException {

    public DirectoryNotFoundException(Long dirSeq) {
        super("Directory not found for dirSeq=" + dirSeq);
    }
}
