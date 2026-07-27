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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Set;
import com.cbtpulsegrid.backend.identity.RefreshTokenRepository;

@Service
public class CurrentUserProfileService {

	private final UserRepository userRepository;
	private final InstitutionProfileQuery institutionProfileQuery;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private static final Set<String> AVATARS = Set.of(
			"emerald-orbit", "amber-sun", "indigo-wave", "coral-arc",
			"teal-grid", "violet-bloom", "navy-pulse", "rose-kite",
			"forest-ring", "golden-node", "cyan-path", "plum-spark"
	);

	public CurrentUserProfileService(UserRepository userRepository, InstitutionProfileQuery institutionProfileQuery,
			PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository) {
		this.userRepository = userRepository;
		this.institutionProfileQuery = institutionProfileQuery;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenRepository = refreshTokenRepository;
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
				user.getRegistrationNumber(),
				institutionId,
				institution == null ? null : institution.name(),
				institution == null ? null : institution.code(),
				user.getAvatarKey(),
				List.copyOf(roles)
		);
	}

	@Transactional
	public CurrentUserResponse update(UUID userId, UUID institutionId, List<String> roles, UpdateProfileRequest request) {
		User user = requireUser(userId);
		String avatar = request.avatarKey();
		if (avatar != null && !AVATARS.contains(avatar)) {
			throw new IllegalArgumentException("Avatar selection is invalid");
		}
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setAvatarKey(avatar);
		return get(userId, institutionId, roles);
	}

	@Transactional
	public void changePassword(UUID userId, ChangePasswordRequest request) {
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new IllegalArgumentException("Password confirmation does not match");
		}
		User user = requireUser(userId);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BadCredentialsException("Current password is incorrect");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
	}

	private User requireUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new NoSuchElementException("Authenticated user not found"));
	}
}
