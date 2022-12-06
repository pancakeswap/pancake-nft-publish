package com.pancakeswap.nft.publish.controller;

import com.pancakeswap.nft.publish.exception.ListingException;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.service.DBService;
import com.pancakeswap.nft.publish.service.NFTService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

@RestController
public class CollectionController {

    private final NFTService nftService;
    private final DBService dbService;

    public CollectionController(NFTService nftService, DBService dbService) {
        this.nftService = nftService;
        this.dbService = dbService;
    }

    @PostMapping(path = "/collections")
    public ResponseEntity<String> listCollection(@Valid @RequestBody CollectionDataDto dataDto) {
        try {
            String result = nftService.listNFT(dataDto);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            throw new ListingException("Failed to list collection");
        }
    }

    @DeleteMapping(path = "/collections/{id}")
    public ResponseEntity<String> listCollection(@PathVariable("id") String id) {
        try {
            dbService.deleteCollection(id);
            return ResponseEntity.ok("Done");
        } catch (Exception ex) {
            throw new ListingException("Failed to list collection");
        }
    }
}
