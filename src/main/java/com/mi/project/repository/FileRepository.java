package com.mi.project.repository;

import com.mi.project.entity.File;
import com.mi.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByUserNameOrderByUploadTimeDesc(String userName);

    long countByUserName(String userName);

    List<File> findByFileStatusOrderByUploadTimeAsc(Integer fileStatus);

}
