package com.pancakeswap.nft.publish.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Document(collection = "collections_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionInfo {

    @Id
    private String id;
    @NotNull(message = "collection id cant not be null")
    private ObjectId collectionId;
    @NotNull
    private Boolean onlyGif;
    @NotNull
    private Boolean isModifiedTokenName;
    @NotNull
    private CollectionType type;
    @NotNull
    private Boolean isCron;
    private String failedIds;

    @Field("created_at")
    @CreatedDate
    private Date createdAt;
    @Field("updated_at")
    @LastModifiedDate
    private Date updatedAt;

}
