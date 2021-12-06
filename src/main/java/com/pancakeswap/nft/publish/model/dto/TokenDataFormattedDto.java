package com.pancakeswap.nft.publish.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class TokenDataFormattedDto extends AbstractTokenDto {

    private List<AttributeDto> attributes;
}
