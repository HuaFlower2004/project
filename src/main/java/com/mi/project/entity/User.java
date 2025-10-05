package com.mi.project.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name="user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false)
    private String password;

    @Column
    private String email;

    @Column
    private String phoneNumber;

    @Column
    private boolean isActive;

    @Column
    private LocalDateTime createdTime;

    @Column
    private LocalDateTime lastLoginTime;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
//    @JsonIgnore
    private List<File> files;

    public User orElse(Object o) {
        if (o != null) {
            return (User) o;
        }
        return this;
    }
}
