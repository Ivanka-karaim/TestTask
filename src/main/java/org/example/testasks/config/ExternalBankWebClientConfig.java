package org.example.testasks.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class ExternalBankWebClientConfig {

    @Bean(name = "externalBankWebClient")
    public WebClient externalBankWebClient(WebClient.Builder builder,
                                          @Value("${external.bank.base-url:http://localhost:8084/mock/api}") String baseUrl,
                                          @Value("${external.bank.connect-timeout-ms:2000}") int connectTimeoutMs,
                                          @Value("${external.bank.response-timeout-ms:5000}") int responseTimeoutMs) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        return builder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

}
