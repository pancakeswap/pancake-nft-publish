package com.pancakeswap.nft.publish.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import com.pancakeswap.nft.publish.model.dto.TokenDataFormattedDto;
import com.pancakeswap.nft.publish.model.dto.TokenDataNoFormattedDto;

public class GsonUtil {

    public static final Gson gsonUnderscores = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

    public static final Gson gsonIdentity = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .create();

    public static AbstractTokenDto parseBody(String body) {
        AbstractTokenDto underscore;
        AbstractTokenDto identity;

        try {
            underscore = gsonUnderscores.fromJson(body, TokenDataFormattedDto.class);
            identity = gsonIdentity.fromJson(body, TokenDataFormattedDto.class);
        } catch (Exception e) {
            underscore = gsonUnderscores.fromJson(body, TokenDataNoFormattedDto.class);
            identity = gsonIdentity.fromJson(body, TokenDataNoFormattedDto.class);
        }

        if (underscore.getImagePng() == null) {
            underscore.setImagePng(identity.getImagePng());
        }

        return underscore;
    }
}
