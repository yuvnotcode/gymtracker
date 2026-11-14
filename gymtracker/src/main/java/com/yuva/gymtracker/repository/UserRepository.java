package com.yuva.gymtracker.repository;
import com.yuva.gymtracker.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "delete from user_custom_split_days where user_id = :userId", nativeQuery = true)
    void deleteCustomSplitDaysByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
