package com.cbtpulsegrid.backend.attempt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.AttemptStudentQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptMonitoringQueryServiceTests {

	private static final UUID INSTITUTION_ID = UUID.randomUUID();
	private static final UUID CANDIDATE_ID = UUID.randomUUID();
	private static final UUID ATTEMPT_ID = UUID.randomUUID();

	@Mock
	private ExamAttemptRepository attemptRepository;
	@Mock
	private AttemptStudentQuery studentQuery;

	private AttemptMonitoringQueryService queryService;

	@BeforeEach
	void createService() {
		queryService = new AttemptMonitoringQueryService(attemptRepository, studentQuery);
	}

	@Test
	void verifiesStudentOwnershipAndTheLockedDevice() throws Exception {
		ExamAttempt attempt = attempt(INSTITUTION_ID, CANDIDATE_ID, hash("device-a"));
		when(attemptRepository.findByIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.of(attempt));

		var result = queryService.requireOwnedActiveAttemptAndDevice(
				INSTITUTION_ID,
				CANDIDATE_ID,
				ATTEMPT_ID,
				"device-a"
		);

		assertEquals(ATTEMPT_ID, result.id());
		verify(studentQuery).requireActiveStudent(INSTITUTION_ID, CANDIDATE_ID);
	}

	@Test
	void rejectsAnotherDevice() throws Exception {
		when(attemptRepository.findByIdForUpdate(ATTEMPT_ID))
				.thenReturn(Optional.of(attempt(INSTITUTION_ID, CANDIDATE_ID, hash("device-a"))));

		assertThrows(
				AttemptConflictException.class,
				() -> queryService.requireOwnedActiveAttemptAndDevice(
						INSTITUTION_ID,
						CANDIDATE_ID,
						ATTEMPT_ID,
						"device-b"
				)
		);
	}

	@Test
	void rejectsAnotherStudentOrInstitution() throws Exception {
		when(attemptRepository.findByIdForUpdate(ATTEMPT_ID))
				.thenReturn(Optional.of(attempt(INSTITUTION_ID, CANDIDATE_ID, hash("device"))));

		assertThrows(
				AccessDeniedException.class,
				() -> queryService.requireOwnedActiveAttempt(
						INSTITUTION_ID,
						UUID.randomUUID(),
						ATTEMPT_ID
				)
		);

		assertThrows(
				AccessDeniedException.class,
				() -> queryService.requireOwnedActiveAttempt(
						UUID.randomUUID(),
						CANDIDATE_ID,
						ATTEMPT_ID
				)
		);
	}

	@Test
	void rejectsMonitoringAfterSubmission() throws Exception {
		ExamAttempt attempt = attempt(INSTITUTION_ID, CANDIDATE_ID, hash("device"));
		ReflectionTestUtils.setField(attempt, "status", AttemptStatus.SUBMITTED);
		when(attemptRepository.findByIdForUpdate(ATTEMPT_ID)).thenReturn(Optional.of(attempt));

		assertThrows(
				AttemptConflictException.class,
				() -> queryService.requireOwnedActiveAttempt(
						INSTITUTION_ID,
						CANDIDATE_ID,
						ATTEMPT_ID
				)
		);
	}

	private static ExamAttempt attempt(
			UUID institutionId,
			UUID candidateId,
			String deviceHash
	) {
		ExamAttempt attempt = new ExamAttempt(
				institutionId,
				UUID.randomUUID(),
				candidateId,
				deviceHash,
				Instant.now(),
				Instant.now().plusSeconds(3600)
		);
		ReflectionTestUtils.setField(attempt, "id", ATTEMPT_ID);
		return attempt;
	}

	private static String hash(String value) throws Exception {
		return HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
		);
	}
}
