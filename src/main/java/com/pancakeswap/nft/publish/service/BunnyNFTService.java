package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pancakeswap.nft.publish.util.GsonUtil.parseBody;

@Service
@Slf4j
public class BunnyNFTService extends AbstractNFTService {

    @Value("${nft.bunny.collection.last.index}")
    private Integer lastIndex;

    public BunnyNFTService(BlockChainService blockChainService, TokenDataService tokenDataService, ImageService imageService, DBService dbService) {
        super(imageService, dbService, tokenDataService, blockChainService);
    }

    public void listOnlyOnePerBunnyID() throws ExecutionException, InterruptedException {
        log.info("fetching tokens started");
        int lastAddedBunnyId = 24;

        BigInteger totalSupply = blockChainService.getTotalSupply();
        String collectionId = dbService.getCollection().getId();

        for (int i = lastIndex; i < totalSupply.intValue(); i++) {
            BigInteger tokenId = null;
            BigInteger bunnyID;
            try {
                tokenId = blockChainService.getTokenId(i);
                bunnyID = blockChainService.getBunnyId(tokenId);
                if (bunnyID.intValue() > lastAddedBunnyId) {
                    loadAndStoreTokenDataAsync(tokenId.toString(), collectionId, new AtomicInteger(0));
                    futureRequests.removeIf(CompletableFuture::isDone);
                    lastAddedBunnyId = bunnyID.intValue();
                }
            } catch (Exception e) {
                if (tokenId != null) {
                    tokenIdsFailed.add(tokenId.toString());
                }
                log.error("failed to store token index: {}, id: {}, collectionId: {}", i, tokenId, collectionId, e);
            }
        }

        waitFutureRequestFinished();
        log.info("fetching tokens finished. LastIndex - {}, lastAddedBunnyId - {}", totalSupply.intValue() - 1, lastAddedBunnyId);
    }

    public void listNFT() throws ExecutionException, InterruptedException {
        log.info("fetching tokens started");

        BigInteger totalSupply = blockChainService.getTotalSupply();
        String collectionId = dbService.getCollection().getId();

        for (int i = lastIndex; i < totalSupply.intValue(); i++) {
            BigInteger tokenId = null;
            try {
                tokenId = blockChainService.getTokenId(i);

                loadAndStoreTokenDataAsync(tokenId.toString(), collectionId, new AtomicInteger(0));
                futureRequests.removeIf(CompletableFuture::isDone);
            } catch (Exception e) {
                if (tokenId != null) {
                    tokenIdsFailed.add(tokenId.toString());
                }
                log.error("failed to store token index: {}, id: {}, collectionId: {}", i, tokenId, collectionId, e);
            }
        }

        waitFutureRequestFinished();
        log.info("fetching tokens finished. LastIndex - {}", totalSupply.intValue() - 1);
    }

    @Override
    protected void loadAndStoreTokenData(String body, String tokenId, String collectionId, String url) {
        try {
            AbstractTokenDto tokenData = parseBody(body);
            tokenData.setTokenId(tokenId);
//            storeTokenImage(tokenData);
            storeBunnyTokenData(collectionId, tokenData);
        } catch (Exception ex) {
            tokenIdsFailed.add(tokenId);
            log.error("Can parse and store token data from: {}. Token id: {}. Error message: {}", url, tokenId, ex.getMessage());
        }
    }

    protected void storeBunnyTokenData(String collectionId, AbstractTokenDto tokenData) {
        futureRequests.offerLast(CompletableFuture.runAsync(
                () -> {
                    try {
                        dbService.storeBunnyToken(collectionId, tokenData);
                    } catch (Exception e) {
                        tokenIdsFailed.add(tokenData.getTokenId());
                        log.error("Can not store token data. Token id: {}, collectionId: {}, Error message: {}", tokenData.getTokenId(), collectionId, e.getMessage());
                    }
                }));
    }
}
