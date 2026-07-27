package com.cbtpulsegrid.backend.identity.bootstrap;

import java.util.Optional;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevelopmentAdminInitializerTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

	@Test
	void forceResetUpdatesExistingAdminPasswordAndRoleWithoutDuplicateUser() {
		User existing = new User(
				"Old",
				"Admin",
				"admin@example.test",
				"old-hash",
				UserStatus.LOCKED
		);
		when(userRepository.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(existing));
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		initializer(true).run(null);

		assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
		assertThat(existing.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(existing.getRoles()).contains(Role.SUPER_ADMIN);
		verify(userRepository).save(existing);
		verify(userRepository, never()).countByRolesContaining(Role.SUPER_ADMIN);
	}

	@Test
	void forceResetCreatesBootstrapAdminWhenConfiguredEmailDoesNotExist() {
		when(userRepository.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("new-password")).thenReturn("created-hash");

		initializer(true).run(null);

		verify(userRepository).save(any(User.class));
		verify(userRepository, never()).countByRolesContaining(Role.SUPER_ADMIN);
	}

	@Test
	void normalBootstrapStillSkipsCreationWhenSuperAdminAlreadyExists() {
		when(userRepository.countByRolesContaining(Role.SUPER_ADMIN)).thenReturn(1L);

		initializer(false).run(null);

		verify(userRepository).countByRolesContaining(Role.SUPER_ADMIN);
		verify(userRepository, never()).findByEmailIgnoreCase(any());
		verify(userRepository, never()).save(any());
	}

	private DevelopmentAdminInitializer initializer(boolean forceReset) {
		return new DevelopmentAdminInitializer(
				userRepository,
				passwordEncoder,
				new BootstrapAdminProperties(
						true,
						forceReset,
						"Admin@Example.Test",
						"new-password"
				)
		);
	}
}
