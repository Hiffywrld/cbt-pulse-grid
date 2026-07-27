package com.cbtpulsegrid.backend.attempt;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, UUID> {

	@Query("""
			select distinct question
			from AttemptQuestion question
			left join fetch question.options
			where question.attemptId = :attemptId
			order by question.position asc
			""")
	List<AttemptQuestion> findAllWithOptionsByAttemptId(@Param("attemptId") UUID attemptId);
}
