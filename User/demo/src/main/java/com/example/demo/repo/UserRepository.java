package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    @Query("select u from User u where u.fName = ?1")
    List<User> findByFirstName(String fName);

    @Query("select u from User u where u.lName = ?1")
    List<User> findByLastName(String lName);
}