package com.example.demo.repo;

import java.time.LocalDate;
import java.util.List;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.User;

@Repository public interface UserRepository extends JpaRepository<User, String> {

    @Query("select u from User u where u.isLocked = true and u.lockedUntil < ?1")
    List<User> findByLockedTrueAndLockedUntilBefore(LocalDate today);

    @Query("select u from User u where lower(concat(u.fName, ' ', u.lName)) like lower(concat('%', :keyword, '%') ) " +
            "or lower(u.fName) like lower(concat('%', :keyword, '%') ) " +
            "or lower(u.lName) like lower(concat('%', :keyword, '%') )")
    List<User> findUserByNameOrPart(@Param("keyword") String keyword);
}