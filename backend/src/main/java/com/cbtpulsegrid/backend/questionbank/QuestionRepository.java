package com.cbtpulsegrid.backend.questionbank;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

	long countByInstitutionIdAndSubjectIdAndDifficultyAndStatus(
			UUID institutionId,
			UUID subjectId,
			QuestionDifficulty difficulty,
			QuestionStatus status
	);

	@Query("""
			select distinct question
			from Question question
			left join fetch question.options
			where question.id in :ids
			""")
	List<Question> findAllWithOptionsByIdIn(@Param("ids") List<UUID> ids);
}
