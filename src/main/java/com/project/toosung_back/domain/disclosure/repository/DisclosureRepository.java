package com.project.toosung_back.domain.disclosure.repository;

import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DisclosureRepository extends JpaRepository<Disclosure, Long> {

    boolean existsByDartId(String dartId);

    @Query("SELECT d FROM Disclosure  d JOIN FETCH d.stock WHERE d.id = :id")
    Optional<Disclosure> findByIdWithStock(@Param("id") Long id);
}
