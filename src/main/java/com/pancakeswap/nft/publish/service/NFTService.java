package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionImageDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.pancakeswap.nft.publish.util.FutureUtils.waitFutureRequestFinished;
import static com.pancakeswap.nft.publish.util.GsonUtil.parseBody;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Service
@Slf4j
public class NFTService extends AbstractNFTService {

    public NFTService(BlockChainService blockChainService, TokenDataService tokenDataService, ImageService imageService, DBService dbService) {
        super(imageService, dbService, tokenDataService, blockChainService);
    }

    public String listNFT(CollectionDataDto dataDto) throws ExecutionException, InterruptedException {
        log.info("fetching tokens started");

        FutureConfig config = FutureConfig.init();

        storeAvatarAndBanner(config, dataDto);

        BigInteger totalSupply = blockChainService.getTotalSupply(dataDto.getAddress());
        String collectionId = dbService.storeCollection(dataDto, totalSupply.intValue()).getId();



        for (int i = 0; i < totalSupply.intValue(); i++) {
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
                config.removeIfDone();
            } catch (Exception e) {
                if (tokenId != null) {
                    config.addFailedTokenId(tokenId.toString());
                }
                log.error("failed to store token index: {}, id: {}, url: {}, collectionId: {}", i, tokenId, url, collectionId, e);
            }
        }

        waitFutureRequestFinished(config);
        log.info("fetching tokens finished");

        if (!config.getTokenIdsFailed().isEmpty()) {
            return "List of failed tokens IDs:" + config.getTokenIdsFailed().stream().sorted().collect(Collectors.joining(","));
        }

        return "Listed";
    }

    private void storeAvatarAndBanner(FutureConfig config, CollectionImageDto dto) {
        config.addFuture(CompletableFuture.runAsync(() -> {
            if (!dto.getAvatarUrl().isEmpty()) {
                imageService.uploadAvatarImage(dto.getAddress(), dto.getAvatarUrl());
            } else {
                log.info("avatar url is empty");
            }
            if (!dto.getBannerUrl().isEmpty()) {
                imageService.uploadBannerImage(dto.getAddress(), dto.getBannerUrl());
            } else {
                log.info("banner url is empty");
            }
        }));
    }

    //If token 'imagePng' exist we assume that 'image' contain gif
    protected void storeTokenImage(FutureConfig config, AbstractTokenDto tokenData, String collectionAddress, boolean onlyGif) {
        config.addFuture(CompletableFuture.runAsync(() -> {
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
        }));
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
        config.addFuture(CompletableFuture.runAsync(
                () -> {
                    try {
                        dbService.storeToken(collectionId, tokenData);
                    } catch (Exception e) {
                        config.addFailedTokenId(tokenData.getTokenId());
                        log.error("Can not store token data. Token id: {}, collectionId: {}, Error message: {}", tokenData.getTokenId(), collectionId, e.getMessage());
                    }
                }));
    }
}
