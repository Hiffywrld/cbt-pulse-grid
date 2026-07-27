package com.cbtpulsegrid.backend.identity.account;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTests {

	private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_INSTITUTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private UserRepository userRepository;

	@Mock
	private InstitutionService institutionService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuditTrail auditTrail;

	@InjectMocks
	private UserAccountService userAccountService;

	@Test
	void createsUserWithNormalizedFieldsAndHashedPassword() {
		ActorContext actor = superAdmin();
		CreateUserRequest request = studentRequest(INSTITUTION_ID, "  Student@One.EDU  ", "  stu-001  ");
		when(userRepository.existsByEmailIgnoreCase("student@one.edu")).thenReturn(false);
		when(userRepository.existsByInstitutionIdAndRegistrationNumberIgnoreCase(INSTITUTION_ID, "STU-001"))
				.thenReturn(false);
		when(passwordEncoder.encode("StrongPass1!")).thenReturn("bcrypt-hash");
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userAccountService.create(actor, request);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).saveAndFlush(captor.capture());
		User saved = captor.getValue();
		assertEquals("student@one.edu", saved.getEmail());
		assertEquals("STU-001", saved.getRegistrationNumber());
		assertEquals("bcrypt-hash", saved.getPasswordHash());
		assertEquals(UserStatus.ACTIVE, saved.getStatus());
		assertEquals(Set.of(Role.STUDENT), saved.getRoles());
		assertEquals("student@one.edu", response.email());
		assertFalse(Arrays.stream(UserResponse.class.getRecordComponents())
				.anyMatch(component -> component.getName().equals("passwordHash")
						|| component.getName().equals("password")));
		verify(passwordEncoder).encode("StrongPass1!");
		verify(institutionService).requireActive(INSTITUTION_ID);
	}

	@Test
	void bcryptProducesANonPlaintextVerifiableHash() {
		BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

		String hash = bcrypt.encode("StrongPass1!");

		assertNotEquals("StrongPass1!", hash);
		assertTrue(bcrypt.matches("StrongPass1!", hash));
	}

	@Test
	void rejectsDuplicateEmailBeforeHashingPassword() {
		when(userRepository.existsByEmailIgnoreCase("student@one.edu")).thenReturn(true);

		assertThrows(
				DuplicateKeyException.class,
				() -> userAccountService.create(
						superAdmin(),
						studentRequest(INSTITUTION_ID, "student@one.edu", "STU-001")
				)
		);
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void rejectsDuplicateRegistrationNumberWithinInstitution() {
		when(userRepository.existsByEmailIgnoreCase("student@one.edu")).thenReturn(false);
		when(userRepository.existsByInstitutionIdAndRegistrationNumberIgnoreCase(INSTITUTION_ID, "STU-001"))
				.thenReturn(true);

		assertThrows(
				DuplicateKeyException.class,
				() -> userAccountService.create(
						superAdmin(),
						studentRequest(INSTITUTION_ID, "student@one.edu", "stu-001")
				)
		);
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void requiresRegistrationNumberForStudents() {
		when(userRepository.existsByEmailIgnoreCase("student@one.edu")).thenReturn(false);

		assertThrows(
				IllegalArgumentException.class,
				() -> userAccountService.create(
						superAdmin(),
						studentRequest(INSTITUTION_ID, "student@one.edu", null)
				)
		);
	}

	@Test
	void preventsInstitutionAdminRoleEscalation() {
		CreateUserRequest request = new CreateUserRequest(
				"Another",
				"Admin",
				"another@one.edu",
				"StrongPass1!",
				INSTITUTION_ID,
				Set.of(Role.INSTITUTION_ADMIN),
				null
		);

		assertThrows(
				AccessDeniedException.class,
				() -> userAccountService.create(institutionAdmin(INSTITUTION_ID), request)
		);
	}

	@Test
	void preventsCreationOfAnotherSuperAdmin() {
		CreateUserRequest request = new CreateUserRequest(
				"Another",
				"Super",
				"super@one.edu",
				"StrongPass1!",
				INSTITUTION_ID,
				Set.of(Role.SUPER_ADMIN),
				null
		);

		assertThrows(
				AccessDeniedException.class,
				() -> userAccountService.create(superAdmin(), request)
		);
	}

	@Test
	void preventsCombiningStudentAndStaffRoles() {
		CreateUserRequest request = new CreateUserRequest(
				"Mixed",
				"Role",
				"mixed@one.edu",
				"StrongPass1!",
				INSTITUTION_ID,
				Set.of(Role.STUDENT, Role.EXAMINER),
				"STU-002"
		);

		assertThrows(
				IllegalArgumentException.class,
				() -> userAccountService.create(superAdmin(), request)
		);
	}

	@Test
	void rejectsCrossInstitutionCreationByInstitutionAdmin() {
		CreateUserRequest request = new CreateUserRequest(
				"Exam",
				"Officer",
				"examiner@one.edu",
				"StrongPass1!",
				OTHER_INSTITUTION_ID,
				Set.of(Role.EXAMINER),
				null
		);

		assertThrows(
				AccessDeniedException.class,
				() -> userAccountService.create(institutionAdmin(INSTITUTION_ID), request)
		);
	}

	@Test
	void rejectsCrossInstitutionUserAccess() {
		UUID userId = UUID.randomUUID();
		User user = new User("Other", "Student", "other@student.edu", "hash", UserStatus.ACTIVE);
		user.setInstitutionId(OTHER_INSTITUTION_ID);
		user.getRoles().add(Role.STUDENT);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		assertThrows(
				AccessDeniedException.class,
				() -> userAccountService.get(institutionAdmin(INSTITUTION_ID), userId)
		);
	}

	private static CreateUserRequest studentRequest(UUID institutionId, String email, String registrationNumber) {
		return new CreateUserRequest(
				"Student",
				"One",
				email,
				"StrongPass1!",
				institutionId,
				Set.of(Role.STUDENT),
				registrationNumber
		);
	}

	private static ActorContext superAdmin() {
		return new ActorContext(UUID.randomUUID(), null, Set.of(Role.SUPER_ADMIN));
	}

	private static ActorContext institutionAdmin(UUID institutionId) {
		return new ActorContext(UUID.randomUUID(), institutionId, Set.of(Role.INSTITUTION_ADMIN));
	}
}
