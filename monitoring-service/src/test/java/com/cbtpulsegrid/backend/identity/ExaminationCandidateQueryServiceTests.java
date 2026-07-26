package com.cbtpulsegrid.backend.identity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExaminationCandidateQueryServiceTests {

	private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_INSTITUTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	@Mock
	private UserRepository userRepository;

	private ExaminationCandidateQueryService queryService;

	@BeforeEach
	void createService() {
		queryService = new ExaminationCandidateQueryService(userRepository);
	}

	@Test
	void returnsAnActiveStudentFromTheRequestedInstitution() {
		User student = user(INSTITUTION_ID, UserStatus.ACTIVE, Set.of(Role.STUDENT));
		when(userRepository.findAllWithRolesByIdIn(Set.of(USER_ID))).thenReturn(List.of(student));

		var result = queryService.requireActiveStudents(INSTITUTION_ID, Set.of(USER_ID));

		assertEquals(USER_ID, result.get(USER_ID).id());
		assertEquals("STU-001", result.get(USER_ID).registrationNumber());
	}

	@Test
	void rejectsInactiveCandidates() {
		User student = user(INSTITUTION_ID, UserStatus.INACTIVE, Set.of(Role.STUDENT));
		when(userRepository.findAllWithRolesByIdIn(Set.of(USER_ID))).thenReturn(List.of(student));

		assertThrows(
				IllegalArgumentException.class,
				() -> queryService.requireActiveStudents(INSTITUTION_ID, Set.of(USER_ID))
		);
	}

	@Test
	void rejectsUsersWithoutTheStudentRole() {
		User examiner = user(INSTITUTION_ID, UserStatus.ACTIVE, Set.of(Role.EXAMINER));
		when(userRepository.findAllWithRolesByIdIn(Set.of(USER_ID))).thenReturn(List.of(examiner));

		assertThrows(
				IllegalArgumentException.class,
				() -> queryService.requireActiveStudents(INSTITUTION_ID, Set.of(USER_ID))
		);
	}

	@Test
	void rejectsCrossInstitutionCandidates() {
		User student = user(OTHER_INSTITUTION_ID, UserStatus.ACTIVE, Set.of(Role.STUDENT));
		when(userRepository.findAllWithRolesByIdIn(Set.of(USER_ID))).thenReturn(List.of(student));

		assertThrows(
				AccessDeniedException.class,
				() -> queryService.requireActiveStudents(INSTITUTION_ID, Set.of(USER_ID))
		);
	}

	private static User user(UUID institutionId, UserStatus status, Set<Role> roles) {
		User user = new User(
				"Ada",
				"Student",
				"ada@example.edu",
				"password-hash",
				status
		);
		user.setInstitutionId(institutionId);
		user.setRegistrationNumber("STU-001");
		user.setRoles(roles);
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}
}
