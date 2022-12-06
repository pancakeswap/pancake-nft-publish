package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.config.FutureConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pancakeswap.nft.publish.util.FutureUtils.waitFutureRequestFinished;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Slf4j
public abstract class AbstractNFTService {

    protected final ImageService imageService;
    protected final DBService dbService;
    protected final TokenDataService tokenDataService;
    protected final BlockChainService blockChainService;

    private final Map<String, String> tokenResponse = Collections.synchronizedMap(new HashMap<>());

    public AbstractNFTService(ImageService imageService, DBService dbService, TokenDataService tokenDataService, BlockChainService blockChainService) {
        this.imageService = imageService;
        this.dbService = dbService;
        this.tokenDataService = tokenDataService;
        this.blockChainService = blockChainService;
    }

    public void relistNft(String collectionAddress, List<BigInteger> tokenIds) {
        log.info("fetching tokens started");

        FutureConfig config = FutureConfig.init();

        String collectionId = dbService.getCollection(collectionAddress).getId();

        ListCollectionTokenParams params = new ListCollectionTokenParams(collectionId, collectionAddress);

        tokenIds.forEach(tokenId -> {
            String url = null;
            try {
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(collectionAddress, tokenId));
                params.setTokenUrl(url);
                params.setTokenId(tokenId.toString());

                reStoreTokenDataAsync(config, params, new AtomicInteger(0));
            } catch (Exception e) {
                log.error("failed to store token id: {}, url: {}, collectionId: {}", tokenId, url, collectionId, e);
            }
        });
        waitFutureRequestFinished(config);
        log.info("fetching tokens finished");
    }

    protected abstract void loadAndStoreTokenData(FutureConfig config, String body, ListCollectionTokenParams params);

    protected void loadAndStoreTokenDataAsync(FutureConfig config, ListCollectionTokenParams params, AtomicInteger attempt) {
        config.addFuture(CompletableFuture.runAsync(() -> {
            try {
                String url = getIpfsFormattedUrl(blockChainService.getTokenURI(params.getCollectionAddress(), new BigInteger(params.getTokenId())));
                params.setTokenUrl(url);

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
                log.error("failed to store token index: {}, id: {}, collectionId: {}", params.getTokenId(), params.getCollectionId(), ex.getMessage());
            }
        }));
    }

    protected void reStoreTokenDataAsync(FutureConfig config, ListCollectionTokenParams params, AtomicInteger attempt) {
        config.addFuture(CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> res = tokenDataService.call(params.getTokenUrl());
                if (res.statusCode() != 200) {
                    loadAndStoreTokenDataAsyncNextAttempt(config, params, attempt, "Response code: " + res.statusCode());
                } else {
                    loadAndStoreTokenData(config, res.body(), params);
                }
            } catch (Exception e) {
                loadAndStoreTokenDataAsyncNextAttempt(config, params, attempt, e.getMessage());
            }
        }));
    }

    protected void loadAndStoreTokenDataAsyncNextAttempt(FutureConfig config, ListCollectionTokenParams params, AtomicInteger attempt, String failReason) {
        int attemptValue = attempt.incrementAndGet();
        if (attemptValue < 10) {
            loadAndStoreTokenDataAsync(config, params, attempt);
        } else {
            config.addFailedTokenId(params.getTokenId());
            log.error("Can not fetch token data from: {}. Token id: {}. Attempt: {}. Error message: {}", params.getTokenUrl(), params.getTokenId(), attemptValue, failReason);
        }
    }

    @Getter
    @Setter
    class ListCollectionTokenParams {
        private final String collectionId;
        private final String collectionAddress;
        private String tokenId;
        private String tokenUrl;
        private Boolean onlyGif;
        private Boolean isModifiedTokenName;

        ListCollectionTokenParams(String collectionId, String collectionAddress) {
            this.collectionId = collectionId;
            this.collectionAddress = collectionAddress;
        }
    }
}
