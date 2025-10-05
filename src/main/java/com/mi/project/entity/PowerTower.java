package com.mi.project.entity;

import java.time.LocalDateTime;
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
@Table(name = "power_tower")
public class PowerTower {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //基本信息
    @Column(unique = true, nullable = false)
    private String towerNumber;  // 杆塔编号

    //几何属性
    @Column
    private Double height;       // 杆塔高度（米）

    @Column
    private Double tiltAngle;    // 倾斜角度（度）

    //结构属性
    @Column
    private String materialType;  // 材质类型

    @Column
    private Double designLoad;   // 设计荷载（kN）

    //电气属性
    @Column
    private String voltageLevel;  // 电压等级（如：110kV、220kV）

    //技术参数
    @Column
    private Integer serviceLife;     // 设计使用年限（年）

    //运维属性
    @Column
    private LocalDateTime constructionDate;  // 建设日期

    @Column
    private Integer maintenanceCycle;  // 维护周期（月）

    @Column
    private LocalDateTime lastMaintenanceDate;  // 上次维护日期

    //关联关系
    @OneToMany(mappedBy = "startTower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PowerLine> startTowerLines;  // 作为起始杆塔的线路

    @OneToMany(mappedBy = "endTower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PowerLine> endTowerLines;    // 作为终止杆塔的线路

}
