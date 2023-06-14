package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.exception.ListingException;
import com.pancakeswap.nft.publish.service.cache.CacheService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.pancakeswap.nft.publish.model.dto.response.CollectionListingFailedResponse.*;
import static com.pancakeswap.nft.publish.util.FutureUtils.waitFutureRequestFinished;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractNFTService {

    protected final ImageService imageService;
    protected final DBService dbService;
    protected final TokenDataService tokenDataService;
    protected final BlockChainService blockChainService;
    protected final CacheService cacheService;

    private final Map<String, String> tokenResponse = Collections.synchronizedMap(new HashMap<>());

    public void relistNft(String collectionAddress, String[] tokenIds) {
        FutureConfig config = FutureConfig.init();
        String collectionId = dbService.getCollection(collectionAddress).getId();

        Arrays.asList(tokenIds).forEach(tokenId -> {
            try {
                ListCollectionTokenParams params = new ListCollectionTokenParams(collectionId, collectionAddress);
                params.setTokenId(tokenId);
                loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
            } catch (Exception e) {
                log.error("Failed to store token id: {}, collectionId: {}. Error: {}", tokenId, collectionId, e.getMessage());
            }
        });
        postListActions(config, collectionId);
    }

    public CompletableFuture<Boolean> isListingPossible(String collectionAddress) {
        switch (cacheService.add(collectionAddress)) {
            case PROCESSING -> {
                if (dbService.getCollection(collectionAddress) == null) {
                    return CompletableFuture.completedFuture(true);
                } else {
                    throw new ListingException(COLLECTION_ALREADY_EXIST.getMessage());
                }
            }
            case MAX_CACHE_SIZE_REACHED ->
                    throw new ListingException(REACHED_MAX_AMOUNT_OF_CONCURRENT_PROCESSING.getMessage());
            case ALREADY_CACHED -> throw new ListingException(COLLECTION_PROCESSING_ALREADY_IN_PROGRESS.getMessage());
        }
        return CompletableFuture.completedFuture(false);
    }

    protected abstract void loadAndStoreTokenData(FutureConfig config, String body, ListCollectionTokenParams params);

    protected void loadAndStoreTokenDataAsync(FutureConfig config, ListCollectionTokenParams params, AtomicInteger attempt) {
        config.addFuture(() -> {
            try {
                String url = params.getTokenUrl();
                if (url == null) {
                    url = getIpfsFormattedUrl(blockChainService.getTokenURI(params.getCollectionAddress(), new BigInteger(params.getTokenId())));
                    params.setTokenUrl(url);
                }

                if (tokenResponse.get(url) != null) {
                    loadAndStoreTokenData(config, tokenResponse.get(url), params);
                } else {
                    try {
                        HttpResponse<String> res = tokenDataService.call(url);
                        if (res.statusCode() != 200) {
                            loadAndStoreTokenDataAsyncNextAttempt(config, params, attempt, "Response code: " + res.statusCode());
                        } else {
                            tokenResponse.putIfAbsent(url, res.body());
                            loadAndStoreTokenData(config, res.body(), params);
                        }
                    } catch (Exception e) {
                        loadAndStoreTokenDataAsyncNextAttempt(config, params, attempt, e.getMessage());
                    }
                }
            } catch (Exception ex) {
                config.addFailedTokenId(params.getTokenId());
                log.debug("Failed to store token index: {}, id: {}, collectionId: {}", params.getTokenId(), params.getCollectionId(), ex.getMessage());
            }
        });
    }

    private void loadAndStoreTokenDataAsyncNextAttempt(FutureConfig config, ListCollectionTokenParams params, AtomicInteger attempt, String failReason) {
        int attemptValue = attempt.incrementAndGet();
        if (attemptValue < 10) {
            loadAndStoreTokenDataAsync(config, params, attempt);
        } else {
            config.addFailedTokenId(params.getTokenId());
            log.error("Can't fetch token data from: {}. Token id: {}. Attempt: {}. Error message: {}", params.getTokenUrl(), params.getTokenId(), attemptValue, failReason);
        }
    }

    protected void postListActions(FutureConfig config, String collectionId) {
        waitFutureRequestFinished(config);
        log.info("Fetching tokens for collection: {} finished", collectionId);

        if (!config.getTokenIdsFailed().isEmpty()) {
            String failedIds = config.getTokenIdsFailed().stream().sorted(Comparator.comparing(Integer::valueOf)).collect(Collectors.joining(","));
            dbService.storeFailedIds(collectionId, failedIds);
            log.debug("List of failed tokens IDs: {}", failedIds);
        }
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    static class ListCollectionTokenParams {
        private final String collectionId;
        private final String collectionAddress;
        private String tokenId;
        private String tokenUrl;
        private Boolean onlyGif;
        private Boolean isModifiedTokenName;
    }
}
