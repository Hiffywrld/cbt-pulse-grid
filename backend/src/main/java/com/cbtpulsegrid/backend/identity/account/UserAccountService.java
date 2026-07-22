package com.cbtpulsegrid.backend.identity.account;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Set<Role> INSTITUTION_ADMIN_ASSIGNABLE_ROLES = Set.of(
			Role.EXAMINER,
			Role.INVIGILATOR,
			Role.STUDENT
	);

	private final UserRepository userRepository;
	private final InstitutionService institutionService;
	private final PasswordEncoder passwordEncoder;

	public UserAccountService(
			UserRepository userRepository,
			InstitutionService institutionService,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.institutionService = institutionService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse create(ActorContext actor, CreateUserRequest request) {
		Set<Role> roles = validateCreationRoles(actor, request.roles());
		UUID institutionId = resolveCreationInstitution(actor, request.institutionId());
		institutionService.requireActive(institutionId);

		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateKeyException("Email already exists");
		}

		String registrationNumber = normalizeRegistrationNumber(request.registrationNumber());
		validateRegistrationNumber(roles, institutionId, registrationNumber, null);

		User user = new User(
				request.firstName().trim(),
				request.lastName().trim(),
				email,
				passwordEncoder.encode(request.password()),
				UserStatus.ACTIVE
		);
		user.setInstitutionId(institutionId);
		user.setRegistrationNumber(registrationNumber);
		user.getRoles().addAll(roles);

		try {
			return toResponse(userRepository.saveAndFlush(user));
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("Email or registration number already exists", exception);
		}
	}

	@Transactional(readOnly = true)
	public UserPageResponse<UserResponse> list(
			ActorContext actor,
			String search,
			UUID institutionId,
			Role role,
			UserStatus status,
			int page,
			int size
	) {
		validatePage(page, size);
		UUID institutionScope = resolveInstitutionScope(actor, institutionId);
		String normalizedSearch = normalizeSearch(search);
		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName"))
		);
		Page<UserResponse> result = userRepository
				.search(normalizedSearch, institutionScope, role, status, pageRequest)
				.map(UserAccountService::toResponse);
		return UserPageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public UserResponse get(ActorContext actor, UUID id) {
		User user = findUser(id);
		assertCanAccess(actor, user);
		return toResponse(user);
	}

	@Transactional
	public UserResponse update(ActorContext actor, UUID id, UpdateUserRequest request) {
		User user = findUser(id);
		assertCanAccess(actor, user);

		String registrationNumber = normalizeRegistrationNumber(request.registrationNumber());
		validateRegistrationNumber(user.getRoles(), user.getInstitutionId(), registrationNumber, user.getId());
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setRegistrationNumber(registrationNumber);

		try {
			return toResponse(userRepository.saveAndFlush(user));
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateKeyException("Registration number already exists", exception);
		}
	}

	@Transactional
	public UserResponse changeStatus(
			ActorContext actor,
			UUID id,
			UserStatus status
	) {
		User user = findUser(id);
		assertCanAccess(actor, user);
		user.setStatus(status);
		return toResponse(userRepository.saveAndFlush(user));
	}

	private Set<Role> validateCreationRoles(ActorContext actor, Set<Role> requestedRoles) {
		assertManager(actor);
		if (requestedRoles == null || requestedRoles.isEmpty()) {
			throw new IllegalArgumentException("At least one role is required");
		}

		Set<Role> roles = Set.copyOf(requestedRoles);
		if (roles.contains(Role.SUPER_ADMIN)) {
			throw new AccessDeniedException("SUPER_ADMIN cannot be assigned through this API");
		}
		if (actor.isInstitutionAdmin() && !INSTITUTION_ADMIN_ASSIGNABLE_ROLES.containsAll(roles)) {
			throw new AccessDeniedException("Requested role assignment is not permitted");
		}
		if (roles.contains(Role.STUDENT) && roles.size() > 1) {
			throw new IllegalArgumentException("STUDENT cannot be combined with staff roles");
		}
		return roles;
	}

	private UUID resolveCreationInstitution(ActorContext actor, UUID requestedInstitutionId) {
		assertManager(actor);
		if (actor.isSuperAdmin()) {
			if (requestedInstitutionId == null) {
				throw new IllegalArgumentException("institutionId is required");
			}
			return requestedInstitutionId;
		}

		UUID actorInstitutionId = requireActorInstitution(actor);
		if (requestedInstitutionId != null && !actorInstitutionId.equals(requestedInstitutionId)) {
			throw new AccessDeniedException("Cross-institution access is denied");
		}
		return actorInstitutionId;
	}

	private UUID resolveInstitutionScope(ActorContext actor, UUID requestedInstitutionId) {
		assertManager(actor);
		if (actor.isSuperAdmin()) {
			if (requestedInstitutionId != null) {
				institutionService.requireActive(requestedInstitutionId);
			}
			return requestedInstitutionId;
		}

		UUID actorInstitutionId = requireActorInstitution(actor);
		if (requestedInstitutionId != null && !actorInstitutionId.equals(requestedInstitutionId)) {
			throw new AccessDeniedException("Cross-institution access is denied");
		}
		institutionService.requireActive(actorInstitutionId);
		return actorInstitutionId;
	}

	private void assertCanAccess(ActorContext actor, User user) {
		assertManager(actor);
		if (!actor.isSuperAdmin()) {
			UUID actorInstitutionId = requireActorInstitution(actor);
			if (!actorInstitutionId.equals(user.getInstitutionId())) {
				throw new AccessDeniedException("Cross-institution access is denied");
			}
		}
		if (user.getInstitutionId() != null) {
			institutionService.requireActive(user.getInstitutionId());
		}
	}

	private static void assertManager(ActorContext actor) {
		if (actor == null || (!actor.isSuperAdmin() && !actor.isInstitutionAdmin())) {
			throw new AccessDeniedException("User-account management is not permitted");
		}
	}

	private static UUID requireActorInstitution(ActorContext actor) {
		if (actor.institutionId() == null) {
			throw new AccessDeniedException("Institution context is required");
		}
		return actor.institutionId();
	}

	private User findUser(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("User not found"));
	}

	private void validateRegistrationNumber(
			Set<Role> roles,
			UUID institutionId,
			String registrationNumber,
			UUID existingUserId
	) {
		if (roles.contains(Role.STUDENT) && registrationNumber == null) {
			throw new IllegalArgumentException("registrationNumber is required for STUDENT users");
		}
		if (registrationNumber == null) {
			return;
		}

		boolean duplicate = existingUserId == null
				? userRepository.existsByInstitutionIdAndRegistrationNumberIgnoreCase(
						institutionId,
						registrationNumber
				)
				: userRepository.existsByInstitutionIdAndRegistrationNumberIgnoreCaseAndIdNot(
						institutionId,
						registrationNumber,
						existingUserId
				);
		if (duplicate) {
			throw new DuplicateKeyException("Registration number already exists");
		}
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeRegistrationNumber(String registrationNumber) {
		if (registrationNumber == null || registrationNumber.isBlank()) {
			return null;
		}
		return registrationNumber.trim().toUpperCase(Locale.ROOT);
	}

	private static String normalizeSearch(String search) {
		if (search == null || search.isBlank()) {
			return null;
		}
		return search.trim();
	}

	private static void validatePage(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must not be negative");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Page size must be between 1 and 100");
		}
	}

	private static UserResponse toResponse(User user) {
		Set<Role> roles = user.getRoles().stream()
				.sorted()
				.collect(Collectors.collectingAndThen(
						Collectors.toCollection(LinkedHashSet::new),
						Set::copyOf
				));
		return new UserResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				user.getInstitutionId(),
				roles,
				user.getRegistrationNumber(),
				user.getStatus(),
				user.getCreatedAt(),
				user.getUpdatedAt(),
				user.getVersion()
		);
	}
}
