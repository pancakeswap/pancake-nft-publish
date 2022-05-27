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
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Service
@Slf4j
public class NFTService extends AbstractNFTService {

    @Value("${nft.collection.avatar}")
    private String avatarUrl;
    @Value("${nft.collection.banner}")
    private String bannerUrl;
    @Value("${nft.collection.modify.token.name: true}")
    private Boolean isModifiedTokenName;

    public NFTService(BlockChainService blockChainService, TokenDataService tokenDataService, ImageService imageService, DBService dbService) {
        super(imageService, dbService, tokenDataService, blockChainService);
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
//                tokenId = BigInteger.valueOf(i);

                url = getIpfsFormattedUrl(blockChainService.getTokenURI(tokenId));
//                url = blockChainService.getTokenURI(tokenId);

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

    protected void loadAndStoreTokenData(String body, String tokenId, String collectionId, String url) {
        try {
            AbstractTokenDto tokenData = parseBody(body);
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
}
