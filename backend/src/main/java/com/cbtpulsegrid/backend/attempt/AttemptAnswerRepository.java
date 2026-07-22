package com.cbtpulsegrid.backend.attempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {

	Optional<AttemptAnswer> findByAttemptIdAndAttemptQuestionId(UUID attemptId, UUID attemptQuestionId);

	@Query("""
			select distinct answer
			from AttemptAnswer answer
			left join fetch answer.selectedOptionIds
			where answer.attemptId = :attemptId
			""")
	List<AttemptAnswer> findAllWithSelectionsByAttemptId(@Param("attemptId") UUID attemptId);
}
