package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;

import java.util.Set;

public interface ImageService {

    void uploadAvatarImage(String collectionAddress, String imageUrl);

    void uploadBannerImage(String collectionAddress, String imageUrl);

    void s3UploadTokenImagesAsync(String collectionAddress, String imageUrl, AbstractTokenDto tokenData, Set<String> tokenIdsFailed, TokenMetadata metadata);
}
