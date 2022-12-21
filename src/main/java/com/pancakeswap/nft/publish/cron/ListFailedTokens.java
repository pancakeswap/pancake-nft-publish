package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.model.entity.CollectionInfo;
import com.pancakeswap.nft.publish.repository.CollectionInfoRepository;
import com.pancakeswap.nft.publish.repository.CollectionRepository;
import com.pancakeswap.nft.publish.service.NFTService;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ListFailedTokens {

    private final NFTService nftService;
    private final CollectionRepository collectionRepository;
    private final CollectionInfoRepository collectionInfoRepository;

    public ListFailedTokens(NFTService nftService, CollectionRepository collectionRepository, CollectionInfoRepository collectionInfoRepository) {
        this.nftService = nftService;
        this.collectionRepository = collectionRepository;
        this.collectionInfoRepository = collectionInfoRepository;
    }

    @Scheduled(fixedDelay = 1000 * 60, initialDelay = 0)
    public void updateTokens() {
        System.out.println("!!! updateTokens cron");
        for (Collection collection: collectionRepository.findAll()) {
            CollectionInfo info = collectionInfoRepository.findByCollectionId(new ObjectId(collection.getId()));
            if (info != null && info.getFailedIds() != null) {
                String[] ids = info.getFailedIds().split(",");
                nftService.relistNft(collection.getAddress(), ids);
            }
        }
    }
}
