package com.pancakeswap.nft.publish.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class ProfileManager {

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @PostConstruct
    private void logActiveProfiles() {
        for (String profileName : activeProfiles.split(",")) {
            log.info("Currently active profile: {}", profileName);
            log.info("Properties will be loaded from application-{}.properties", profileName);
        }
    }
}
