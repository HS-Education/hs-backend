package com.hs.hstesis.iam.interfaces.acl;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.queries.GetUserByIdQuery;
import com.hs.hstesis.iam.domain.model.queries.GetUsersByIdsQuery;
import com.hs.hstesis.iam.domain.services.UserQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IamContextFacade {

    private final UserQueryService userQueryService;

    public IamContextFacade(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    private Optional<User> fetchUser(Long userId) {
        if (userId == null) return Optional.empty();
        var query = new GetUserByIdQuery(userId);
        return userQueryService.handle(query);
    }

    private List<User> fetchUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        var query = new GetUsersByIdsQuery(userIds);
        return userQueryService.handle(query);
    }

    public Optional<String> fetchUserNameById(Long userId) {
        return fetchUser(userId)
                .map(User::getName);
    }

    public Map<Long, String> fetchUserNamesByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        var query = new GetUsersByIdsQuery(userIds);
        var users = userQueryService.handle(query);

        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        User::getName
                ));
    }

    public List<Long> getMissingUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();

        var foundIds = fetchUsers(userIds).stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        return userIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
    }

    public List<String> getUserNamesWithoutRole(Set<Long> userIds, String roleName) {
        return fetchUsers(userIds).stream()
                .filter(user -> user.getRoles().stream()
                        .noneMatch(role -> role.getRoleName().equals(roleName)))
                .map(User::getName)
                .toList();
    }

    public boolean existsUserById(Long userId) {
        return fetchUser(userId).isPresent();
    }

    public boolean hasRole(Long userId, String roleName) {
        return fetchUser(userId)
                .map(user -> user.getRoles()
                        .stream()
                        .anyMatch(role -> role.getRoleName().equals(roleName)))
                .orElse(false);
    }
}