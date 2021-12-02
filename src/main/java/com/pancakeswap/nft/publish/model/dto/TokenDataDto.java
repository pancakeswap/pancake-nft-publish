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

    private Boolean mp4;
    private Boolean webm;
    private Boolean gif;

    private List<AttributeDto> attributes;
}
