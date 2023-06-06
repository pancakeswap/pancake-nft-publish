package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.model.entity.CollectionInfo;
import com.pancakeswap.nft.publish.repository.CollectionInfoRepository;
import com.pancakeswap.nft.publish.repository.CollectionRepository;
import com.pancakeswap.nft.publish.service.BlockChainService;
import com.pancakeswap.nft.publish.service.NFTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateNewMintedTokens {

    private final NFTService nftService;
    private final CollectionRepository collectionRepository;
    private final CollectionInfoRepository collectionInfoRepository;
    private final BlockChainService blockChainService;

    @Scheduled(fixedDelay = 60, initialDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void updateCollections() throws ExecutionException, InterruptedException {
        log.info("updateCollections started");
        for (Collection collection : collectionRepository.findAll()) {
            log.info(collection.getAddress());
            CollectionInfo info = collectionInfoRepository.findByCollectionId(new ObjectId(collection.getId()));
            if (info != null && info.getIsCron()) {
                FutureConfig config = FutureConfig.init();
                switch (info.getType()) {
                    case ENUMERABLE -> {
                        BigInteger totalSupply = blockChainService.getTotalSupply(collection.getAddress());
                        if (totalSupply.intValue() > collection.getTotalSupply()) {
                            nftService.listNFT(config, from(collection, info), collection.getTotalSupply() - 1);
                            collection.setTotalSupply(totalSupply.intValue());
                            collectionRepository.save(collection);
                        }
                    }
                    case NO_ENUMERABLE_INFINITE ->
                            nftService.listNoEnumerableInfiniteNFT(config, from(collection, info), collection.getTotalSupply());
                }
            }
        }
        log.info("updateCollections ended");
    }

    private CollectionDataDto from(Collection collection, CollectionInfo collectionInfo) {
        CollectionDataDto dataDto = new CollectionDataDto();
        dataDto.setAddress(collection.getAddress());
        dataDto.setName(collection.getName());
        dataDto.setDescription(collection.getDescription());
        dataDto.setSymbol(collection.getSymbol());
        dataDto.setOwner(collection.getOwner());

        dataDto.setIsModifiedTokenName(collectionInfo.getIsModifiedTokenName());
        dataDto.setOnlyGif(collectionInfo.getOnlyGif());

        return dataDto;
    }
}
