package com.pancakeswap.nft.publish.model.dto.collection;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public abstract class AbstractCollectionDto {
    @NotNull
    private String address;
}
