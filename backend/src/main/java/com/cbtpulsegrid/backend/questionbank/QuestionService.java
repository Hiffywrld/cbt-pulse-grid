package com.cbtpulsegrid.backend.questionbank;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cbtpulsegrid.backend.institution.InstitutionService;
import com.cbtpulsegrid.backend.questionbank.QuestionStructureValidator.OptionRule;
import com.cbtpulsegrid.backend.questionbank.api.CreateQuestionRequest;
import com.cbtpulsegrid.backend.questionbank.api.QuestionBankActor;
import com.cbtpulsegrid.backend.questionbank.api.QuestionBankPageResponse;
import com.cbtpulsegrid.backend.questionbank.api.QuestionOptionRequest;
import com.cbtpulsegrid.backend.questionbank.api.StaffQuestionOptionResponse;
import com.cbtpulsegrid.backend.questionbank.api.StaffQuestionResponse;
import com.cbtpulsegrid.backend.questionbank.api.UpdateQuestionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

	private static final int MAX_PAGE_SIZE = 100;

	private final QuestionRepository questionRepository;
	private final SubjectService subjectService;
	private final InstitutionService institutionService;
	private final QuestionStructureValidator structureValidator;
	private final QuestionBankAuthorization authorization;

	public QuestionService(
			QuestionRepository questionRepository,
			SubjectService subjectService,
			InstitutionService institutionService,
			QuestionStructureValidator structureValidator,
			QuestionBankAuthorization authorization
	) {
		this.questionRepository = questionRepository;
		this.subjectService = subjectService;
		this.institutionService = institutionService;
		this.structureValidator = structureValidator;
		this.authorization = authorization;
	}

	@Transactional
	public StaffQuestionResponse create(QuestionBankActor actor, CreateQuestionRequest request) {
		UUID institutionId = authorization.requireQuestionManagementAccess(actor);
		institutionService.requireActive(institutionId);
		subjectService.requireActiveSubject(institutionId, request.subjectId());
		validateStructure(request.type(), request.marks(), request.options());

		Question question = new Question(
				institutionId,
				request.subjectId(),
				actor.userId(),
				request.questionText().trim(),
				request.type(),
				request.difficulty(),
				request.marks(),
				QuestionStatus.DRAFT
		);
		question.replaceOptions(toOptions(request.options()));
		return toStaffResponse(questionRepository.saveAndFlush(question));
	}

	@Transactional(readOnly = true)
	public QuestionBankPageResponse<StaffQuestionResponse> list(
			QuestionBankActor actor,
			UUID subjectId,
			QuestionType type,
			QuestionDifficulty difficulty,
			QuestionStatus status,
			String search,
			int page,
			int size
	) {
		UUID institutionId = authorization.requireQuestionManagementAccess(actor);
		institutionService.requireActive(institutionId);
		validatePage(page, size);
		if (subjectId != null) {
			subjectService.requireOwnedSubject(institutionId, subjectId);
		}

		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "updatedAt")
		);
		Page<Question> questionPage = questionRepository.findAll(
				QuestionSpecifications.filteredBy(
						institutionId,
						subjectId,
						type,
						difficulty,
						status,
						normalizeSearch(search)
				),
				pageRequest
		);

		if (questionPage.isEmpty()) {
			return QuestionBankPageResponse.from(
					new PageImpl<>(List.of(), questionPage.getPageable(), questionPage.getTotalElements())
			);
		}

		List<UUID> questionIds = questionPage.getContent().stream()
				.map(Question::getId)
				.toList();
		Map<UUID, Question> questionsWithOptions = questionRepository.findAllWithOptionsByIdIn(questionIds)
				.stream()
				.collect(Collectors.toMap(
						Question::getId,
						question -> question,
						(first, duplicate) -> first,
						LinkedHashMap::new
				));
		List<StaffQuestionResponse> content = questionIds.stream()
				.map(id -> requireFetchedQuestion(id, questionsWithOptions))
				.map(QuestionService::toStaffResponse)
				.toList();

		Page<StaffQuestionResponse> result = new PageImpl<>(
				content,
				questionPage.getPageable(),
				questionPage.getTotalElements()
		);
		return QuestionBankPageResponse.from(result);
	}

	@Transactional(readOnly = true)
	public StaffQuestionResponse get(QuestionBankActor actor, UUID id) {
		UUID institutionId = authorization.requireQuestionManagementAccess(actor);
		institutionService.requireActive(institutionId);
		return toStaffResponse(requireOwnedQuestion(institutionId, id));
	}

	@Transactional
	public StaffQuestionResponse update(QuestionBankActor actor, UUID id, UpdateQuestionRequest request) {
		UUID institutionId = authorization.requireQuestionManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Question question = requireOwnedQuestion(institutionId, id);
		subjectService.requireActiveSubject(institutionId, request.subjectId());
		validateStructure(request.type(), request.marks(), request.options());

		question.setSubjectId(request.subjectId());
		question.setQuestionText(request.questionText().trim());
		question.setType(request.type());
		question.setDifficulty(request.difficulty());
		question.setMarks(request.marks());
		question.replaceOptions(toOptions(request.options()));
		return toStaffResponse(questionRepository.saveAndFlush(question));
	}

	@Transactional
	public StaffQuestionResponse changeStatus(
			QuestionBankActor actor,
			UUID id,
			QuestionStatus status
	) {
		UUID institutionId = authorization.requireQuestionManagementAccess(actor);
		institutionService.requireActive(institutionId);
		Question question = requireOwnedQuestion(institutionId, id);

		if (status == QuestionStatus.PUBLISHED) {
			subjectService.requireActiveSubject(institutionId, question.getSubjectId());
			structureValidator.validate(
					question.getType(),
					question.getMarks(),
					question.getOptions().stream()
							.map(option -> new OptionRule(option.isCorrect(), option.getDisplayOrder()))
							.toList()
			);
		}
		question.setStatus(status);
		return toStaffResponse(questionRepository.saveAndFlush(question));
	}

	private Question requireOwnedQuestion(UUID institutionId, UUID id) {
		Question question = questionRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Question not found"));
		authorization.requireTenant(institutionId, question.getInstitutionId());
		return question;
	}

	private static Question requireFetchedQuestion(UUID id, Map<UUID, Question> questionsById) {
		Question question = questionsById.get(id);
		if (question == null) {
			throw new IllegalStateException("Paged question could not be loaded");
		}
		return question;
	}

	private void validateStructure(
			QuestionType type,
			java.math.BigDecimal marks,
			List<QuestionOptionRequest> options
	) {
		List<OptionRule> optionRules = options == null
				? null
				: options.stream()
						.map(option -> new OptionRule(option.correct(), option.displayOrder()))
						.toList();
		structureValidator.validate(
				type,
				marks,
				optionRules
		);
	}

	private static List<QuestionOption> toOptions(List<QuestionOptionRequest> options) {
		return options.stream()
				.map(option -> new QuestionOption(
						option.optionText().trim(),
						option.correct(),
						option.displayOrder()
				))
				.toList();
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

	private static StaffQuestionResponse toStaffResponse(Question question) {
		List<StaffQuestionOptionResponse> options = question.getOptions().stream()
				.sorted(Comparator.comparingInt(QuestionOption::getDisplayOrder))
				.map(option -> new StaffQuestionOptionResponse(
						option.getId(),
						option.getOptionText(),
						option.isCorrect(),
						option.getDisplayOrder()
				))
				.toList();
		return new StaffQuestionResponse(
				question.getId(),
				question.getInstitutionId(),
				question.getSubjectId(),
				question.getCreatedBy(),
				question.getQuestionText(),
				question.getType(),
				question.getDifficulty(),
				question.getMarks(),
				question.getStatus(),
				options,
				question.getCreatedAt(),
				question.getUpdatedAt(),
				question.getVersion()
		);
	}

}
