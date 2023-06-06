package com.pancakeswap.nft.publish.controller;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.exception.ListingException;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.service.BunnyNFTService;
import com.pancakeswap.nft.publish.service.DBService;
import com.pancakeswap.nft.publish.service.NFTService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Duration;

@RestController
public class CollectionController {

    private static final String SECURE_TOKEN = "x-secure-token";

    @Value(value = "${secure.token}")
    public String accessToken;

    private final NFTService nftService;
    private final BunnyNFTService bunnyNftService;
    private final DBService dbService;

    private final Bucket bucket;

    public CollectionController(NFTService nftService, BunnyNFTService bunnyNftService, DBService dbService) {
        this.nftService = nftService;
        this.bunnyNftService = bunnyNftService;
        this.dbService = dbService;
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @PostMapping(path = "/collections")
    public ResponseEntity<String> listCollection(@Valid @RequestBody CollectionDataDto dataDto,
                                                 @RequestHeader(value = SECURE_TOKEN) String token) {
        if (isValidToken(token)) {
            if (bucket.tryConsume(1)) {
                if (dbService.getCollection(dataDto.getAddress()) == null) {
                    try {
                        FutureConfig config = FutureConfig.init();
                        nftService.storeAvatarAndBanner(config, dataDto.getAddress(), dataDto.getAvatarUrl(), dataDto.getBannerUrl());
                        String result = switch (dataDto.getType()) {
                            case ENUMERABLE -> nftService.listNFT(config, dataDto, 0);
                            case NO_ENUMERABLE ->
                                    nftService.listNoEnumerableNFT(config, dataDto, dataDto.getStartIndex() != null ? dataDto.getStartIndex() : 0);
                            case NO_ENUMERABLE_INFINITE ->
                                    nftService.listNoEnumerableInfiniteNFT(config, dataDto, dataDto.getStartIndex() != null ? dataDto.getStartIndex() : 0);
                            default -> "CollectionType not found";
                        };
                        return ResponseEntity.ok(result);
                    } catch (Exception ex) {
                        throw new ListingException("Failed to list collection");
                    }
                } else {
                    return ResponseEntity.badRequest().body("Collection already exist");
                }
            }

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping(path = "/bunny/collections/{address}")
    public ResponseEntity<String> addBunnyNFt(@PathVariable("address") String address,
                                              @RequestHeader(value = SECURE_TOKEN) String token) {
        if (isValidToken(token)) {
            if (bucket.tryConsume(1)) {
                if (dbService.getCollection(address) == null) {
                    try {
                        bunnyNftService.listOnlyOnePerBunnyID(address);
                        return ResponseEntity.ok("ListOnlyOnePerBunnyID finished");
                    } catch (Exception ex) {
                        throw new ListingException("Failed to list collection");
                    }
                } else {
                    return ResponseEntity.badRequest().body("Collection already exist");
                }
            }

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @DeleteMapping(path = "/collections/{id}")
    public ResponseEntity<String> deleteCollection(@PathVariable("id") String id,
                                                   @RequestHeader(value = SECURE_TOKEN) String token) {
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
