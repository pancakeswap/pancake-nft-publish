package com.pancakeswap.nft.publish.repository;

import com.pancakeswap.nft.publish.model.entity.Metadata;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MetadataRepository extends MongoRepository<Metadata, String> {
    void deleteAllByParentCollection(ObjectId parentId);
}
