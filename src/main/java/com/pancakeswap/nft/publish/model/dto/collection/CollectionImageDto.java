package com.pancakeswap.nft.publish.model.dto.collection;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class CollectionImageDto extends AbstractCollectionDto {
    private String avatarUrl;
    private String bannerUrl;

}
