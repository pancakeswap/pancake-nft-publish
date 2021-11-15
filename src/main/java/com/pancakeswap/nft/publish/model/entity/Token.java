package com.pancakeswap.nft.publish.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Document(collection = "tokens")
@Data
@NoArgsConstructor
public class Token {

    @Id
    private String id;

    @Field("parent_collection")
    @NotNull(message = "token parent_collection cant not be null")
    private ObjectId parentCollection;

    @Field("token_id")
    @NotNull(message = "token token_id cant not be null")
    private String tokenId;

    @NotNull(message = "token metadata cant not be null")
    private ObjectId metadata;

    @NotNull(message = "token attributes cant not be null")
    private List<ObjectId> attributes;

    private Boolean burned;

    @Field("created_at")
    private Date createdAt;
    @Field("updated_at")
    private Date updatedAt;

}
