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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DevelopmentAdminInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevelopmentAdminInitializer.class);
	private static final String ADMIN_FIRST_NAME = "System";
	private static final String ADMIN_LAST_NAME = "Administrator";

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
		if (!properties.enabled()) {
			return;
		}

		String email = properties.email().trim().toLowerCase(Locale.ROOT);
		if (properties.forceReset()) {
			userRepository.findByEmailIgnoreCase(email)
					.ifPresentOrElse(
							admin -> {
								admin.setPasswordHash(passwordEncoder.encode(properties.password()));
								admin.setStatus(UserStatus.ACTIVE);
								admin.getRoles().add(Role.SUPER_ADMIN);
								userRepository.save(admin);
								log.warn("Bootstrap admin recovery ran for configured email");
							},
							() -> createBootstrapAdmin(email)
					);
			return;
		}

		if (userRepository.countByRolesContaining(Role.SUPER_ADMIN) > 0) {
			return;
		}

		createBootstrapAdmin(email);
	}

	private void createBootstrapAdmin(String email) {
		User admin = new User(
				ADMIN_FIRST_NAME,
				ADMIN_LAST_NAME,
				email,
				passwordEncoder.encode(properties.password()),
				UserStatus.ACTIVE
		);
		admin.getRoles().add(Role.SUPER_ADMIN);
		userRepository.save(admin);
	}
}
