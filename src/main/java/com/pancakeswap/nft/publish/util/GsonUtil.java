package com.pancakeswap.nft.publish.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pancakeswap.nft.publish.model.dto.TokenDataDto;

public class GsonUtil {

    public static final Gson gsonUnderscores = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

    public static final Gson gsonIdentity = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .create();

    public static TokenDataDto parseBody(String body) {
        TokenDataDto underscore = gsonUnderscores.fromJson(body, TokenDataDto.class);
        TokenDataDto identity = gsonIdentity.fromJson(body, TokenDataDto.class);

        if (underscore.getImagePng() == null) {
            underscore.setImagePng(identity.getImagePng());
        }

        return underscore;
    }
}
