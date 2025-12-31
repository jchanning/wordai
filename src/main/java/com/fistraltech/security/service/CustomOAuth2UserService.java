package com.fistraltech.security.service;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
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
