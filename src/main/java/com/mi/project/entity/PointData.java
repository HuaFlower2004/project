package com.mi.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "point_data")
public class PointData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "position_z")
    private Double positionZ;

    @Column
    private Integer classification;

    @Column
    private Integer intensity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_line_id")
    @JsonBackReference
    private PowerLine powerLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_data_id")
    @JsonBackReference
    private MetaData metaData;

    // 批量处理任务ID
    @Column(name = "batch_task_id")
    private String batchTaskId;

    // 便于JSON序列化的辅助方法
    @Transient
    public List<Double> getPosition() {
        return List.of(positionX, positionY, positionZ);
    }

    public void setPosition(List<Double> position) {
        if (position != null && position.size() >= 3) {
            this.positionX = position.get(0);
            this.positionY = position.get(1);
            this.positionZ = position.get(2);
        }
    }
}