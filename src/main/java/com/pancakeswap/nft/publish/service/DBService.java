package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.dto.*;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.entity.*;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.repository.*;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.transaction.annotation.Isolation.REPEATABLE_READ;

@Service
public class DBService {
    private final CollectionRepository collectionRepository;
    private final AttributeRepository attributeRepository;
    private final TokenRepository tokenRepository;
    private final MetadataRepository metadataRepository;
    private final CollectionInfoRepository collectionInfoRepository;

    private final Map<String, String> attributesMapCache = Collections.synchronizedMap(new HashMap<>());

    public DBService(CollectionRepository collectionRepository, AttributeRepository attributeRepository, TokenRepository tokenRepository, MetadataRepository metadataRepository, CollectionInfoRepository collectionInfoRepository) {
        this.collectionRepository = collectionRepository;
        this.attributeRepository = attributeRepository;
        this.tokenRepository = tokenRepository;
        this.metadataRepository = metadataRepository;
        this.collectionInfoRepository = collectionInfoRepository;
    }

    public Collection getCollection(String collectionAddress) {
        return collectionRepository.findByAddress(collectionAddress.toLowerCase(Locale.ROOT));
    }

    public Collection storeCollection(CollectionDataDto dataDto, Integer totalSupply) {
        Collection collection = getCollection(dataDto.getAddress());
        if (collection != null) {
            return collection;
        }

        collection = Collection.builder()
                .address(dataDto.getAddress().toLowerCase(Locale.ROOT))
                .owner(dataDto.getOwner().toLowerCase(Locale.ROOT))
                .name(dataDto.getName())
                .description(dataDto.getDescription())
                .symbol(dataDto.getSymbol())
                .totalSupply(totalSupply)
                .verified(false).visible(false)
                .createdAt(new Date()).updatedAt(new Date()).build();

        collection = collectionRepository.save(collection);

        CollectionInfo info = CollectionInfo.builder()
                .collectionId(new ObjectId(collection.getId()))
                .isModifiedTokenName(dataDto.getIsModifiedTokenName())
                .onlyGif(dataDto.getOnlyGif())
                .createdAt(new Date()).updatedAt(new Date()).build();
        collectionInfoRepository.save(info);

        return collection;
    }

    public void storeFailedIds(String collectionId, String ids) {
        CollectionInfo info = collectionInfoRepository.findByCollectionId(new ObjectId(collectionId));
        info.setFailedIds(ids);
        collectionInfoRepository.save(info);
    }

    @Transactional(isolation = REPEATABLE_READ)
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
        collectionInfoRepository.deleteByCollectionId(new ObjectId(id));
        collectionInfoRepository.deleteByCollectionId(new ObjectId(id));

        collectionRepository.deleteById(id);
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
