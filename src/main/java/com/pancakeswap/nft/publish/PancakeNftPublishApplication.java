package com.pancakeswap.nft.publish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableMongoRepositories
public class PancakeNftPublishApplication {

    public static void main(String[] args) {
        SpringApplication.run(PancakeNftPublishApplication.class, args);
    }

}
