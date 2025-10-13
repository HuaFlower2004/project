package com.mi.project.repository;

import com.mi.project.entity.PointData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 31591
 */
@Repository
public interface PointDataRepository extends JpaRepository<PointData, Long> {

    List<PointData> findByMetaDataId(Long metaDataId);

    @Query("SELECT p FROM PointData p WHERE p.metaData.id = :metaDataId AND p.classification = :classification")
    List<PointData> findByMetaDataIdAndClassification(@Param("metaDataId") Long metaDataId,
                                                      @Param("classification") Integer classification);

    List<PointData> findByBatchTaskId(String batchTaskId);

    void deleteByBatchTaskId(String batchTaskId);
}

