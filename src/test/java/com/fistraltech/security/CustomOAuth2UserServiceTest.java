package com.fistraltech.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.security.service.CustomOAuth2UserService;

@DisplayName("CustomOAuth2UserService Tests")
class CustomOAuth2UserServiceTest {

    @Test
    @DisplayName("loadUser_googleExistingUser_updatesLastLogin")
    void loadUser_googleExistingUser_updatesLastLogin() {
        UserRepository repository = Mockito.mock(UserRepository.class);
        OAuth2User oauth2User = oauthUser("alice@example.com", "Alice", "google-123");
        TestCustomOAuth2UserService service = new TestCustomOAuth2UserService(repository, oauth2User);

        User existingUser = new User();
        existingUser.setEmail("alice@example.com");
        existingUser.setProvider("google");
        existingUser.setProviderId("google-123");
        existingUser.setLastLogin(LocalDateTime.of(2026, 1, 1, 8, 0));
        when(repository.findByProviderAndProviderId("google", "google-123")).thenReturn(Optional.of(existingUser));
        when(repository.save(existingUser)).thenReturn(existingUser);

        OAuth2User loadedUser = service.loadUser(userRequest("google"));

        assertEquals(oauth2User, loadedUser);
        assertNotNull(existingUser.getLastLogin());
        verify(repository).save(existingUser);
    }

    @Test
    @DisplayName("loadUser_googleNewUser_createsPersistedUser")
    void loadUser_googleNewUser_createsPersistedUser() {
        UserRepository repository = Mockito.mock(UserRepository.class);
        OAuth2User oauth2User = oauthUser("alice@example.com", "Alice", "google-123");
        TestCustomOAuth2UserService service = new TestCustomOAuth2UserService(repository, oauth2User);

        when(repository.findByProviderAndProviderId("google", "google-123")).thenReturn(Optional.empty());

        service.loadUser(userRequest("google"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());

        User savedUser = captor.getValue();
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("alice_google", savedUser.getUsername());
        assertEquals("Alice", savedUser.getFullName());
        assertEquals("google", savedUser.getProvider());
        assertEquals("google-123", savedUser.getProviderId());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getLastLogin());
    }

    @Test
    @DisplayName("loadUser_appleNewUser_usesAppleAttributes")
    void loadUser_appleNewUser_usesAppleAttributes() {
        UserRepository repository = Mockito.mock(UserRepository.class);
        OAuth2User oauth2User = oauthUser("brie@example.com", "Brie", "apple-456");
        TestCustomOAuth2UserService service = new TestCustomOAuth2UserService(repository, oauth2User);

        when(repository.findByProviderAndProviderId("apple", "apple-456")).thenReturn(Optional.empty());

        service.loadUser(userRequest("apple"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals("brie_apple", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("loadUser_unsupportedProvider_skipsPersistence")
    void loadUser_unsupportedProvider_skipsPersistence() {
        UserRepository repository = Mockito.mock(UserRepository.class);
        OAuth2User oauth2User = oauthUser("dev@example.com", "Dev", "provider-1");
        TestCustomOAuth2UserService service = new TestCustomOAuth2UserService(repository, oauth2User);

        OAuth2User loadedUser = service.loadUser(userRequest("github"));

        assertEquals(oauth2User, loadedUser);
        verify(repository, never()).findByProviderAndProviderId(any(), any());
        verifyNoInteractions(repository);
    }

    private static OAuth2UserRequest userRequest(String registrationId) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth/authorize")
                .tokenUri("https://example.com/oauth/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("sub")
                .clientName(registrationId)
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-value",
                Instant.parse("2026-04-25T10:00:00Z"),
                Instant.parse("2026-04-25T11:00:00Z")
        );

        return new OAuth2UserRequest(registration, accessToken);
    }

    private static OAuth2User oauthUser(String email, String name, String sub) {
        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", email, "name", name, "sub", sub),
                "sub"
        );
    }

    private static final class TestCustomOAuth2UserService extends CustomOAuth2UserService {
        private final OAuth2User delegateUser;

        private TestCustomOAuth2UserService(UserRepository userRepository, OAuth2User delegateUser) {
            super(userRepository);
            this.delegateUser = delegateUser;
        }

        @Override
        protected OAuth2User loadDelegateUser(OAuth2UserRequest userRequest) {
            return delegateUser;
        }
    }
}