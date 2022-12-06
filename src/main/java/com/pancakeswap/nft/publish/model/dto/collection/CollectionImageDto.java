package com.pancakeswap.nft.publish.model.dto.collection;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class CollectionImageDto extends AbstractCollectionDto {
    @NotNull
    private String avatarUrl;
    @NotNull
    private String bannerUrl;

}
