package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.pancakeswap.nft.publish.util.GsonUtil.parseBody;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Service
@Slf4j
public class NFTService {

    @Value("${nft.collection.avatar}")
    private String avatarUrl;
    @Value("${nft.collection.banner}")
    private String bannerUrl;
    @Value("${nft.collection.modify.token.name: true}")
    private Boolean isModifiedTokenName;
    @Value("${nft.collection.only.gif}")
    private Boolean onlyGif;

    private final BlockChainService blockChainService;
    private final TokenDataService tokenDataService;
    private final ImageService imageService;
    private final DBService dbService;

    private final Deque<CompletableFuture<?>> futureRequests = new ConcurrentLinkedDeque<>();
    private final Set<String> tokenIdsFailed = Collections.synchronizedSet(new HashSet<>());

    public NFTService(BlockChainService blockChainService, TokenDataService tokenDataService, ImageService imageService, DBService dbService) {
        this.blockChainService = blockChainService;
        this.tokenDataService = tokenDataService;
        this.imageService = imageService;
        this.dbService = dbService;
    }

    public void listNFT() throws ExecutionException, InterruptedException {
        log.info("fetching tokens started");

        storeAvatarAndBanner();

        BigInteger totalSupply = blockChainService.getTotalSupply();
        String collectionId = dbService.storeCollection(totalSupply.intValue()).getId();

        for (int i = 0; i < totalSupply.intValue(); i++) {
            BigInteger tokenId = null;
            String url = null;
            try {
                tokenId = blockChainService.getTokenId(i);
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(tokenId));

                loadAndStoreTokenDataAsync(tokenId.toString(), collectionId, url, new AtomicInteger(0));
                futureRequests.removeIf(CompletableFuture::isDone);
            } catch (Exception e) {
                if (tokenId != null) {
                    tokenIdsFailed.add(tokenId.toString());
                }
                log.error("failed to store token index: {}, id: {}, url: {}, collectionId: {}", i, tokenId, url, collectionId, e);
            }
        }

        waitFutureRequestFinished();
        log.info("fetching tokens finished");
    }

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

    private void waitFutureRequestFinished() {
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

    private void storeAvatarAndBanner() {
        if (!avatarUrl.isEmpty()) {
            futureRequests.offerLast(imageService.uploadAvatarImage(avatarUrl));
        } else {
            log.info("avatar url is empty");
        }
        if (!bannerUrl.isEmpty()) {
            futureRequests.offerLast(imageService.uploadBannerImage(bannerUrl));
        } else {
            log.info("banner url is empty");
        }
    }

    private void loadAndStoreTokenDataAsync(String tokenId, String collectionId, String url, AtomicInteger attempt) {
        CompletableFuture<HttpResponse<String>> resp = tokenDataService.callAsync(url)
                .whenComplete((res, e) -> {
                    if (e != null) {
                        loadAndStoreTokenDataAsyncNextAttempt(tokenId, collectionId, url, attempt, e.getMessage());
                    } else {
                        if (res.statusCode() != 200) {
                            loadAndStoreTokenDataAsyncNextAttempt(tokenId, collectionId, url, attempt, "Response code: " + res.statusCode());
                        } else {
                            try {
                                AbstractTokenDto tokenData = parseBody(res.body());
                                tokenData.setTokenId(tokenId);
                                if (isModifiedTokenName) {
                                    tokenData.setName(String.format("%s %s", tokenData.getName(), tokenId));
                                }
                                storeTokenImage(tokenData);
                                storeTokenData(collectionId, tokenData);
                            } catch (Exception ex) {
                                tokenIdsFailed.add(tokenId);
                                log.error("Can parse and store token data from: {}. Token id: {}. Error message: {}", url, tokenId, ex.getMessage());
                            }
                        }
                    }
                });

        futureRequests.offerLast(resp);
    }

    private void loadAndStoreTokenDataAsyncNextAttempt(String tokenId, String collectionId, String url, AtomicInteger attempt, String failReason) {
        int attemptValue = attempt.incrementAndGet();
        if (attemptValue < 10) {
            loadAndStoreTokenDataAsync(tokenId, collectionId, url, attempt);
        } else {
            tokenIdsFailed.add(tokenId);
            log.error("Can not fetch token data from: {}. Token id: {}. Attempt: {}. Error message: {}", url, tokenId, attemptValue, failReason);
        }
    }

    private void storeTokenData(String collectionId, AbstractTokenDto tokenData) {
        futureRequests.offerLast(CompletableFuture.runAsync(
                () -> {
                    try {
                        dbService.storeToken(collectionId, tokenData);
                    } catch (Exception e) {
                        tokenIdsFailed.add(tokenData.getTokenId());
                        log.error("Can not store token data. Token id: {}, collectionId: {}, Error message: {}", tokenData.getTokenId(), collectionId, e.getMessage());
                    }
                }));
    }

    //If token 'imagePng' exist we assume that 'image' contain gif
    private void storeTokenImage(AbstractTokenDto tokenData) {
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
