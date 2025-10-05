package com.mi.project.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private final String preFilePath =  System.getProperty("user.dir")+"\\src\\main\\resources\\las";

    @Column
    private String fileUrl;

    @Column(nullable = false)
    private String relativeFilePath;

    @Column
    private String userName;

    @Column(nullable = false)
    private LocalDateTime uploadTime;

    @Column
    private LocalDateTime processStartTime;

    @Column
    private LocalDateTime processEndTime;
    @Column
    private Integer fileStatus;

    @Column
    private Integer fileSize;

    @Column(nullable = false)
    private String fileType;

    @Column(columnDefinition = "TEXT")
    private String processResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
}
