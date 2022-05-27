package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import com.pancakeswap.nft.publish.model.dto.AttributeDto;
import com.pancakeswap.nft.publish.model.dto.TokenDataFormattedDto;
import com.pancakeswap.nft.publish.model.dto.TokenDataNoFormattedDto;
import com.pancakeswap.nft.publish.model.entity.*;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.repository.*;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DBService {

    @Value("${nft.collection.address}")
    private String contract;
    @Value("${nft.collection.name}")
    private String name;
    @Value("${nft.collection.description}")
    private String description;
    @Value("${nft.collection.symbol}")
    private String symbol;
    @Value("${nft.collection.owner}")
    private String owner;

    private final CollectionRepository collectionRepository;
    private final AttributeRepository attributeRepository;
    private final TokenRepository tokenRepository;
    private final MetadataRepository metadataRepository;

    private final Map<String, String> attributesMapCache = Collections.synchronizedMap(new HashMap<>());

    public DBService(CollectionRepository collectionRepository, AttributeRepository attributeRepository, TokenRepository tokenRepository, MetadataRepository metadataRepository) {
        this.collectionRepository = collectionRepository;
        this.attributeRepository = attributeRepository;
        this.tokenRepository = tokenRepository;
        this.metadataRepository = metadataRepository;
    }

    public Collection getCollection() {
        return collectionRepository.findByAddress(contract.toLowerCase(Locale.ROOT));
    }

    public Collection storeCollection(Integer totalSupply) {
        Collection collection = getCollection();
        if (collection != null) {
            return collection;
        }

        collection = new Collection();
        collection.setAddress(contract.toLowerCase(Locale.ROOT));
        collection.setOwner(owner.toLowerCase(Locale.ROOT));
        collection.setName(name);
        collection.setDescription(description);
        collection.setSymbol(symbol);
        collection.setTotalSupply(totalSupply);
        collection.setVerified(false);
        collection.setVisible(false);
        collection.setCreatedAt(new Date());
        collection.setUpdatedAt(new Date());

        return collectionRepository.save(collection);
    }

    @Transactional
    public <T extends AbstractTokenDto> void storeToken(String collectionId, T tokenDataDto) {
        List<ObjectId> attributes;
        if (tokenDataDto instanceof TokenDataFormattedDto) {
            TokenDataFormattedDto dto = (TokenDataFormattedDto) tokenDataDto;
            attributes = storeAttributes(collectionId, dto.getAttributes());
        } else {
            TokenDataNoFormattedDto dto = (TokenDataNoFormattedDto) tokenDataDto;
            attributes = storeAttributes(collectionId, dto.getAttributes().entrySet().stream().map(e -> new AttributeDto(e.getKey(), e.getValue())).collect(Collectors.toList()));
        }

        Token token = findToken(collectionId, tokenDataDto.getTokenId());
        if (token.getMetadata() == null) {
            Metadata metadata = storeMetadata(tokenDataDto, collectionId);
            token.setMetadata(new ObjectId(metadata.getId()));
        } else {
            metadataRepository.findById(token.getMetadata().toString()).ifPresent((meta) -> {
                meta.setName(tokenDataDto.getName());
                meta.setDescription(tokenDataDto.getDescription());
                meta.setGif(Boolean.TRUE.equals(tokenDataDto.getIsGif()));
                meta.setMp4(Boolean.TRUE.equals(tokenDataDto.getIsMp4()));
                meta.setWebm(Boolean.TRUE.equals(tokenDataDto.getIsWebm()));
                meta.setUpdatedAt(new Date());
                metadataRepository.save(meta);
            });
        }

        token.setTokenId(tokenDataDto.getTokenId());
        token.setBurned(false);
        token.setAttributes(attributes);

        tokenRepository.save(token);
    }

    @Transactional
    public <T extends AbstractTokenDto> void storeBunnyToken(String collectionId, T tokenDataDto) {
        List<ObjectId> attributes;
        if (tokenDataDto instanceof TokenDataFormattedDto) {
            TokenDataFormattedDto dto = (TokenDataFormattedDto) tokenDataDto;
            attributes = storeAttributes(collectionId, dto.getAttributes());
        } else {
            TokenDataNoFormattedDto dto = (TokenDataNoFormattedDto) tokenDataDto;
            attributes = storeAttributes(collectionId, dto.getAttributes().entrySet().stream().map(e -> new AttributeDto(e.getKey(), e.getValue())).collect(Collectors.toList()));
        }

        Token token = findToken(collectionId, tokenDataDto.getTokenId());
        if (token.getMetadata() == null) {
            Metadata metadata = metadataRepository.findByParentCollectionAndName(new ObjectId(collectionId), tokenDataDto.getName())
                    .orElse(storeMetadata(tokenDataDto, collectionId));
            token.setMetadata(new ObjectId(metadata.getId()));
        }

        token.setTokenId(tokenDataDto.getTokenId());
        token.setBurned(Boolean.TRUE.equals(token.getBurned()));
        token.setAttributes(attributes);

        tokenRepository.save(token);
    }

    private Token findToken(String collectionId, String tokenId) {
        Token token = tokenRepository.findByParentCollectionAndTokenId(new ObjectId(collectionId), tokenId);
        if (token == null) {
            token = new Token();
            token.setParentCollection(new ObjectId(collectionId));
            token.setCreatedAt(new Date());
            token.setUpdatedAt(new Date());
        }
        return token;
    }

    @Transactional
    public void deleteCollection(String id) {
        tokenRepository.deleteAllByParentCollection(new ObjectId(id));
        metadataRepository.deleteAllByParentCollection(new ObjectId(id));
        attributeRepository.deleteAllByParentCollection(new ObjectId(id));

        collectionRepository.deleteById(id);
    }

    public void deleteAttributes(String id) {
        attributeRepository.deleteAllByParentCollection(new ObjectId(id));
    }

    private List<ObjectId> storeAttributes(String collectionId, List<AttributeDto> attributes) {
        synchronized (attributesMapCache) {

            List<ObjectId> existed = new ArrayList<>();

            List<AttributeDto> toStore = attributes.stream().filter(item -> {
                String id = attributesMapCache.get(String.format("%s-%s-%s", collectionId, item.getTraitType(), item.getValue()));
                if (id != null) {
                    existed.add(new ObjectId(id));
                    return false;
                } else {
                    Optional<Attribute> attr = attributeRepository.findByParentCollectionAndTraitTypeAndValue(new ObjectId(collectionId), item.getTraitType(), item.getValue());
                    attr.ifPresent((a) -> {
                        attributesMapCache.put(String.format("%s-%s-%s", collectionId, a.getTraitType(), a.getValue()), a.getId());
                        existed.add(new ObjectId(a.getId()));
                    });

                    return attr.isEmpty();
                }
            }).collect(Collectors.toList());

            List<Attribute> entities = toStore.stream().map(attributeDto -> {
                Attribute attribute = new Attribute();
                attribute.setParentCollection(new ObjectId(collectionId));
                attribute.setTraitType(attributeDto.getTraitType());
                attribute.setValue(attributeDto.getValue());

                attribute.setCreatedAt(new Date());
                attribute.setUpdatedAt(new Date());

                return attribute;
            }).collect(Collectors.toList());

            if (entities.size() > 0) {
                List<Attribute> stored = attributeRepository.saveAll(entities);
                stored.forEach(a -> attributesMapCache.put(String.format("%s-%s-%s", collectionId, a.getTraitType(), a.getValue()), a.getId()));
                existed.addAll(stored.stream().map(a -> new ObjectId(a.getId())).collect(Collectors.toList()));
            }

            return existed;
        }
    }

    private Metadata storeMetadata(AbstractTokenDto dto, String parentId) {
        Metadata metadata = new Metadata();
        metadata.setParentCollection(new ObjectId(parentId));
        metadata.setName(dto.getName());
        metadata.setDescription(dto.getDescription());
        metadata.setGif(Boolean.TRUE.equals(dto.getIsGif()));
        metadata.setMp4(Boolean.TRUE.equals(dto.getIsMp4()));
        metadata.setWebm(Boolean.TRUE.equals(dto.getIsWebm()));
        metadata.setCreatedAt(new Date());
        metadata.setUpdatedAt(new Date());

        return metadataRepository.save(metadata);
    }
}
