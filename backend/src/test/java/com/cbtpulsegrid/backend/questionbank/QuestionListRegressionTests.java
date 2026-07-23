package com.cbtpulsegrid.backend.questionbank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.audit.AuditTrail;
import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.questionbank.api.QuestionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionListRegressionTests {

	private static final UUID INSTITUTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID SUBJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private QuestionRepository questionRepository;
	private MockMvc mockMvc;

	@BeforeEach
	void configureControllerPath() {
		questionRepository = mock(QuestionRepository.class);
		SubjectService subjectService = mock(SubjectService.class);
		InstitutionService institutionService = mock(InstitutionService.class);
		QuestionService questionService = new QuestionService(
				questionRepository,
				subjectService,
				institutionService,
				new QuestionStructureValidator(),
				new QuestionBankAuthorization(),
				mock(AuditTrail.class)
		);
		QuestionController controller = new QuestionController(questionService);

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setCustomArgumentResolvers(jwtArgumentResolver(examinerJwt()))
				.build();
	}

	@Test
	void returnsPagedQuestionOptionsWithoutServerError() throws Exception {
		UUID questionId = UUID.randomUUID();
		Question question = questionWithOptions(questionId);
		PageRequest pageRequest = PageRequest.of(0, 10);
		when(questionRepository.findAll(
				any(Specification.class),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(question), pageRequest, 1));
		when(questionRepository.findAllWithOptionsByIdIn(List.of(questionId)))
				.thenReturn(List.of(question));

		mockMvc.perform(get("/api/v1/questions").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(questionId.toString()))
				.andExpect(jsonPath("$.content[0].options.length()").value(2))
				.andExpect(jsonPath("$.content[0].options[0].correct").value(true))
				.andExpect(jsonPath("$.content[0].options[1].correct").value(false));
	}

	@Test
	void returnsPagedQuestionOptionsForBlankSearch() throws Exception {
		UUID questionId = UUID.randomUUID();
		Question question = questionWithOptions(questionId);
		PageRequest pageRequest = PageRequest.of(0, 10);
		when(questionRepository.findAll(
				any(Specification.class),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(question), pageRequest, 1));
		when(questionRepository.findAllWithOptionsByIdIn(List.of(questionId)))
				.thenReturn(List.of(question));

		mockMvc.perform(get("/api/v1/questions")
						.param("search", "   ")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(questionId.toString()))
				.andExpect(jsonPath("$.content[0].options.length()").value(2));
	}

	private static Question questionWithOptions(UUID questionId) {
		Question question = new Question(
				INSTITUTION_ID,
				SUBJECT_ID,
				UUID.randomUUID(),
				"Which value is prime?",
				QuestionType.SINGLE_CHOICE,
				QuestionDifficulty.EASY,
				BigDecimal.ONE,
				QuestionStatus.DRAFT
		);
		question.replaceOptions(List.of(
				new QuestionOption("Two", true, 1),
				new QuestionOption("Four", false, 2)
		));
		ReflectionTestUtils.setField(question, "id", questionId);
		ReflectionTestUtils.setField(question, "createdAt", Instant.EPOCH);
		ReflectionTestUtils.setField(question, "updatedAt", Instant.EPOCH);
		return question;
	}

	private static Jwt examinerJwt() {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.claim("institutionId", INSTITUTION_ID.toString())
				.claim("roles", List.copyOf(Set.of("EXAMINER")))
				.build();
	}

	private static HandlerMethodArgumentResolver jwtArgumentResolver(Jwt jwt) {
		return new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
						&& Jwt.class.isAssignableFrom(parameter.getParameterType());
			}

			@Override
			public Object resolveArgument(
					MethodParameter parameter,
					ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest,
					WebDataBinderFactory binderFactory
			) {
				return jwt;
			}
		};
	}
}
