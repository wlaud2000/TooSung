package com.project.toosung_back.domain.disclosure.repository;

import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisclosureRepository extends JpaRepository<Disclosure, Long> {

    boolean existsByDartId(String dartId);
}
