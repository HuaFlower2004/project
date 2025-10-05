package com.mi.project.entity;

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
@Table(name = "power_line")
@Builder
public class PowerLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //杆塔关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_tower_id")
    private PowerTower startTower;  // 起始杆塔

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_tower_id")
    private PowerTower endTower;    // 终止杆塔

    //几何属性
    @Column
    @OneToMany(mappedBy = "powerLine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PointData> coordinates;

    @Column
    private Double totalLength;

    @Column
    private Double maxSag;

    @Column
    private Double minGroundDistance;

    //物理工程属性
    private String conductorModel;        // 导线型号

    private String material;              // 导线材质

    private String voltageLevel;          // 电压等级

    private String towerNumbers;          // 杆塔编号（多个用逗号分隔）

    //状态评估属性
    private Double wearDegree;            // 磨损程度(%)

    private Double treeLineRisk;          // 树线矛盾风险值

}
