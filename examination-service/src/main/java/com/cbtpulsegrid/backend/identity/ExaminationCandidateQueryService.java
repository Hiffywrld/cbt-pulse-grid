package com.cbtpulsegrid.backend.identity;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ExaminationCandidateQueryService implements ExaminationCandidateQuery {

	private final UserRepository userRepository;

	ExaminationCandidateQueryService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, CandidateProfile> requireActiveStudents(UUID institutionId, Set<UUID> userIds) {
		Map<UUID, User> users = loadUsers(userIds);
		for (UUID userId : userIds) {
			User user = users.get(userId);
			if (!institutionId.equals(user.getInstitutionId())) {
				throw new AccessDeniedException("Cross-institution candidate assignment is denied");
			}
			if (user.getStatus() != UserStatus.ACTIVE) {
				throw new IllegalArgumentException("Assigned candidates must be ACTIVE");
			}
			if (!user.getRoles().contains(Role.STUDENT)) {
				throw new IllegalArgumentException("Assigned users must have the STUDENT role");
			}
		}
		return toProfiles(users);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, CandidateProfile> findByIds(Set<UUID> userIds) {
		return toProfiles(loadUsers(userIds));
	}

	private Map<UUID, User> loadUsers(Set<UUID> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, User> users = userRepository.findAllWithRolesByIdIn(userIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		if (users.size() != userIds.size()) {
			throw new NoSuchElementException("Candidate user not found");
		}
		return users;
	}

	private static Map<UUID, CandidateProfile> toProfiles(Map<UUID, User> users) {
		return users.values().stream().collect(Collectors.toUnmodifiableMap(
				User::getId,
				user -> new CandidateProfile(
						user.getId(),
						user.getInstitutionId(),
						user.getFirstName(),
						user.getLastName(),
						user.getEmail(),
						user.getRegistrationNumber(),
						user.getStatus()
				)
		));
	}
}
