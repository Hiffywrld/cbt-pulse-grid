package com.cbtpulsegrid.backend.identity.bootstrap;

import java.util.Locale;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DevelopmentAdminInitializer implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final BootstrapAdminProperties properties;

	public DevelopmentAdminInitializer(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			BootstrapAdminProperties properties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments arguments) {
		if (!properties.enabled() || userRepository.countByRolesContaining(Role.SUPER_ADMIN) > 0) {
			return;
		}

		String email = properties.email().trim().toLowerCase(Locale.ROOT);
		User admin = new User(
				"System",
				"Administrator",
				email,
				passwordEncoder.encode(properties.password()),
				UserStatus.ACTIVE
		);
		admin.getRoles().add(Role.SUPER_ADMIN);
		userRepository.save(admin);
	}
}
