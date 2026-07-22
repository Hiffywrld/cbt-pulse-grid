package com.cbtpulsegrid.backend.examination;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamCandidateRepository extends JpaRepository<ExamCandidate, UUID> {

	long countByExamId(UUID examId);

	Page<ExamCandidate> findByExamId(UUID examId, Pageable pageable);

	List<ExamCandidate> findAllByExamIdAndUserIdIn(UUID examId, Collection<UUID> userIds);

	Optional<ExamCandidate> findByExamIdAndUserId(UUID examId, UUID userId);
}
