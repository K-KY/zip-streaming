package com.example.download.repository;

import com.example.download.domain.Directory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {

    List<Directory> findAllByParent_DirSeq(Long parentDirSeq);
}
