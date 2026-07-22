package com.cbtpulsegrid.backend.examination;

import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ExamSpecificationsTests {

	@Test
	void appliesTenantSubjectStatusAndCaseInsensitiveSearchPredicates() {
		UUID institutionId = UUID.randomUUID();
		UUID subjectId = UUID.randomUUID();
		Root<Exam> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
		Path<UUID> institutionPath = mock(Path.class);
		Path<UUID> subjectPath = mock(Path.class);
		Path<ExamStatus> statusPath = mock(Path.class);
		Path<String> codePath = mock(Path.class);
		Path<String> titlePath = mock(Path.class);
		Expression<String> loweredCode = mock(Expression.class);
		Expression<String> loweredTitle = mock(Expression.class);
		when(root.<UUID>get("institutionId")).thenReturn(institutionPath);
		when(root.<UUID>get("subjectId")).thenReturn(subjectPath);
		when(root.<ExamStatus>get("status")).thenReturn(statusPath);
		when(root.<String>get("code")).thenReturn(codePath);
		when(root.<String>get("title")).thenReturn(titlePath);
		when(criteriaBuilder.lower(codePath)).thenReturn(loweredCode);
		when(criteriaBuilder.lower(titlePath)).thenReturn(loweredTitle);

		ExamSpecifications.filteredBy(
				institutionId,
				subjectId,
				ExamStatus.DRAFT,
				"  AlGeBrA  "
		).toPredicate(root, query, criteriaBuilder);

		verify(criteriaBuilder).equal(institutionPath, institutionId);
		verify(criteriaBuilder).equal(subjectPath, subjectId);
		verify(criteriaBuilder).equal(statusPath, ExamStatus.DRAFT);
		verify(criteriaBuilder).like(loweredCode, "%algebra%");
		verify(criteriaBuilder).like(loweredTitle, "%algebra%");
	}
}
