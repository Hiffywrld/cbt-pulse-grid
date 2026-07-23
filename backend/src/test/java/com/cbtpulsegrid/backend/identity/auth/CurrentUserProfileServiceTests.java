package com.cbtpulsegrid.backend.identity.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.institution.InstitutionProfile;
import com.cbtpulsegrid.backend.institution.InstitutionProfileQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentUserProfileServiceTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final InstitutionProfileQuery institutionProfileQuery = mock(InstitutionProfileQuery.class);
	private final CurrentUserProfileService service = new CurrentUserProfileService(userRepository, institutionProfileQuery);

	@Test
	void returnsHumanFriendlyIdentityAndInstitutionForInstitutionUser() {
		UUID userId = UUID.randomUUID();
		UUID institutionId = UUID.randomUUID();
		User user = new User("Amina", "Okafor", "amina@niitlagos.local", "unused-hash", UserStatus.ACTIVE);
		InstitutionProfile institution = new InstitutionProfile(
				institutionId,
				"NIIT Lagos Campus",
				"NIIT-LAGOS"
		);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(institutionProfileQuery.requireProfile(institutionId)).thenReturn(institution);

		CurrentUserResponse response = service.get(
				userId,
				institutionId,
				List.of("INSTITUTION_ADMIN")
		);

		assertEquals("Amina", response.firstName());
		assertEquals("Okafor", response.lastName());
		assertEquals("NIIT Lagos Campus", response.institutionName());
		assertEquals("NIIT-LAGOS", response.institutionCode());
		assertEquals(List.of("INSTITUTION_ADMIN"), response.roles());
	}

	@Test
	void returnsNullInstitutionFieldsForPlatformAdministrator() {
		UUID userId = UUID.randomUUID();
		User user = new User("System", "Administrator", "admin@cbtpulse.local", "unused-hash", UserStatus.ACTIVE);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		CurrentUserResponse response = service.get(userId, null, List.of("SUPER_ADMIN"));

		assertEquals("System", response.firstName());
		assertEquals("Administrator", response.lastName());
		assertNull(response.institutionId());
		assertNull(response.institutionName());
		assertNull(response.institutionCode());
		verify(institutionProfileQuery, never()).requireProfile(org.mockito.ArgumentMatchers.any());
	}
}
