package com.pancakeswap.nft.publish.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Document(collection = "attributes")
@Data
@NoArgsConstructor
public class Attribute {

    @Id
    private String id;

    @Field("parent_collection")
    @NotNull(message = "attribute parent_collection cant not be null")
    private ObjectId parentCollection;

    @Field("trait_type")
    @NotNull(message = "attribute traitType cant not be null")
    private String traitType;

    @NotNull(message = "attribute value cant not be null")
    private String value;

    @Field("display_type")
    private String displayType;

    @Field("created_at")
    private Date createdAt;
    @Field("updated_at")
    private Date updatedAt;

}
