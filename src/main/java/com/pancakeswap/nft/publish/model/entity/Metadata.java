package com.pancakeswap.nft.publish.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Document(collection = "metadata")
@Data
@NoArgsConstructor
public class Metadata {

    @Id
    private String id;

    @Field("parent_collection")
    @NotNull(message = "metadata parent_collection cant not be null")
    private ObjectId parentCollection;

    @NotNull(message = "metadata name cant not be null")
    private String name;

    private String description;

    private Boolean mp4;
    private Boolean webm;
    private Boolean gif;

    @Field("created_at")
    private Date createdAt;
    @Field("updated_at")
    private Date updatedAt;

}
