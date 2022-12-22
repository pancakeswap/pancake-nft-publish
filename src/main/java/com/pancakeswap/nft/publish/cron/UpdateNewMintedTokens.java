package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.model.entity.CollectionInfo;
import com.pancakeswap.nft.publish.repository.CollectionInfoRepository;
import com.pancakeswap.nft.publish.repository.CollectionRepository;
import com.pancakeswap.nft.publish.service.BlockChainService;
import com.pancakeswap.nft.publish.service.NFTService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class UpdateNewMintedTokens {

    private final NFTService nftService;
    private final CollectionRepository collectionRepository;
    private final CollectionInfoRepository collectionInfoRepository;
    private final BlockChainService blockChainService;

    public UpdateNewMintedTokens(NFTService nftService, CollectionRepository collectionRepository, CollectionInfoRepository collectionInfoRepository, BlockChainService blockChainService) {
        this.nftService = nftService;
        this.collectionRepository = collectionRepository;
        this.collectionInfoRepository = collectionInfoRepository;
        this.blockChainService = blockChainService;
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60, initialDelay = 0)
    public void updateCollections() throws ExecutionException, InterruptedException {
        log.info("updateCollections started");
        for (Collection collection :  collectionRepository.findAll()) {
            CollectionInfo info = collectionInfoRepository.findByCollectionId(new ObjectId(collection.getId()));
            BigInteger totalSupply = blockChainService.getTotalSupply(collection.getAddress());
            if (info != null && totalSupply.intValue() > collection.getTotalSupply()) {
                System.out.println("Found new minted. Collection: " + collection.getAddress());
                FutureConfig config = FutureConfig.init();
                nftService.listNFT(config, from(collection, info), collection.getTotalSupply() - 1);
                collection.setTotalSupply(totalSupply.intValue());
                collectionRepository.save(collection);
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
