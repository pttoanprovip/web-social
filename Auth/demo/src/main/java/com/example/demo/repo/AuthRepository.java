package com.example.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Auth;

@Repository
public interface AuthRepository extends JpaRepository<Auth, String> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Auth findByEmail(String email);

    Auth findByPhone(String phone);

}
