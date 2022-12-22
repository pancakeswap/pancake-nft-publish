package com.pancakeswap.nft.publish.controller;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.exception.ListingException;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.service.DBService;
import com.pancakeswap.nft.publish.service.NFTService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import java.time.Duration;

@RestController
public class CollectionController {

    @Value(value = "${secure.token}")
    public String accessToken;

    private final NFTService nftService;
    private final DBService dbService;

    private final Bucket bucket;

    public CollectionController(NFTService nftService, DBService dbService) {
        this.nftService = nftService;
        this.dbService = dbService;
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @PostMapping(path = "/collections")
    public ResponseEntity<String> listCollection(@Valid @RequestBody CollectionDataDto dataDto,
                                                 @RequestHeader(value = "x-secure-token") String token) {
        if (isValidToken(token)) {
            if (bucket.tryConsume(1)) {
                try {
                    FutureConfig config = FutureConfig.init();
                    nftService.storeAvatarAndBanner(config, dataDto.getAddress(), dataDto.getAvatarUrl(), dataDto.getBannerUrl());
                    String result = nftService.listNFT(config, dataDto, 0);
                    return ResponseEntity.ok(result);
                } catch (Exception ex) {
                    throw new ListingException("Failed to list collection");
                }
            }

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @DeleteMapping(path = "/collections/{id}")
    public ResponseEntity<String> deleteCollection(@PathVariable("id") String id,
                                                   @RequestHeader(value = "x-secure-token") String token) {
        if (isValidToken(token)) {
            if (bucket.tryConsume(1)) {
                try {
                    dbService.deleteCollection(id);
                    return ResponseEntity.ok("Done");
                } catch (Exception ex) {
                    throw new ListingException("Failed to list collection");
                }
            }
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private boolean isValidToken(String token) {
        return accessToken.equals(token);
    }
}
