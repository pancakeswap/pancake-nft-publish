package com.pancakeswap.nft.publish.model.dto.collection;

import com.pancakeswap.nft.publish.model.entity.CollectionType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CollectionDataDto extends CollectionImageDto {

    @NotNull
    private String name;
    @NotNull
    private String description;
    @NotNull
    private String symbol;
    @NotNull
    private String owner;

    @NotNull
    private Boolean onlyGif;
    @NotNull
    private Boolean isModifiedTokenName;

    @NotNull
    private CollectionType type;

    @NotNull
    private Boolean isCron;
    private Integer startIndex;
    private Integer totalSupply;


}
