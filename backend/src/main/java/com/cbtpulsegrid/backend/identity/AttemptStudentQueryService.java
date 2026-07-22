package com.cbtpulsegrid.backend.identity;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AttemptStudentQueryService implements AttemptStudentQuery {

	private final UserRepository userRepository;

	AttemptStudentQueryService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public StudentProfile requireActiveStudent(UUID institutionId, UUID userId) {
		User user = userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new NoSuchElementException("Student not found"));
		if (!institutionId.equals(user.getInstitutionId())) {
			throw new AccessDeniedException("Cross-institution student access is denied");
		}
		if (!user.getRoles().contains(Role.STUDENT)) {
			throw new AccessDeniedException("Student role is required");
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new AccessDeniedException("Student account is not active");
		}
		return new StudentProfile(user.getId(), user.getInstitutionId());
	}
}
