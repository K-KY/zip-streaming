package com.example.download.repository;

import com.example.download.domain.FileEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    @EntityGraph(attributePaths = {"parent"})
    List<FileEntity> findAllByParent_DirSeqIn(Collection<Long> dirSeqs);
}
