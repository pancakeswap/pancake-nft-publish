package com.pancakeswap.nft.publish.controller;

import com.pancakeswap.nft.publish.exception.ListingException;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.service.BunnyNFTService;
import com.pancakeswap.nft.publish.service.NFTService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Duration;
import java.util.Optional;

import static com.pancakeswap.nft.publish.model.dto.response.CollectionListingFailedResponse.COLLECTION_LISTING_HAS_BEEN_INITIATED;
import static com.pancakeswap.nft.publish.model.dto.response.CollectionListingFailedResponse.FAILED_TO_LIST_COLLECTION;

@Slf4j
@RestController
public class CollectionController {

    private static final String SECURE_TOKEN = "x-secure-token";
    private static final Bandwidth LIMIT = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
    protected final Bucket bucket = Bucket.builder()
            .addLimit(LIMIT)
            .build();

    @Value(value = "${secure.token}")
    public String accessToken;

    private final NFTService nftService;
    private final BunnyNFTService bunnyNftService;

    public CollectionController(NFTService nftService, BunnyNFTService bunnyNftService) {
        this.nftService = nftService;
        this.bunnyNftService = bunnyNftService;
    }

    @PostMapping(path = "/collections")
    public ResponseEntity<String> listCollection(@Valid @RequestBody CollectionDataDto dataDto,
                                                 @RequestHeader(value = SECURE_TOKEN) String secureToken) {
        Optional<ResponseEntity<String>> responseEntity = isRequestNotAllowed(secureToken);
        if (responseEntity.isPresent()) {
            return responseEntity.get();
        }
        try {
            nftService.isListingPossible(dataDto.getAddress()).thenRunAsync(() -> {
                try {
                    nftService.listNFTs(dataDto);
                } catch (Exception e) {
                    log.error(FAILED_TO_LIST_COLLECTION.getMessage(), e);
                }
            });
            return ResponseEntity.ok(COLLECTION_LISTING_HAS_BEEN_INITIATED.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping(path = "/bunny/collections/{address}")
    public ResponseEntity<String> addBunnyNFt(@PathVariable("address") String address,
                                              @RequestHeader(value = SECURE_TOKEN) String secureToken) {
        Optional<ResponseEntity<String>> responseEntity = isRequestNotAllowed(secureToken);
        if (responseEntity.isPresent()) {
            return responseEntity.get();
        }
        try {
            bunnyNftService.isListingPossible(address).thenRunAsync(() -> {
                try {
                    bunnyNftService.listNFTs(address);
                } catch (Exception e) {
                    log.error(FAILED_TO_LIST_COLLECTION.getMessage(), e);
                }
            });
            return ResponseEntity.ok(COLLECTION_LISTING_HAS_BEEN_INITIATED.getMessage());
        } catch (ListingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping(path = "/collections/{id}")
    public ResponseEntity<String> deleteCollection(@PathVariable("id") String collectionId,
                                                   @RequestHeader(value = SECURE_TOKEN) String secureToken) {
        Optional<ResponseEntity<String>> responseEntity = isRequestNotAllowed(secureToken);
        if (responseEntity.isPresent()) {
            return responseEntity.get();
        }
        if (nftService.deleteCollection(collectionId)) {
            return ResponseEntity.ok("Deleted");
        } else {
            return ResponseEntity.badRequest().body(FAILED_TO_LIST_COLLECTION.getMessage());
        }
    }

    private Optional<ResponseEntity<String>> isRequestNotAllowed(String secureToken) {
        if (isValidToken(secureToken)) {
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        if (!bucket.tryConsume(1)) {
            return Optional.of(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
        }
        return Optional.empty();
    }

    private boolean isValidToken(String token) {
        return !accessToken.equals(token);
    }
}
