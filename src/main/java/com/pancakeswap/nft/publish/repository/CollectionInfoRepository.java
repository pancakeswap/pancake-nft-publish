package com.pancakeswap.nft.publish.repository;

import com.pancakeswap.nft.publish.model.entity.CollectionInfo;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CollectionInfoRepository extends MongoRepository<CollectionInfo, String> {
    CollectionInfo findByCollectionId(ObjectId collectionId);

    void deleteByCollectionId(ObjectId collectionId);
}
