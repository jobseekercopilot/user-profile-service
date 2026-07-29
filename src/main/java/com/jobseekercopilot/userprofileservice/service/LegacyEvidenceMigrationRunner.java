package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LegacyEvidenceMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyEvidenceMigrationRunner.class);

    private final UserProfileRepository profileRepository;
    private final LegacyEvidenceMigrator migrator;

    public LegacyEvidenceMigrationRunner(
            UserProfileRepository profileRepository,
            LegacyEvidenceMigrator migrator) {
        this.profileRepository = profileRepository;
        this.migrator = migrator;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        int profiles = 0;
        int entries = 0;
        for (String userId : profileRepository.findAllUserIds()) {
            entries += migrator.migrateForOwner(userId);
            profiles++;
        }
        log.info("Legacy evidence migration scan completed profilesCount={} migratedEntriesCount={}",
                profiles, entries);
    }
}
