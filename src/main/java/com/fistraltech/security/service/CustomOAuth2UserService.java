package com.fistraltech.security.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;

/**
 * Spring Security OAuth2 user service that synchronises provider identities with local users.
 *
 * <p>The service delegates attribute loading to Spring Security, derives the provider-specific
 * identity fields needed by WordAI, and updates or creates the corresponding local user record.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Loads the provider user profile and synchronises it with the local user store.
     *
     * @param userRequest OAuth2 user request from Spring Security
     * @return provider user attributes
     * @throws OAuth2AuthenticationException if the provider user cannot be loaded
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = loadDelegateUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String email = null;
        String name = null;
        String providerId = null;
        
        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("sub");
        } else if ("apple".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("sub");
        }
        
        if (email != null) {
            processOAuthUser(email, name, registrationId, providerId);
        }
        
        return oauth2User;
    }

    /**
     * Delegates provider user loading to the Spring Security base implementation.
     *
     * @param userRequest OAuth2 user request from Spring Security
     * @return provider user attributes
     */
    protected OAuth2User loadDelegateUser(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }
    
    private void processOAuthUser(String email, String name, String provider, String providerId) {
        userRepository.findByProviderAndProviderId(provider, providerId)
            .ifPresentOrElse(
                user -> {
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                },
                () -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(email.split("@")[0] + "_" + provider);
                    newUser.setFullName(name);
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setLastLogin(LocalDateTime.now());
                    userRepository.save(newUser);
                }
            );
    }
}
