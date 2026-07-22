package com.cbtpulsegrid.backend.questionbank;

import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class QuestionSpecificationsTests {

	private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID SUBJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private final Root<Question> root = mock(Root.class);
	private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
	private final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

	@Test
	void omitsTextPredicateWhenSearchIsNull() {
		apply(null, null, null, null, null);

		verify(criteriaBuilder, never()).lower(any());
		verify(criteriaBuilder, never()).like(any(), anyString());
	}

	@Test
	void omitsTextPredicateWhenSearchIsBlank() {
		apply(null, null, null, null, "   ");

		verify(criteriaBuilder, never()).lower(any());
		verify(criteriaBuilder, never()).like(any(), anyString());
	}

	@Test
	void createsCaseInsensitiveTextPredicateOnlyForNonBlankSearch() {
		Path<String> questionText = mock(Path.class);
		Expression<String> loweredQuestionText = mock(Expression.class);
		when(root.<String>get("questionText")).thenReturn(questionText);
		when(criteriaBuilder.lower(questionText)).thenReturn(loweredQuestionText);

		apply(null, null, null, null, "  MiXeD CaSe  ");

		verify(criteriaBuilder).lower(questionText);
		verify(criteriaBuilder).like(loweredQuestionText, "%mixed case%");
	}

	@Test
	void includesTenantSubjectTypeDifficultyAndStatusPredicates() {
		Path<UUID> institutionId = mock(Path.class);
		Path<UUID> subjectId = mock(Path.class);
		Path<QuestionType> type = mock(Path.class);
		Path<QuestionDifficulty> difficulty = mock(Path.class);
		Path<QuestionStatus> status = mock(Path.class);
		when(root.<UUID>get("institutionId")).thenReturn(institutionId);
		when(root.<UUID>get("subjectId")).thenReturn(subjectId);
		when(root.<QuestionType>get("type")).thenReturn(type);
		when(root.<QuestionDifficulty>get("difficulty")).thenReturn(difficulty);
		when(root.<QuestionStatus>get("status")).thenReturn(status);

		apply(
				SUBJECT_ID,
				QuestionType.SINGLE_CHOICE,
				QuestionDifficulty.HARD,
				QuestionStatus.PUBLISHED,
				null
		);

		verify(criteriaBuilder).equal(institutionId, INSTITUTION_ID);
		verify(criteriaBuilder).equal(subjectId, SUBJECT_ID);
		verify(criteriaBuilder).equal(type, QuestionType.SINGLE_CHOICE);
		verify(criteriaBuilder).equal(difficulty, QuestionDifficulty.HARD);
		verify(criteriaBuilder).equal(status, QuestionStatus.PUBLISHED);
	}

	private void apply(
			UUID subjectId,
			QuestionType type,
			QuestionDifficulty difficulty,
			QuestionStatus status,
			String search
	) {
		QuestionSpecifications.filteredBy(
				INSTITUTION_ID,
				subjectId,
				type,
				difficulty,
				status,
				search
		).toPredicate(root, query, criteriaBuilder);
	}
}
