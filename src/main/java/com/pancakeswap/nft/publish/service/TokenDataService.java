package com.pancakeswap.nft.publish.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TokenDataService {

    private final HttpClient client;

    public TokenDataService() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public CompletableFuture<HttpResponse<String>> callAsync(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(1))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
