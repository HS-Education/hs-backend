package com.hs.hstesis.iam.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String permissionName;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

    protected Permission() {}

    public Permission(String permissionName) {
        this.permissionName = permissionName;
    }
}
