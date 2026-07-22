package com.cbtpulsegrid.backend.attempt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

	Optional<ExamAttempt> findByExamIdAndCandidateId(UUID examId, UUID candidateId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select attempt from ExamAttempt attempt where attempt.id = :id")
	Optional<ExamAttempt> findByIdForUpdate(@Param("id") UUID id);

	@Query("""
			select attempt.id
			from ExamAttempt attempt
			where attempt.status = :status
			and attempt.expiresAt <= :now
			order by attempt.expiresAt asc
			""")
	List<UUID> findExpiredIds(
			@Param("status") AttemptStatus status,
			@Param("now") Instant now,
			Pageable pageable
	);
}
