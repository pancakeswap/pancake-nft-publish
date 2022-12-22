package com.pancakeswap.nft.publish.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Document(collection = "collections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection {

    @Id
    private String id;
    @NotNull(message = "collection address cant not be null")
    private String address;

    @NotNull(message = "collection owner cant not be null")
    private String owner;

    @NotNull(message = "collection name cant not be null")
    private String name;

    @NotNull(message = "collection description cant not be null")
    private String description;

    @NotNull(message = "collection symbol cant not be null")
    private String symbol;

    @NotNull(message = "collection total_supply cant not be null")
    @Field("total_supply")
    private Integer totalSupply;

    private Boolean visible;
    private Boolean verified;

    @Field("created_at")
    private Date createdAt;
    @Field("updated_at")
    private Date updatedAt;

}
