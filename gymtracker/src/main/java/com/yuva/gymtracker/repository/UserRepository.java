package com.yuva.gymtracker.repository;
import com.yuva.gymtracker.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
