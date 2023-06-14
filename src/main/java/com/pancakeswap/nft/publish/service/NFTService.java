package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.exception.ListingException;
import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.service.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pancakeswap.nft.publish.util.GsonUtil.parseBody;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Slf4j
@Service
public class NFTService extends AbstractNFTService {

    public NFTService(
            BlockChainService blockChainService,
            TokenDataService tokenDataService,
            ImageService imageService,
            DBService dbService,
            CacheService cacheService) {
        super(imageService, dbService, tokenDataService, blockChainService, cacheService);
    }

    @Async
    public void listNFTs(CollectionDataDto dataDto) {
        String address = dataDto.getAddress();
        try {
            list(dataDto).thenRun(() -> cacheService.remove(address));
        } catch (ExecutionException | InterruptedException e) {
            cacheService.remove(address);
            throw new ListingException(e.getMessage(), e);
        }
    }

    @Async
    protected CompletableFuture<Boolean> list(CollectionDataDto dataDto) throws ExecutionException, InterruptedException {
        FutureConfig config = FutureConfig.init();
        storeAvatarAndBanner(config, dataDto.getAddress(), dataDto.getAvatarUrl(), dataDto.getBannerUrl());
        switch (dataDto.getType()) {
            case ENUMERABLE -> listNFT(config, dataDto, 0);
            case NO_ENUMERABLE ->
                    listNoEnumerableNFT(config, dataDto, dataDto.getStartIndex() != null ? dataDto.getStartIndex() : 0);
            case NO_ENUMERABLE_INFINITE ->
                    listNoEnumerableInfiniteNFT(config, dataDto, dataDto.getStartIndex() != null ? dataDto.getStartIndex() : 0);
        }
        return CompletableFuture.completedFuture(true);
    }

    public void listNoEnumerableInfiniteNFT(FutureConfig config, CollectionDataDto dataDto, int startIndex) {
        log.info("Fetching tokens for collection: {} started", dataDto.getAddress());

        Collection collection = dbService.storeCollectionIfNotExist(dataDto, startIndex);

        boolean allStored = false;
        int i = startIndex;
        String url = null;
        while (!allStored) {
            try {
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(dataDto.getAddress(), BigInteger.valueOf(i)));
            } catch (Exception e) {
                allStored = true;
                collection.setTotalSupply(i);
                collection = dbService.storeCollection(collection);
            }
            try {
                ListCollectionTokenParams params = new ListCollectionTokenParams(collection.getId(), dataDto.getAddress());
                params.setIsModifiedTokenName(dataDto.getIsModifiedTokenName());
                params.setOnlyGif(dataDto.getOnlyGif());
                params.setTokenId(String.valueOf(i));
                params.setTokenUrl(url);

                loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
            } catch (Exception e) {
                log.error("Failed to store token id: {}, url: {}, collectionId: {}. Error message: {}", i, url, collection.getId(), e.getMessage());
            }
            i++;
        }
        postListActions(config, collection.getId());
    }

    public void listNFT(FutureConfig config, CollectionDataDto dataDto, int startIndex) throws ExecutionException, InterruptedException {
        log.info("Fetching tokens for collection: {} started", dataDto.getAddress());

        BigInteger totalSupply = blockChainService.getTotalSupply(dataDto.getAddress());
        String collectionId = dbService.storeCollectionIfNotExist(dataDto, totalSupply.intValue()).getId();

        for (int i = startIndex; i < totalSupply.intValue(); i++) {
            BigInteger tokenId = null;
            String url = null;
            try {
                tokenId = blockChainService.getTokenId(dataDto.getAddress(), i);
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(dataDto.getAddress(), tokenId));

                ListCollectionTokenParams params = new ListCollectionTokenParams(collectionId, dataDto.getAddress());
                params.setIsModifiedTokenName(dataDto.getIsModifiedTokenName());
                params.setOnlyGif(dataDto.getOnlyGif());
                params.setTokenId(tokenId.toString());
                params.setTokenUrl(url);

                loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
            } catch (Exception e) {
                if (tokenId != null) {
                    config.addFailedTokenId(tokenId.toString());
                }
                log.error("Failed to store token index: {}, id: {}, url: {}, collectionId: {}. Error message: {}", i, tokenId, url, collectionId, e.getMessage());
            }
        }

        postListActions(config, collectionId);
    }

    public boolean deleteCollection(String collectionId) {
        try {
            dbService.deleteCollection(collectionId);
            return true;
        } catch (Exception ex) {
            throw new ListingException(ex.getMessage());
        }
    }

    private void listNoEnumerableNFT(FutureConfig config, CollectionDataDto dataDto, int startIndex) {
        log.info("Fetching tokens for collection: {} started", dataDto.getAddress());

        String collectionId = dbService.storeCollectionIfNotExist(dataDto, dataDto.getTotalSupply()).getId();

        for (int i = startIndex; i < dataDto.getTotalSupply() + startIndex; i++) {
            String url = null;
            try {
                url = getIpfsFormattedUrl(blockChainService.getTokenURI(dataDto.getAddress(), BigInteger.valueOf(i)));

                ListCollectionTokenParams params = new ListCollectionTokenParams(collectionId, dataDto.getAddress());
                params.setIsModifiedTokenName(dataDto.getIsModifiedTokenName());
                params.setOnlyGif(dataDto.getOnlyGif());
                params.setTokenId(String.valueOf(i));
                params.setTokenUrl(url);

                loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
            } catch (Exception e) {
                config.addFailedTokenId(String.valueOf(i));
                log.error("Failed to store token id: {}, url: {}, collectionId: {}. Error message: {}", i, url, collectionId, e.getMessage());
            }
        }

        postListActions(config, collectionId);
    }

    private void storeAvatarAndBanner(FutureConfig config, String address, String avatarUrl, String bannerUrl) {
        config.addFuture(() -> {
            if (!avatarUrl.isEmpty()) {
                imageService.uploadAvatarImage(address, avatarUrl);
            } else {
                log.info("Avatar url is empty");
            }
            if (!bannerUrl.isEmpty()) {
                imageService.uploadBannerImage(address, bannerUrl);
            } else {
                log.info("Banner url is empty");
            }
        });
    }

    //If token 'imagePng' exist we assume that 'image' contain gif
    private void storeTokenImage(FutureConfig config, AbstractTokenDto tokenData, String collectionAddress, boolean onlyGif) {
        config.addFuture(() -> {
            if (onlyGif) {
                imageService.s3UploadTokenImagesAsync(collectionAddress, tokenData.getImage(), tokenData, config.getTokenIdsFailed(), TokenMetadata.GIF);
                tokenData.setIsGif(true);
            } else if (Strings.isNotBlank(tokenData.getImagePng())) {
                imageService.s3UploadTokenImagesAsync(collectionAddress, tokenData.getImagePng(), tokenData, config.getTokenIdsFailed(), TokenMetadata.PNG);
                imageService.s3UploadTokenImagesAsync(collectionAddress, tokenData.getImage(), tokenData, config.getTokenIdsFailed(), TokenMetadata.GIF);
                tokenData.setIsGif(true);
            } else if (Strings.isNotBlank(tokenData.getGif())) {
                imageService.s3UploadTokenImagesAsync(collectionAddress, tokenData.getImage(), tokenData, config.getTokenIdsFailed(), TokenMetadata.PNG);
                imageService.s3UploadTokenImagesAsync(collectionAddress, tokenData.getGif(), tokenData, config.getTokenIdsFailed(), TokenMetadata.GIF);
                tokenData.setIsGif(true);
            } else {
                imageService.s3UploadTokenImagesAsync(collectionAddress, tokenData.getImage(), tokenData, config.getTokenIdsFailed(), TokenMetadata.PNG);
            }
        });
    }

    protected void loadAndStoreTokenData(FutureConfig config, String body, ListCollectionTokenParams params) {
        try {
            AbstractTokenDto tokenData = parseBody(body);
            tokenData.setTokenId(params.getTokenId());
            if (params.getIsModifiedTokenName()) {
                tokenData.setName(String.format("%s %s", tokenData.getName(), params.getTokenId()));
            }
            storeTokenImage(config, tokenData, params.getCollectionAddress(), params.getOnlyGif());
            storeTokenData(config, params.getCollectionId(), tokenData);
        } catch (Exception ex) {
            config.addFailedTokenId(params.getTokenId());
            log.error("Can parse and store token data from: {}. Token id: {}. Error message: {}", params.getTokenUrl(), params.getTokenId(), ex.getMessage());
        }
    }

    private void storeTokenData(FutureConfig config, String collectionId, AbstractTokenDto tokenData) {
        config.addFuture(() -> {
            try {
                dbService.storeToken(collectionId, tokenData);
            } catch (Exception e) {
                config.addFailedTokenId(tokenData.getTokenId());
                log.error("Can not store token data. Token id: {}, collectionId: {}, Error message: {}", tokenData.getTokenId(), collectionId, e.getMessage());
            }
        });
    }
}
