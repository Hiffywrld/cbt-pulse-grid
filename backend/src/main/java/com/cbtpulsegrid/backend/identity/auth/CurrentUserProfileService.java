package com.cbtpulsegrid.backend.identity.auth;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.institution.InstitutionProfile;
import com.cbtpulsegrid.backend.institution.InstitutionProfileQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserProfileService {

	private final UserRepository userRepository;
	private final InstitutionProfileQuery institutionProfileQuery;

	public CurrentUserProfileService(UserRepository userRepository, InstitutionProfileQuery institutionProfileQuery) {
		this.userRepository = userRepository;
		this.institutionProfileQuery = institutionProfileQuery;
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse get(UUID userId, UUID institutionId, List<String> roles) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NoSuchElementException("Authenticated user not found"));
		InstitutionProfile institution = institutionId == null ? null : institutionProfileQuery.requireProfile(institutionId);

		return new CurrentUserResponse(
				userId,
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				institutionId,
				institution == null ? null : institution.name(),
				institution == null ? null : institution.code(),
				List.copyOf(roles)
		);
	}
}
