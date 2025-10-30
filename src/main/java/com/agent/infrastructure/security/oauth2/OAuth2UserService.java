package com.agent.infrastructure.security.oauth2;

import com.agent.domain.model.Role;
import com.agent.domain.model.User;
import com.agent.infrastructure.security.service.CustomUserDetailsService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Custom OAuth2 User Service for handling OAuth2 authentication
 */
@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final CustomUserDetailsService userDetailsService;

    public OAuth2UserService(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        return processOAuth2User(userRequest, oauth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userDetailsService.findByUsername(userInfo.getEmail());
        User user;
        
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, userInfo);
        } else {
            user = registerNewUser(userInfo);
        }

        return new CustomOAuth2User(oauth2User.getAttributes(), user);
    }

    private User registerNewUser(OAuth2UserInfo userInfo) {
        User user = new User();
        user.setId(java.util.UUID.randomUUID().toString());
        user.setUsername(userInfo.getEmail());
        user.setEmail(userInfo.getEmail());
        user.setEnabled(true);
        
        // Assign default user role
        Role userRole = new Role("1", "USER", "Standard user role");
        user.setRoles(Set.of(userRole));
        
        return userDetailsService.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        // Update user information if needed
        return existingUser;
    }
}