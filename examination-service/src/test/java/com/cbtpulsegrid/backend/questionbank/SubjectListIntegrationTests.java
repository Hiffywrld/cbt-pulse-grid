package com.cbtpulsegrid.backend.questionbank;

import java.util.List;
import java.util.UUID;

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
class SubjectListIntegrationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private InstitutionRepository institutionRepository;
	@Autowired
	private SubjectRepository subjectRepository;

	@Test
	void listsSubjectsWhenOptionalSearchAndStatusAreAbsent() throws Exception {
		Institution institution = institutionRepository.saveAndFlush(new Institution(
				"Subject List Campus",
				"SUBJECT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
				InstitutionStatus.ACTIVE
		));
		Subject subject = subjectRepository.saveAndFlush(new Subject(
				institution.getId(),
				"CSC-101",
				"Computer Science",
				null,
				SubjectStatus.ACTIVE
		));

		mockMvc.perform(get("/api/v1/subjects")
						.param("page", "0")
						.param("size", "20")
						.with(institutionAdmin(institution.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id", hasItem(subject.getId().toString())));
	}

	@Test
	void appliesCaseInsensitiveSearchAndStatusTogether() throws Exception {
		Institution institution = institutionRepository.saveAndFlush(new Institution(
				"Filtered Subject Campus",
				"FILTER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
				InstitutionStatus.ACTIVE
		));
		Subject subject = subjectRepository.saveAndFlush(new Subject(
				institution.getId(),
				"MAT-201",
				"Applied Mathematics",
				null,
				SubjectStatus.INACTIVE
		));

		mockMvc.perform(get("/api/v1/subjects")
						.param("search", "aPpLiEd MaTh")
						.param("status", "INACTIVE")
						.param("page", "0")
						.param("size", "20")
						.with(institutionAdmin(institution.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].id", hasItem(subject.getId().toString())));
	}

	private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor institutionAdmin(
			UUID institutionId
	) {
		return jwt()
				.jwt(token -> token
						.subject(UUID.randomUUID().toString())
						.claim("institutionId", institutionId.toString())
						.claim("roles", List.of("INSTITUTION_ADMIN")))
				.authorities(new SimpleGrantedAuthority("ROLE_INSTITUTION_ADMIN"));
	}
}
