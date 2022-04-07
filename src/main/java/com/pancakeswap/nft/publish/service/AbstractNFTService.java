package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Slf4j
public abstract class AbstractNFTService {

    @Value("${nft.collection.only.gif}")
    private Boolean onlyGif;

    protected final ImageService imageService;
    protected final DBService dbService;
    protected final TokenDataService tokenDataService;
    protected final BlockChainService blockChainService;

    protected final Deque<CompletableFuture<?>> futureRequests = new ConcurrentLinkedDeque<>();
    protected final Set<String> tokenIdsFailed = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, String> tokenResponse = Collections.synchronizedMap(new HashMap<>());

    public AbstractNFTService(ImageService imageService, DBService dbService, TokenDataService tokenDataService, BlockChainService blockChainService) {
        this.imageService = imageService;
        this.dbService = dbService;
        this.tokenDataService = tokenDataService;
        this.blockChainService = blockChainService;
    }

    public abstract void listNFT() throws ExecutionException, InterruptedException;

    public void relistNft(List<BigInteger> tokenIds) {
        log.info("fetching tokens started");

        String collectionId = dbService.getCollection().getId();
        tokenIds.forEach(tokenId -> {
            String url = null;
            try {
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(tokenId));

                loadAndStoreTokenDataAsync(tokenId.toString(), collectionId, url, new AtomicInteger(0));
            } catch (Exception e) {
                log.error("failed to store token id: {}, url: {}, collectionId: {}", tokenId, url, collectionId, e);
            }
        });
        waitFutureRequestFinished();
        log.info("fetching tokens finished");
    }

    public void relistNftByIndex(List<Integer> tokenIds) {
        log.info("fetching tokens started");

        String collectionId = dbService.getCollection().getId();
        tokenIds.forEach(i -> {
            String url = null;
            BigInteger tokenId = null;

            try {
                tokenId = blockChainService.getTokenId(i);

                url = getIpfsFormattedUrl(blockChainService.getTokenURI(tokenId));

                loadAndStoreTokenDataAsync(tokenId.toString(), collectionId, url, new AtomicInteger(0));
            } catch (Exception e) {
                log.error("failed to store token id: {}, url: {}, collectionId: {}", tokenId, url, collectionId, e);
            }
        });
        waitFutureRequestFinished();
        log.info("fetching tokens finished");
    }

    protected abstract void loadAndStoreTokenData(String body, String tokenId, String collectionId, String url);

    protected void loadAndStoreTokenDataAsync(String tokenId, String collectionId, AtomicInteger attempt) {
        futureRequests.offerLast(CompletableFuture.runAsync(() -> {
            try {
                String url = getIpfsFormattedUrl(blockChainService.getTokenURI(new BigInteger(tokenId)));

                if (tokenResponse.get(url) != null) {
                    loadAndStoreTokenData(tokenResponse.get(url), tokenId, collectionId, url);
                } else {
                    futureRequests.offerLast(CompletableFuture.runAsync(() -> {
                        try {
                            HttpResponse<String> res = tokenDataService.call(url);
                            if (res.statusCode() != 200) {
                                loadAndStoreTokenDataAsyncNextAttempt(tokenId, collectionId, url, attempt, "Response code: " + res.statusCode());
                            } else {
                                tokenResponse.putIfAbsent(url, res.body());
                                loadAndStoreTokenData(res.body(), tokenId, collectionId, url);
                            }
                        } catch (Exception e) {
                            loadAndStoreTokenDataAsyncNextAttempt(tokenId, collectionId, url, attempt, e.getMessage());
                        }
                    }));
                }
            } catch (Exception ex) {
                log.error("failed to store token index: {}, id: {}, collectionId: {}", tokenId, collectionId, ex.getMessage());
            }
        }));
    }

    protected void loadAndStoreTokenDataAsync(String tokenId, String collectionId, String url, AtomicInteger attempt) {
        futureRequests.offerLast(CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> res = tokenDataService.call(url);
                if (res.statusCode() != 200) {
                    loadAndStoreTokenDataAsyncNextAttempt(tokenId, collectionId, url, attempt, "Response code: " + res.statusCode());
                } else {
                    loadAndStoreTokenData(res.body(), tokenId, collectionId, url);
                }
            } catch (Exception e) {
                loadAndStoreTokenDataAsyncNextAttempt(tokenId, collectionId, url, attempt, e.getMessage());
            }
        }));
    }

    protected void loadAndStoreTokenDataAsyncNextAttempt(String tokenId, String collectionId, String url, AtomicInteger attempt, String failReason) {
        int attemptValue = attempt.incrementAndGet();
        if (attemptValue < 10) {
            loadAndStoreTokenDataAsync(tokenId, collectionId, url, attempt);
        } else {
            tokenIdsFailed.add(tokenId);
            log.error("Can not fetch token data from: {}. Token id: {}. Attempt: {}. Error message: {}", url, tokenId, attemptValue, failReason);
        }
    }

    protected void waitFutureRequestFinished() {
        boolean allAttemptsFinished = false;
        while (!allAttemptsFinished) {
            try {
                CompletableFuture<?> item = futureRequests.pop();
                item.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.trace("Failed blocked waiting", e);
            }

            //Dirty fix for race condition on last item
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }

            futureRequests.removeIf(CompletableFuture::isDone);
            allAttemptsFinished = futureRequests.size() == 0;
        }
        if (!tokenIdsFailed.isEmpty()) {
            log.error("List of failed tokens IDs: {}", tokenIdsFailed.stream().sorted().collect(Collectors.joining(",")));
        }
    }

    //If token 'imagePng' exist we assume that 'image' contain gif
    protected void storeTokenImage(AbstractTokenDto tokenData) {
        if (onlyGif) {
            futureRequests.offerLast(imageService.s3UploadTokenImagesAsync(tokenData.getImage(), tokenData, tokenIdsFailed, TokenMetadata.GIF));
            tokenData.setIsGif(true);
        } else if (Strings.isNotBlank(tokenData.getImagePng())) {
            futureRequests.offerLast(imageService.s3UploadTokenImagesAsync(tokenData.getImagePng(), tokenData, tokenIdsFailed, TokenMetadata.PNG));
            futureRequests.offerLast(imageService.s3UploadTokenImagesAsync(tokenData.getImage(), tokenData, tokenIdsFailed, TokenMetadata.GIF));
            tokenData.setIsGif(true);
        } else if (Strings.isNotBlank(tokenData.getGif())) {
            futureRequests.offerLast(imageService.s3UploadTokenImagesAsync(tokenData.getImage(), tokenData, tokenIdsFailed, TokenMetadata.PNG));
            futureRequests.offerLast(imageService.s3UploadTokenImagesAsync(tokenData.getGif(), tokenData, tokenIdsFailed, TokenMetadata.GIF));
            tokenData.setIsGif(true);
        } else {
            futureRequests.offerLast(imageService.s3UploadTokenImagesAsync(tokenData.getImage(), tokenData, tokenIdsFailed, TokenMetadata.PNG));
        }
    }
}
