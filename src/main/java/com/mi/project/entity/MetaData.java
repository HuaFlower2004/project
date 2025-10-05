
package com.mi.project.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "meta_data")
public class MetaData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Double version;

    @Column
    private String generator;

    @Column
    private String type;

    @Column
    private Integer count;

    // 存储bounds信息
    @Embedded
    private Bounds bounds;

    // 存储分类统计信息
    @ElementCollection
    @CollectionTable(name = "meta_data_classification_stats",
            joinColumns = @JoinColumn(name = "meta_data_id"))
    @MapKeyColumn(name = "classification_key")
    @Column(name = "classification_value")
    @JsonProperty("classification_stats")
    @Builder.Default
    private Map<String, Integer> classificationStats = new HashMap<>();

    @OneToMany(mappedBy = "metaData", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<PointData> pointDatas;

    @Column(name = "original_file")
    @JsonProperty("original_file")
    private String originalFile;

    // 嵌套类用于处理bounds信息
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Bounds {


        private List<Double> min;

        private List<Double> max;

        private List<Double> center;
    }
}
