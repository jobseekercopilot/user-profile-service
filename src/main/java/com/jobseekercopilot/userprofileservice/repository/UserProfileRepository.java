package com.jobseekercopilot.userprofileservice.repository;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(String userId);
    void deleteByUserId(String userId);
}
