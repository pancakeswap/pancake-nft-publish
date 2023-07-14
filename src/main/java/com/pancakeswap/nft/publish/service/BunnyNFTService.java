package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.dto.collection.ListCollectionTokenParams;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.service.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pancakeswap.nft.publish.util.FutureUtils.waitFutureRequestFinished;
import static com.pancakeswap.nft.publish.util.GsonUtil.parseBody;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Slf4j
@Service
public class BunnyNFTService extends AbstractNFTService {

    @Value("${nft.bunny.collection.last.index}")
    private Integer lastIndex;

    public BunnyNFTService(
            BlockChainService blockChainService,
            TokenDataService tokenDataService,
            ImageService imageService,
            DBService dbService, CacheService cacheService) {
        super(imageService, dbService, tokenDataService, blockChainService, cacheService);
    }

    @Async
    public void listNFTs(String collectionAddress) {
        try {
            listOnlyOnePerBunnyId(collectionAddress).thenRun(() -> cacheService.remove(collectionAddress));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    protected CompletableFuture<Boolean> listOnlyOnePerBunnyId(String collectionAddress) throws ExecutionException, InterruptedException {
        log.info("Fetching tokens for collection: {} started", collectionAddress);
        int lastAddedBunnyId = 27;

        FutureConfig config = FutureConfig.init();

        BigInteger totalSupply = blockChainService.getTotalSupply(collectionAddress);
        Collection collection = dbService.getCollection(collectionAddress);

        for (int i = lastIndex; i < totalSupply.intValue(); i++) {
            BigInteger tokenId = null;
            BigInteger bunnyID;
            try {
                tokenId = blockChainService.getTokenId(collectionAddress, i);
                bunnyID = blockChainService.getBunnyId(collectionAddress, tokenId);
                ListCollectionTokenParams params = new ListCollectionTokenParams(collection.getId(), collection.getAddress());

                params.setTokenId(tokenId.toString());
                if (bunnyID.intValue() > lastAddedBunnyId) {
                    loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
                    lastAddedBunnyId = bunnyID.intValue();
                }
            } catch (Exception e) {
                if (tokenId != null) {
                    config.addFailedTokenId(tokenId.toString());
                }
                log.error("Failed to store token index: {}, id: {}, collectionId: {}", i, tokenId, collection.getId(), e);
            }
        }

        waitFutureRequestFinished(config);
        log.info("Fetching tokens for collection: {} finished. LastIndex - {}, lastAddedBunnyId - {}",
                collectionAddress, totalSupply.intValue() - 1, lastAddedBunnyId);
        return CompletableFuture.completedFuture(true);
    }

    @Deprecated
    public void listNFT(CollectionDataDto dataDto) throws ExecutionException, InterruptedException {
        FutureConfig config = FutureConfig.init();

        BigInteger totalSupply = blockChainService.getTotalSupply(dataDto.getAddress());
        Collection collection = dbService.getCollection(dataDto.getAddress());

        for (int i = lastIndex; i < totalSupply.intValue(); i++) {
            BigInteger tokenId = null;
            try {
                tokenId = blockChainService.getTokenId(dataDto.getAddress(), i);

                ListCollectionTokenParams params = new ListCollectionTokenParams(collection.getId(), collection.getAddress());
                params.setTokenId(tokenId.toString());

                loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
            } catch (Exception e) {
                if (tokenId != null) {
                    config.addFailedTokenId(tokenId.toString());
                }
                log.error("Failed to store token index: {}, id: {}, collectionId: {}", i, tokenId, collection.getId(), e);
            }
        }

        waitFutureRequestFinished(config);
    }

    @Deprecated
    public void relistNftByIndex(String collectionAddress, List<Integer> tokenIds) {
        FutureConfig config = FutureConfig.init();

        Collection collection = dbService.getCollection(collectionAddress);

        tokenIds.forEach(i -> {
            String url = null;
            BigInteger tokenId = null;
            try {
                ListCollectionTokenParams params = new ListCollectionTokenParams(collection.getId(), collection.getAddress());

                tokenId = blockChainService.getTokenId(collectionAddress, i);
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(collectionAddress, tokenId));

                params.setTokenId(tokenId.toString());
                params.setTokenUrl(url);

                loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
            } catch (Exception e) {
                log.error("Failed to store token id: {}, url: {}, collectionId: {}", tokenId, url, collection.getId(), e);
            }
        });
        waitFutureRequestFinished(config);
    }

    @Override
    protected void loadAndStoreTokenData(FutureConfig config, String body, ListCollectionTokenParams params) {
        try {
            AbstractTokenDto tokenData = parseBody(body);
            tokenData.setTokenId(params.getTokenId());
            storeBunnyTokenData(config, params.getCollectionId(), tokenData);
        } catch (Exception ex) {
            config.addFailedTokenId(params.getTokenId());
            log.error("Can't parse and store token data from: {}. Token id: {}. Error message: {}", params.getTokenUrl(), params.getTokenId(), ex.getMessage());
        }
    }

    private void storeBunnyTokenData(FutureConfig config, String collectionId, AbstractTokenDto tokenData) {
        config.addFuture(
                () -> {
                    try {
                        dbService.storeBunnyToken(collectionId, tokenData);
                    } catch (Exception e) {
                        config.addFailedTokenId(tokenData.getTokenId());
                        log.error("Can not store token data. Token id: {}, collectionId: {}, Error message: {}", tokenData.getTokenId(), collectionId, e.getMessage());
                    }
                });
    }
}
