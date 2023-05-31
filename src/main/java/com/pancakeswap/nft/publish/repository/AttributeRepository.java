package com.pancakeswap.nft.publish.repository;

import com.pancakeswap.nft.publish.model.entity.Attribute;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.Optional;

public interface AttributeRepository extends MongoRepository<Attribute, String> {
    void deleteAllByParentCollection(ObjectId parentId);

    Optional<Attribute> findByParentCollectionAndTraitTypeAndValue(ObjectId parentId, String traitType, String value);

    Optional<Attribute> findFirstByIdInAndTraitType(Collection<String> id, String traitType);
}
