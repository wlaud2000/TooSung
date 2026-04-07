package com.project.toosung_back.domain.disclosure.repository;

import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisclosureAnalysisRepository extends JpaRepository<DisclosureAnalysis, Long> {

    boolean existsByDisclosureId(Long disclosureId);

    Optional<DisclosureAnalysis> findByDisclosureId(Long disclosureId);
}
