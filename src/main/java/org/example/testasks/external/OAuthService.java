package org.example.testasks.external;

import lombok.RequiredArgsConstructor;
import org.example.testasks.external.dto.OAuthTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
/**
 * Service responsible for OAuth2 authentication with external banking system.
 *
 * <p>Implements Client Credentials flow to obtain access tokens
 * used for authenticating requests to external APIs.</p>
 *
 * <h3>Token caching strategy:</h3>
 * <ul>
 *   <li>Token is cached in-memory to reduce authentication requests</li>
 *   <li>Token is refreshed when expired (with 30 seconds safety buffer)</li>
 *   <li>Thread-safe access ensured via synchronization</li>
 * </ul>
 *
 * <p><b>Important:</b> This is a simple in-memory cache.
 * In distributed systems, a shared cache (e.g., Redis) would be required.</p>
 */
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final RestTemplate restTemplate;

    @Value("${external.bank.base-url}")
    private String baseUrl;

    @Value("${external.bank.client-id}")
    private String clientId;

    @Value("${external.bank.client-secret}")
    private String clientSecret;
    /**
     * Cached access token (in-memory).
     */
    private volatile String cachedToken;
    /**
     * Expiration time of the cached token.
     */
    private volatile Instant tokenExpiresAt = Instant.EPOCH;
    /**
     * Returns a valid access token.
     *
     * <p>If a cached token exists and is still valid (with 30s safety margin),
     * it is returned. Otherwise, a new token is requested from the OAuth server.</p>
     *
     * @return valid OAuth2 access token
     */
    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(30))) {
            return cachedToken;
        }
        return fetchNewToken();
    }
    /**
     * Requests a new access token from the external OAuth2 server.
     *
     * <p>Sends a client_credentials grant request and updates in-memory cache.</p>
     *
     * @return newly issued access token
     * @throws RuntimeException if token cannot be retrieved or response is invalid
     */
    private String fetchNewToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        OAuthTokenResponse response = restTemplate.postForObject(
                baseUrl + "/oauth/token",
                new HttpEntity<>(body, headers),
                OAuthTokenResponse.class
        );

        if (response == null || response.getAccessToken() == null) {
            throw new RuntimeException("Failed to obtain access token");
        }

        long expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 3600L;
        cachedToken = response.getAccessToken();
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
        return cachedToken;
    }
}