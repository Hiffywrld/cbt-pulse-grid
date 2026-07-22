package com.cbtpulsegrid.backend.identity.security;

import java.util.Locale;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public IdentityUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

		String[] authorities = user.getRoles().stream()
				.map(Role::name)
				.sorted()
				.map(role -> "ROLE_" + role)
				.toArray(String[]::new);

		return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
				.password(user.getPasswordHash())
				.authorities(authorities)
				.disabled(user.getStatus() == UserStatus.INACTIVE)
				.accountLocked(user.getStatus() == UserStatus.LOCKED)
				.build();
	}
}
