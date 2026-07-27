package com.cbtpulsegrid.backend;

import java.util.List;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.Role;
import com.cbtpulsegrid.backend.identity.User;
import com.cbtpulsegrid.backend.identity.UserRepository;
import com.cbtpulsegrid.backend.identity.UserStatus;
import com.cbtpulsegrid.backend.institution.Institution;
import com.cbtpulsegrid.backend.institution.InstitutionRepository;
import com.cbtpulsegrid.backend.institution.InstitutionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase2ManagementListIntegrationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private InstitutionRepository institutionRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void platformDashboardAndInstitutionPageRequestsWorkWithoutSearch() throws Exception {
		Institution institution = institutionRepository.saveAndFlush(new Institution(
				"Runtime Regression Campus",
				"RUNTIME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
				InstitutionStatus.ACTIVE
		));

		mockMvc.perform(get("/api/v1/institutions")
						.param("page", "0")
						.param("size", "1")
						.with(superAdmin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").isNumber());

		mockMvc.perform(get("/api/v1/institutions")
						.param("page", "0")
						.param("size", "20")
						.with(superAdmin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id", hasItem(institution.getId().toString())));
	}

	@Test
	void institutionFiltersRemainCaseInsensitiveAndEnumSafe() throws Exception {
		Institution institution = institutionRepository.saveAndFlush(new Institution(
				"Mixed Case Academy",
				"MIXED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
				InstitutionStatus.SUSPENDED
		));

		mockMvc.perform(get("/api/v1/institutions")
						.param("search", "mIxEd CaSe")
						.param("status", "SUSPENDED")
						.param("page", "0")
						.param("size", "20")
						.with(superAdmin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id", hasItem(institution.getId().toString())));
	}

	@Test
	void platformDashboardAndAdministratorRequestsWorkWithOptionalFiltersAbsent() throws Exception {
		Institution institution = institutionRepository.saveAndFlush(new Institution(
				"Administrator Runtime Campus",
				"ADMIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
				InstitutionStatus.ACTIVE
		));
		User administrator = new User(
				"Runtime",
				"Administrator",
				"runtime-" + UUID.randomUUID() + "@example.test",
				"not-used-by-this-test",
				UserStatus.ACTIVE
		);
		administrator.setInstitutionId(institution.getId());
		administrator.getRoles().add(Role.INSTITUTION_ADMIN);
		administrator = userRepository.saveAndFlush(administrator);

		mockMvc.perform(get("/api/v1/users")
						.param("page", "0")
						.param("size", "1")
						.with(superAdmin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").isNumber());

		mockMvc.perform(get("/api/v1/users")
						.param("role", "INSTITUTION_ADMIN")
						.param("page", "0")
						.param("size", "20")
						.with(superAdmin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id", hasItem(administrator.getId().toString())));
	}

	@Test
	void userSearchAndCombinedEnumAndTenantFiltersRemainFunctional() throws Exception {
		Institution institution = institutionRepository.saveAndFlush(new Institution(
				"Filtered User Campus",
				"FILTER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
				InstitutionStatus.ACTIVE
		));
		User student = new User(
				"Amina",
				"Okafor",
				"amina-" + UUID.randomUUID() + "@example.test",
				"not-used-by-this-test",
				UserStatus.ACTIVE
		);
		student.setInstitutionId(institution.getId());
		student.setRegistrationNumber("REG-" + UUID.randomUUID().toString().substring(0, 8));
		student.getRoles().add(Role.STUDENT);
		student = userRepository.saveAndFlush(student);

		mockMvc.perform(get("/api/v1/users")
						.param("search", "aMiNa OkAfOr")
						.param("institutionId", institution.getId().toString())
						.param("role", "STUDENT")
						.param("status", "ACTIVE")
						.param("page", "0")
						.param("size", "20")
						.with(superAdmin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id", hasItem(student.getId().toString())));
	}

	private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdmin() {
		return jwt()
				.jwt(token -> token
						.subject(UUID.randomUUID().toString())
						.claim("roles", List.of("SUPER_ADMIN")))
				.authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
	}
}
