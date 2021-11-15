package com.pancakeswap.nft.publish.service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pancakeswap.nft.publish.exception.HttpException;
import com.pancakeswap.nft.publish.model.dto.TokenDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TokenDataService {

    private final HttpClient client;
    private final Gson gson;

    public TokenDataService() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    public TokenDataDto call(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = null;
        for (int i = 0; i < 10; i++) {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                break;
            } catch (Exception e) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignore) {
                }
                log.error("Can not fetch token data from: {}. Attempt: {}. Error message: {}", url, i, e.getMessage());
            }
        }
        if (response == null) {
            throw new HttpException("Can not fetch token data");
        }

        return gson.fromJson(response.body(), TokenDataDto.class);
    }

    public CompletableFuture<HttpResponse<String>> callAsync(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(1))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
