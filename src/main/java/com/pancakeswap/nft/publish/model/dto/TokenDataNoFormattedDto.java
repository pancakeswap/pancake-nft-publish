package com.pancakeswap.nft.publish.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class TokenDataNoFormattedDto extends AbstractTokenDto {

    private Map<String,String> attributes;

}
