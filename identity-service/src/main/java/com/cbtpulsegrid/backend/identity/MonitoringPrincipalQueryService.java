package com.cbtpulsegrid.backend.identity;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MonitoringPrincipalQueryService implements MonitoringPrincipalQuery {

	private final UserRepository userRepository;

	MonitoringPrincipalQueryService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<PrincipalView> findActive(UUID userId) {
		return userRepository.findWithRolesById(userId)
				.filter(user -> user.getStatus() == UserStatus.ACTIVE)
				.map(user -> new PrincipalView(
						user.getId(),
						user.getInstitutionId(),
						user.getRoles().stream()
								.map(Role::name)
								.collect(Collectors.toUnmodifiableSet())
				));
	}
}
