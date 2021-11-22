package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.dto.TokenDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.pancakeswap.nft.publish.util.GsonUtil.parseBody;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Service
@Slf4j
public class NFTService {

    @Value("${nft.collection.address}")
    private String contract;
    @Value("${nft.connection.avatar}")
    private String avatarUrl;
    @Value("${nft.connection.banner}")
    private String bannerUrl;
    @Value("${nft.connection.modify.token.name: true}")
    private Boolean isModifiedTokenName;

    private final BlockChainService blockChainService;
    private final TokenDataService tokenDataService;
    private final ImageService imageService;
    private final DBService dbService;

    private final List<CompletableFuture<?>> futureRequests = Collections.synchronizedList(new LinkedList<>());
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

        if (!tokenIdsFailed.isEmpty()) {
            log.error("List of failed tokens IDs: {}", tokenIdsFailed.stream().sorted().collect(Collectors.joining(",")));
        }
    }

    public void relistNft(List<BigInteger> tokenIds) {
        String collectionId = dbService.getCollection().getId();
        tokenIds.forEach(tokenId -> {
            String url = null;
            try {
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(tokenId));
                loadAndStoreTokenDataAsync(tokenId.toString(), collectionId, url, new AtomicInteger(0));
                futureRequests.removeIf(CompletableFuture::isDone);
            } catch (Exception e) {
                log.error("failed to store token id: {}, url: {}, collectionId: {}", tokenId, url, collectionId, e);
            }
        });
        waitFutureRequestFinished();
    }

    private void waitFutureRequestFinished() {
        boolean allAttemptsFinished = false;
        while (!allAttemptsFinished) {
            for(int i = 0; i < futureRequests.size(); i++) {
                try {
                    CompletableFuture<?> item = futureRequests.get(i);
                    futureRequests.remove(item);
                    item.get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.trace("Failed blocked waiting", e);
                }
            }

            futureRequests.removeIf(CompletableFuture::isDone);
            allAttemptsFinished = futureRequests.size() == 0;
        }
    }

    private void storeAvatarAndBanner() {
        futureRequests.add(imageService.uploadBannerImage(bannerUrl, Keys.toChecksumAddress(contract)));
        futureRequests.add(imageService.uploadAvatarImage(avatarUrl, Keys.toChecksumAddress(contract)));
    }

    private void loadAndStoreTokenDataAsync(String tokenId, String collectionId, String url, AtomicInteger attempt) {
        CompletableFuture<HttpResponse<String>> resp = tokenDataService.callAsync(url)
                .whenComplete((res, e) -> {
                    if (e != null) {
                        int attemptValue = attempt.incrementAndGet();
                        if (attemptValue < 10) {
                            loadAndStoreTokenDataAsync(tokenId, collectionId, url, attempt);
                        } else {
                            tokenIdsFailed.add(tokenId);
                            log.error("Can not fetch token data from: {}. Token id: {}. Attempt: {}. Error message: {}", url, tokenId, attemptValue, e.getMessage());
                        }
                    } else {
                        TokenDataDto tokenData = parseBody(res.body());
                        tokenData.setTokenId(tokenId);
                        if (isModifiedTokenName) {
                            tokenData.setName(String.format("%s %s", tokenData.getName(), tokenId));
                        }
                        storeTokenImage(tokenData);
                        storeTokenData(collectionId, tokenData);
                    }
                });

        futureRequests.add(resp);
        futureRequests.removeIf(CompletableFuture::isDone);
    }

    private void storeTokenData(String collectionId, TokenDataDto tokenData) {
        futureRequests.add(CompletableFuture.runAsync(() -> {
            dbService.storeToken(collectionId, tokenData);
        }));
    }

    private void storeTokenImage(TokenDataDto tokenData) {
        String imageUrl = tokenData.getImagePng();
        if (imageUrl == null || !imageUrl.endsWith(".png")) {
            imageUrl = tokenData.getImage();
        }

        futureRequests.add(imageService.s3SyncUploadTokenImages(imageUrl, Keys.toChecksumAddress(contract), tokenData, tokenIdsFailed));
        futureRequests.removeIf(CompletableFuture::isDone);
    }
}
