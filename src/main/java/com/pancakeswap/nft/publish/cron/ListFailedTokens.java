package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.model.entity.CollectionInfo;
import com.pancakeswap.nft.publish.repository.CollectionInfoRepository;
import com.pancakeswap.nft.publish.repository.CollectionRepository;
import com.pancakeswap.nft.publish.service.NFTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListFailedTokens {

    private final NFTService nftService;
    private final CollectionRepository collectionRepository;
    private final CollectionInfoRepository collectionInfoRepository;

    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES, initialDelay = 1)
    public void updateTokens() {
        log.info("updateTokens started");
        for (Collection collection : collectionRepository.findAll()) {
            CollectionInfo info = collectionInfoRepository.findByCollectionId(new ObjectId(collection.getId()));
            if (info != null && info.getFailedIds() != null) {
                String[] ids = info.getFailedIds().split(",");
                nftService.relistNft(collection.getAddress(), ids);
            }
        }
        log.info("updateTokens ended");
    }
}
