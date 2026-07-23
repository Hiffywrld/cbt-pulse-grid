package com.cbtpulsegrid.backend.questionbank;

import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.questionbank.api.CreateSubjectRequest;
import com.cbtpulsegrid.backend.questionbank.api.QuestionBankActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTests {

	private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_INSTITUTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private SubjectRepository subjectRepository;

	@Mock
	private InstitutionService institutionService;
	@Mock
	private AuditTrail auditTrail;

	private SubjectService subjectService;

	@BeforeEach
	void createService() {
		subjectService = new SubjectService(
				subjectRepository,
				institutionService,
				new QuestionBankAuthorization(),
				auditTrail
		);
	}

	@Test
	void normalizesAndCreatesUniqueSubjectCode() {
		when(subjectRepository.existsByInstitutionIdAndCodeIgnoreCase(INSTITUTION_ID, "CSC-101"))
				.thenReturn(false);
		when(subjectRepository.saveAndFlush(any(Subject.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		subjectService.create(
				institutionAdmin(INSTITUTION_ID),
				new CreateSubjectRequest("  csc-101  ", "Computer Science", null)
		);

		ArgumentCaptor<Subject> captor = ArgumentCaptor.forClass(Subject.class);
		verify(subjectRepository).saveAndFlush(captor.capture());
		assertEquals("CSC-101", captor.getValue().getCode());
	}

	@Test
	void rejectsDuplicateSubjectCode() {
		when(subjectRepository.existsByInstitutionIdAndCodeIgnoreCase(INSTITUTION_ID, "CSC-101"))
				.thenReturn(true);

		assertThrows(DuplicateKeyException.class, () -> subjectService.create(
				institutionAdmin(INSTITUTION_ID),
				new CreateSubjectRequest("csc-101", "Computer Science", null)
		));
		verify(subjectRepository, never()).saveAndFlush(any(Subject.class));
	}

	@Test
	void rejectsCrossInstitutionSubjectAccess() {
		UUID subjectId = UUID.randomUUID();
		Subject subject = new Subject(
				OTHER_INSTITUTION_ID,
				"CSC-101",
				"Computer Science",
				null,
				SubjectStatus.ACTIVE
		);
		when(subjectRepository.findById(subjectId)).thenReturn(java.util.Optional.of(subject));

		assertThrows(
				AccessDeniedException.class,
				() -> subjectService.get(institutionAdmin(INSTITUTION_ID), subjectId)
		);
	}

	private static QuestionBankActor institutionAdmin(UUID institutionId) {
		return new QuestionBankActor(UUID.randomUUID(), institutionId, Set.of("INSTITUTION_ADMIN"));
	}
}
