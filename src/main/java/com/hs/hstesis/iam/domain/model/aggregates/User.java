package com.hs.hstesis.iam.domain.model.aggregates;

import com.hs.hstesis.iam.domain.model.commands.CreateUserCommand;
import com.hs.hstesis.iam.domain.model.entity.Role;
import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class User extends AuditableAbstractAggregateRoot<User> {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, name = "is_active")
    private boolean isActive;

    @Column(name = "is_temporary_password", columnDefinition = "boolean default true")
    private boolean isTemporaryPassword;

    @Column(name = "last_password_change", columnDefinition = "timestamp default current_timestamp")
    private java.time.LocalDateTime lastPasswordChange;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    protected User () {}

    public User(CreateUserCommand command){
        this.name = command.name();
        this.username = command.username();
        this.passwordHash = command.passwordHash();
        this.isActive = true;
        this.isTemporaryPassword = true; // Default to true when created
        this.lastPasswordChange = java.time.LocalDateTime.now();
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }
}