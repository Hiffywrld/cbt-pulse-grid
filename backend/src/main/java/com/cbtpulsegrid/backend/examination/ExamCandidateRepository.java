package com.cbtpulsegrid.backend.examination;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamCandidateRepository extends JpaRepository<ExamCandidate, UUID> {

	long countByExamId(UUID examId);

	boolean existsByExamIdAndUserId(UUID examId, UUID userId);

	Page<ExamCandidate> findByExamId(UUID examId, Pageable pageable);

	List<ExamCandidate> findAllByExamIdAndUserIdIn(UUID examId, Collection<UUID> userIds);

	List<ExamCandidate> findAllByUserId(UUID userId);

	Optional<ExamCandidate> findByExamIdAndUserId(UUID examId, UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select candidate
			from ExamCandidate candidate
			where candidate.examId = :examId
			and candidate.userId = :userId
			""")
	Optional<ExamCandidate> findByExamIdAndUserIdForUpdate(
			@Param("examId") UUID examId,
			@Param("userId") UUID userId
	);
}
