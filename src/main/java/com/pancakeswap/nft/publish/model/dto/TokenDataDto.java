package com.pancakeswap.nft.publish.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TokenDataDto {
    private String tokenId;
    private String name;
    private String description;
    private String image;
    private String imagePng;

    private List<AttributeDto> attributes;
}
