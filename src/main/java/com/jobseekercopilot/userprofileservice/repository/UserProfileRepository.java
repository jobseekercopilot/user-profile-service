package com.jobseekercopilot.userprofileservice.repository;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(String userId);
    void deleteByUserId(String userId);

    @Query("select profile.userId from UserProfile profile order by profile.id")
    List<String> findAllUserIds();
}
