package com.cbtpulsegrid.backend.result;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.QuestionType;
import com.cbtpulsegrid.backend.result.api.CandidateResultResponse;
import com.cbtpulsegrid.backend.result.api.CandidateResultStatus;
import com.cbtpulsegrid.backend.result.api.ExamResultSummaryResponse;
import com.cbtpulsegrid.backend.result.api.ResultActor;
import com.cbtpulsegrid.backend.result.api.ResultPageResponse;
import com.cbtpulsegrid.backend.result.api.StaffAttemptResultResponse;
import com.cbtpulsegrid.backend.result.api.StaffOptionReviewResponse;
import com.cbtpulsegrid.backend.result.api.StaffQuestionReviewResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultService {

	private static final int MAX_PAGE_SIZE = 100;

	private static final String CANDIDATE_FROM = """
			from exam_candidates assignment
			join exams exam on exam.id = assignment.exam_id
			join users candidate on candidate.id = assignment.user_id
			left join exam_attempts attempt
			  on attempt.exam_id = assignment.exam_id
			 and attempt.candidate_id = assignment.user_id
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public ResultService(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public ExamResultSummaryResponse summary(ResultActor actor, UUID examId) {
		UUID institutionId = requireStaff(actor);
		String sql = """
				select exam.id,
				       exam.code,
				       exam.title,
				       count(assignment.id) as assigned_candidates,
				       count(assignment.id) filter (where attempt.id is null) as not_started,
				       count(attempt.id) filter (where attempt.status = 'IN_PROGRESS') as in_progress,
				       count(attempt.id) filter (where attempt.status = 'SUBMITTED') as submitted,
				       count(attempt.id) filter (where attempt.status = 'AUTO_SUBMITTED') as auto_submitted,
				       count(attempt.id) filter (
				           where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED') and attempt.passed = true
				       ) as passed,
				       count(attempt.id) filter (
				           where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED') and attempt.passed = false
				       ) as failed,
				       coalesce(round(avg(attempt.percentage) filter (
				           where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED')
				       ), 2), 0)::numeric as average_percentage,
				       coalesce(min(attempt.percentage) filter (
				           where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED')
				       ), 0)::numeric as minimum_percentage,
				       coalesce(max(attempt.percentage) filter (
				           where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED')
				       ), 0)::numeric as maximum_percentage,
				       case
				           when count(attempt.id) filter (
				               where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED')
				           ) = 0 then 0::numeric
				           else round(
				               100.0 * count(attempt.id) filter (
				                   where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED')
				                     and attempt.passed = true
				               ) / count(attempt.id) filter (
				                   where attempt.status in ('SUBMITTED', 'AUTO_SUBMITTED')
				               ),
				               2
				           )
				       end::numeric as pass_rate,
				       coalesce((
				           select sum(rule.question_count * rule.marks_per_question)
				           from exam_pool_rules rule
				           where rule.exam_id = exam.id
				       ), 0)::numeric as total_obtainable_marks
				from exams exam
				left join exam_candidates assignment on assignment.exam_id = exam.id
				left join exam_attempts attempt
				  on attempt.exam_id = assignment.exam_id
				 and attempt.candidate_id = assignment.user_id
				where exam.id = :examId
				and exam.institution_id = :institutionId
				group by exam.id, exam.code, exam.title
				""";
		try {
			return jdbc.queryForObject(
					sql,
					Map.of("examId", examId, "institutionId", institutionId),
					ResultService::mapSummary
			);
		}
		catch (EmptyResultDataAccessException exception) {
			throw new NoSuchElementException("Exam not found");
		}
	}

	@Transactional(readOnly = true)
	public ResultPageResponse<CandidateResultResponse> candidates(
			ResultActor actor,
			UUID examId,
			String search,
			CandidateResultStatus status,
			Boolean passed,
			int page,
			int size
	) {
		UUID institutionId = requireStaff(actor);
		validatePage(page, size);
		CandidateQuery query = candidateQuery(
				examId,
				institutionId,
				search,
				status,
				passed
		);
		ensureExamExists(examId, institutionId);
		long total = jdbc.queryForObject(
				"select count(*) " + CANDIDATE_FROM + query.whereClause(),
				query.parameters(),
				Long.class
		);
		MapSqlParameterSource pageParameters = copy(query.parameters())
				.addValue("limit", size)
				.addValue("offset", (long) page * size);
		List<CandidateResultResponse> content = jdbc.query(
				candidateSelect() + CANDIDATE_FROM + query.whereClause()
						+ candidateOrder() + " limit :limit offset :offset",
				pageParameters,
				ResultService::mapCandidate
		);
		int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
		return new ResultPageResponse<>(content, page, size, total, totalPages);
	}

	@Transactional(readOnly = true)
	public StaffAttemptResultResponse attempt(ResultActor actor, UUID attemptId) {
		UUID institutionId = requireStaff(actor);
		String sql = """
				select attempt.id as attempt_id,
				       exam.id as exam_id,
				       exam.code as exam_code,
				       exam.title as exam_title,
				       candidate.id as candidate_id,
				       candidate.first_name,
				       candidate.last_name,
				       candidate.email,
				       candidate.registration_number,
				       attempt.status,
				       attempt.score,
				       coalesce(attempt.maximum_score, (
				           select coalesce(sum(rule.question_count * rule.marks_per_question), 0)
				           from exam_pool_rules rule
				           where rule.exam_id = exam.id
				       ))::numeric as maximum_score,
				       attempt.percentage,
				       attempt.passed,
				       attempt.started_at,
				       attempt.submitted_at
				from exam_attempts attempt
				join exams exam on exam.id = attempt.exam_id
				join users candidate on candidate.id = attempt.candidate_id
				where attempt.id = :attemptId
				and attempt.institution_id = :institutionId
				""";
		AttemptHeader header;
		try {
			header = jdbc.queryForObject(
					sql,
					Map.of("attemptId", attemptId, "institutionId", institutionId),
					ResultService::mapAttemptHeader
			);
		}
		catch (EmptyResultDataAccessException exception) {
			throw new NoSuchElementException("Attempt not found");
		}
		boolean reviewAvailable = header.status() == CandidateResultStatus.SUBMITTED
				|| header.status() == CandidateResultStatus.AUTO_SUBMITTED;
		List<StaffQuestionReviewResponse> questions = reviewAvailable
				? loadQuestionReview(attemptId)
				: List.of();
		return new StaffAttemptResultResponse(
				header.attemptId(),
				header.examId(),
				header.examCode(),
				header.examTitle(),
				header.candidateId(),
				header.firstName(),
				header.lastName(),
				header.email(),
				header.registrationNumber(),
				header.status(),
				header.score(),
				header.maximumScore(),
				header.percentage(),
				header.passed(),
				header.startedAt(),
				header.submittedAt(),
				reviewAvailable,
				questions
		);
	}

	@Transactional(readOnly = true)
	public ResultCsvExport exportCsv(
			ResultActor actor,
			UUID examId,
			String search,
			CandidateResultStatus status,
			Boolean passed
	) {
		UUID institutionId = requireStaff(actor);
		ExamName exam = requireExam(examId, institutionId);
		CandidateQuery query = candidateQuery(
				examId,
				institutionId,
				search,
				status,
				passed
		);
		List<CandidateResultResponse> candidates = jdbc.query(
				candidateSelect() + CANDIDATE_FROM + query.whereClause() + candidateOrder(),
				query.parameters(),
				ResultService::mapCandidate
		);
		StringBuilder csv = new StringBuilder();
		csv.append("Registration Number,First Name,Last Name,Email,Status,Score,Maximum Score,Percentage,Passed,Started At,Submitted At\r\n");
		for (CandidateResultResponse candidate : candidates) {
			appendCsvRow(csv, java.util.Arrays.asList(
					candidate.registrationNumber(),
					candidate.firstName(),
					candidate.lastName(),
					candidate.email(),
					candidate.attemptStatus(),
					candidate.score(),
					candidate.maximumScore(),
					candidate.percentage(),
					candidate.passed(),
					candidate.startedAt(),
					candidate.submittedAt()
			));
		}
		return new ResultCsvExport(
				"exam-" + exam.code().toLowerCase(Locale.ROOT) + "-results.csv",
				csv.toString().getBytes(StandardCharsets.UTF_8)
		);
	}

	private List<StaffQuestionReviewResponse> loadQuestionReview(UUID attemptId) {
		String sql = """
				select question.id as question_id,
				       question.position,
				       question.question_text,
				       question.question_type,
				       question.marks,
				       option.id as option_id,
				       option.option_text,
				       option.display_order,
				       option.correct,
				       (selection.attempt_option_id is not null) as selected
				from attempt_questions question
				join attempt_options option on option.attempt_question_id = question.id
				left join attempt_answers answer
				  on answer.attempt_id = question.attempt_id
				 and answer.attempt_question_id = question.id
				left join attempt_answer_selections selection
				  on selection.attempt_answer_id = answer.id
				 and selection.attempt_option_id = option.id
				where question.attempt_id = :attemptId
				order by question.position, option.display_order
				""";
		Map<UUID, QuestionReviewBuilder> questions = new LinkedHashMap<>();
		jdbc.query(sql, Map.of("attemptId", attemptId), resultSet -> {
			UUID questionId = resultSet.getObject("question_id", UUID.class);
			QuestionReviewBuilder question = questions.get(questionId);
			if (question == null) {
				question = new QuestionReviewBuilder(
						questionId,
						resultSet.getInt("position"),
						resultSet.getString("question_text"),
						QuestionType.valueOf(resultSet.getString("question_type")),
						resultSet.getBigDecimal("marks")
				);
				questions.put(questionId, question);
			}
			question.addOption(
					resultSet.getObject("option_id", UUID.class),
					resultSet.getString("option_text"),
					resultSet.getInt("display_order"),
					resultSet.getBoolean("selected"),
					resultSet.getBoolean("correct")
			);
		});
		return questions.values().stream().map(QuestionReviewBuilder::build).toList();
	}

	private CandidateQuery candidateQuery(
			UUID examId,
			UUID institutionId,
			String search,
			CandidateResultStatus status,
			Boolean passed
	) {
		StringBuilder where = new StringBuilder(" where exam.id = :examId and exam.institution_id = :institutionId ");
		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("examId", examId)
				.addValue("institutionId", institutionId);
		String normalizedSearch = search == null || search.isBlank()
				? null
				: search.trim().toLowerCase(Locale.ROOT);
		if (normalizedSearch != null) {
			where.append("""
					and (
					    lower(candidate.first_name) like :search
					 or lower(candidate.last_name) like :search
					 or lower(candidate.first_name || ' ' || candidate.last_name) like :search
					 or lower(candidate.email) like :search
					 or lower(coalesce(candidate.registration_number, '')) like :search
					)
					""");
			parameters.addValue("search", "%" + normalizedSearch + "%");
		}
		if (status == CandidateResultStatus.NOT_STARTED) {
			where.append(" and attempt.id is null ");
		}
		else if (status != null) {
			where.append(" and attempt.status = :status ");
			parameters.addValue("status", status.name());
		}
		if (passed != null) {
			where.append(" and attempt.passed = :passed ");
			parameters.addValue("passed", passed);
		}
		return new CandidateQuery(where.toString(), parameters);
	}

	private static String candidateSelect() {
		return """
				select candidate.id as candidate_id,
				       candidate.first_name,
				       candidate.last_name,
				       candidate.email,
				       candidate.registration_number,
				       attempt.id as attempt_id,
				       coalesce(attempt.status, 'NOT_STARTED') as result_status,
				       attempt.score,
				       coalesce(attempt.maximum_score, (
				           select coalesce(sum(rule.question_count * rule.marks_per_question), 0)
				           from exam_pool_rules rule
				           where rule.exam_id = exam.id
				       ))::numeric as maximum_score,
				       attempt.percentage,
				       attempt.passed,
				       attempt.started_at,
				       attempt.submitted_at
				""";
	}

	private static String candidateOrder() {
		return """
				 order by coalesce(candidate.registration_number, ''),
				          lower(candidate.last_name),
				          lower(candidate.first_name),
				          assignment.id
				""";
	}

	private void ensureExamExists(UUID examId, UUID institutionId) {
		requireExam(examId, institutionId);
	}

	private ExamName requireExam(UUID examId, UUID institutionId) {
		try {
			return jdbc.queryForObject(
					"select code, title from exams where id = :examId and institution_id = :institutionId",
					Map.of("examId", examId, "institutionId", institutionId),
					(resultSet, rowNumber) -> new ExamName(
							resultSet.getString("code"),
							resultSet.getString("title")
					)
			);
		}
		catch (EmptyResultDataAccessException exception) {
			throw new NoSuchElementException("Exam not found");
		}
	}

	private static UUID requireStaff(ResultActor actor) {
		if (actor == null || !actor.canReadStaffResults()) {
			throw new AccessDeniedException("Institution staff access is required");
		}
		return actor.institutionId();
	}

	private static void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Result pagination is invalid");
		}
	}

	private static ExamResultSummaryResponse mapSummary(ResultSet resultSet, int rowNumber)
			throws SQLException {
		return new ExamResultSummaryResponse(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("code"),
				resultSet.getString("title"),
				resultSet.getLong("assigned_candidates"),
				resultSet.getLong("not_started"),
				resultSet.getLong("in_progress"),
				resultSet.getLong("submitted"),
				resultSet.getLong("auto_submitted"),
				resultSet.getLong("passed"),
				resultSet.getLong("failed"),
				resultSet.getBigDecimal("average_percentage"),
				resultSet.getBigDecimal("minimum_percentage"),
				resultSet.getBigDecimal("maximum_percentage"),
				resultSet.getBigDecimal("pass_rate"),
				resultSet.getBigDecimal("total_obtainable_marks")
		);
	}

	private static CandidateResultResponse mapCandidate(ResultSet resultSet, int rowNumber)
			throws SQLException {
		return new CandidateResultResponse(
				resultSet.getObject("candidate_id", UUID.class),
				resultSet.getString("first_name"),
				resultSet.getString("last_name"),
				resultSet.getString("email"),
				resultSet.getString("registration_number"),
				resultSet.getObject("attempt_id", UUID.class),
				CandidateResultStatus.valueOf(resultSet.getString("result_status")),
				resultSet.getBigDecimal("score"),
				resultSet.getBigDecimal("maximum_score"),
				resultSet.getBigDecimal("percentage"),
				(Boolean) resultSet.getObject("passed"),
				instant(resultSet, "started_at"),
				instant(resultSet, "submitted_at")
		);
	}

	private static AttemptHeader mapAttemptHeader(ResultSet resultSet, int rowNumber)
			throws SQLException {
		return new AttemptHeader(
				resultSet.getObject("attempt_id", UUID.class),
				resultSet.getObject("exam_id", UUID.class),
				resultSet.getString("exam_code"),
				resultSet.getString("exam_title"),
				resultSet.getObject("candidate_id", UUID.class),
				resultSet.getString("first_name"),
				resultSet.getString("last_name"),
				resultSet.getString("email"),
				resultSet.getString("registration_number"),
				CandidateResultStatus.valueOf(resultSet.getString("status")),
				resultSet.getBigDecimal("score"),
				resultSet.getBigDecimal("maximum_score"),
				resultSet.getBigDecimal("percentage"),
				(Boolean) resultSet.getObject("passed"),
				instant(resultSet, "started_at"),
				instant(resultSet, "submitted_at")
		);
	}

	private static Instant instant(ResultSet resultSet, String column) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp(column);
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static MapSqlParameterSource copy(MapSqlParameterSource source) {
		MapSqlParameterSource copy = new MapSqlParameterSource();
		for (String name : source.getParameterNames()) {
			copy.addValue(name, source.getValue(name));
		}
		return copy;
	}

	private static void appendCsvRow(StringBuilder csv, List<?> cells) {
		for (int index = 0; index < cells.size(); index++) {
			if (index > 0) {
				csv.append(',');
			}
			csv.append(csvCell(cells.get(index)));
		}
		csv.append("\r\n");
	}

	static String csvCell(Object value) {
		if (value == null) {
			return "";
		}
		String text = value.toString();
		String leadingTrimmed = text.stripLeading();
		if (!leadingTrimmed.isEmpty()
				&& (leadingTrimmed.charAt(0) == '='
				|| leadingTrimmed.charAt(0) == '+'
				|| leadingTrimmed.charAt(0) == '-'
				|| leadingTrimmed.charAt(0) == '@')) {
			text = "'" + text;
		}
		boolean quote = text.indexOf(',') >= 0
				|| text.indexOf('"') >= 0
				|| text.indexOf('\r') >= 0
				|| text.indexOf('\n') >= 0;
		String escaped = text.replace("\"", "\"\"");
		return quote ? "\"" + escaped + "\"" : escaped;
	}

	private record CandidateQuery(String whereClause, MapSqlParameterSource parameters) {
	}

	private record ExamName(String code, String title) {
	}

	private record AttemptHeader(
			UUID attemptId,
			UUID examId,
			String examCode,
			String examTitle,
			UUID candidateId,
			String firstName,
			String lastName,
			String email,
			String registrationNumber,
			CandidateResultStatus status,
			BigDecimal score,
			BigDecimal maximumScore,
			BigDecimal percentage,
			Boolean passed,
			Instant startedAt,
			Instant submittedAt
	) {
	}

	private static final class QuestionReviewBuilder {
		private final UUID id;
		private final int position;
		private final String text;
		private final QuestionType type;
		private final BigDecimal marks;
		private final List<StaffOptionReviewResponse> options = new ArrayList<>();
		private final Set<UUID> selected = new LinkedHashSet<>();
		private final Set<UUID> correct = new LinkedHashSet<>();

		private QuestionReviewBuilder(
				UUID id,
				int position,
				String text,
				QuestionType type,
				BigDecimal marks
		) {
			this.id = id;
			this.position = position;
			this.text = text;
			this.type = type;
			this.marks = marks;
		}

		private void addOption(
				UUID optionId,
				String optionText,
				int displayOrder,
				boolean isSelected,
				boolean isCorrect
		) {
			options.add(new StaffOptionReviewResponse(
					optionId,
					optionText,
					displayOrder,
					isSelected,
					isCorrect
			));
			if (isSelected) {
				selected.add(optionId);
			}
			if (isCorrect) {
				correct.add(optionId);
			}
		}

		private StaffQuestionReviewResponse build() {
			boolean exact = selected.equals(correct);
			if (type != QuestionType.MULTIPLE_CHOICE) {
				exact = exact && selected.size() == 1;
			}
			return new StaffQuestionReviewResponse(
					id,
					position,
					text,
					type,
					marks,
					exact ? marks : BigDecimal.ZERO,
					List.copyOf(options)
			);
		}
	}
}
