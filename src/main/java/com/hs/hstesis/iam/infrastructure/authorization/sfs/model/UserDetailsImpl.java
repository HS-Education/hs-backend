package com.hs.hstesis.iam.infrastructure.authorization.sfs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.entity.Permission;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@EqualsAndHashCode
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String name;
    private final String username;
    @JsonIgnore
    private final String password;
    private final boolean enabled;

    private final Collection<? extends GrantedAuthority> authorities;

    private final List<String> roles;

    private final boolean accountNonExpired = true;
    private final boolean accountNonLocked = true;
    private final boolean credentialsNonExpired = true;

    public UserDetailsImpl(Long id, String name, String username, String password,
                           boolean enabled, Collection<? extends GrantedAuthority> authorities,
                           List<String> roles) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
        this.roles = roles;
    }

    public static UserDetailsImpl build(User user){
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    Stream<String> roleAuth = Stream.of("ROLE_" + role.getRoleName());
                    Stream<String> permissionAuth = role.getPermissions().stream()
                            .map(Permission::getPermissionName);
                    return Stream.concat(roleAuth, permissionAuth);
                })
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getRoleName())
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                authorities,
                roles);
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return password; }

    @Override
    public @NonNull String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return accountNonExpired; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return credentialsNonExpired; }

    @Override
    public boolean isEnabled() { return enabled; }
}