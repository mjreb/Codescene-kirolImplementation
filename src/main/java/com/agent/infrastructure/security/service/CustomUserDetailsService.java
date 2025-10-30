package com.agent.infrastructure.security.service;

import com.agent.domain.model.Permission;
import com.agent.domain.model.Role;
import com.agent.domain.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public CustomUserDetailsService() {
        initializeDefaultUsers();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(getAuthorities(user))
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }

    /**
     * Get authorities for user based on roles and permissions
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                // Add role authority
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

                // Add permission authorities
                if (role.getPermissions() != null) {
                    for (Permission permission : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(permission.getName()));
                    }
                }
            }
        }

        return authorities;
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    /**
     * Save user
     */
    public User save(User user) {
        users.put(user.getUsername(), user);
        return user;
    }

    /**
     * Initialize default users for testing
     */
    private void initializeDefaultUsers() {
        // Create default permissions
        Permission readConversation = new Permission("1", "conversation:read", "conversation", "read");
        Permission writeConversation = new Permission("2", "conversation:write", "conversation", "write");
        Permission executeTool = new Permission("3", "tool:execute", "tool", "execute");
        Permission adminAccess = new Permission("4", "admin:access", "admin", "access");

        // Create default roles
        Role userRole = new Role("1", "USER", "Standard user role");
        userRole.setPermissions(Set.of(readConversation, writeConversation, executeTool));

        Role adminRole = new Role("2", "ADMIN", "Administrator role");
        adminRole.setPermissions(Set.of(readConversation, writeConversation, executeTool, adminAccess));

        // Create default users
        User defaultUser = new User("1", "user@example.com", "user@example.com");
        defaultUser.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi."); // "password"
        defaultUser.setRoles(Set.of(userRole));

        User adminUser = new User("2", "admin@example.com", "admin@example.com");
        adminUser.setPasswordHash("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi."); // "password"
        adminUser.setRoles(Set.of(adminRole));

        users.put(defaultUser.getUsername(), defaultUser);
        users.put(adminUser.getUsername(), adminUser);
    }
}