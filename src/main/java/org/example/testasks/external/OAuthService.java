package org.example.testasks.external;

import lombok.RequiredArgsConstructor;
import org.example.testasks.external.dto.OAuthTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

    public String getAccessToken() {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        OAuthTokenResponse response =
                restTemplate.postForObject(
                        baseUrl + "/oauth/token",
                        request,
                        OAuthTokenResponse.class
                );

        if (response == null || response.getAccessToken() == null) {
            throw new RuntimeException("Failed to obtain access token");
        }

        return response.getAccessToken();
    }
}