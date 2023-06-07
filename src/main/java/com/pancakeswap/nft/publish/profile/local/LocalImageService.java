package com.pancakeswap.nft.publish.profile.local;

import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import com.pancakeswap.nft.publish.service.ImageService;
import com.pancakeswap.nft.publish.service.TokenMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@Profile("local")
public class LocalImageService implements ImageService {

    @Override
    public void uploadAvatarImage(String collectionAddress, String imageUrl) {
        log.info("Called uploadAvatarImage");
    }

    @Override
    public void uploadBannerImage(String collectionAddress, String imageUrl) {
        log.info("Called uploadBannerImage");
    }

    @Override
    public void s3UploadTokenImagesAsync(String collectionAddress, String imageUrl, AbstractTokenDto tokenData, Set<String> tokenIdsFailed, TokenMetadata metadata) {
        log.info("Called s3UploadTokenImagesAsync");
    }

}
