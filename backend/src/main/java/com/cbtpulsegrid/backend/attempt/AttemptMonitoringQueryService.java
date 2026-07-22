package com.cbtpulsegrid.backend.attempt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.AttemptStudentQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AttemptMonitoringQueryService implements AttemptMonitoringQuery {

	private final ExamAttemptRepository attemptRepository;
	private final AttemptStudentQuery studentQuery;

	AttemptMonitoringQueryService(
			ExamAttemptRepository attemptRepository,
			AttemptStudentQuery studentQuery
	) {
		this.attemptRepository = attemptRepository;
		this.studentQuery = studentQuery;
	}

	@Override
	@Transactional
	public AttemptView requireOwnedActiveAttempt(
			UUID institutionId,
			UUID candidateId,
			UUID attemptId
	) {
		studentQuery.requireActiveStudent(institutionId, candidateId);
		ExamAttempt attempt = requireForUpdate(attemptId);
		requireOwnership(attempt, institutionId, candidateId);
		requireInProgress(attempt);
		return toView(attempt);
	}

	@Override
	@Transactional
	public AttemptView requireOwnedActiveAttemptAndDevice(
			UUID institutionId,
			UUID candidateId,
			UUID attemptId,
			String deviceId
	) {
		studentQuery.requireActiveStudent(institutionId, candidateId);
		ExamAttempt attempt = requireForUpdate(attemptId);
		requireOwnership(attempt, institutionId, candidateId);
		requireInProgress(attempt);
		if (deviceId == null || !sameHash(attempt.getDeviceIdHash(), sha256(deviceId))) {
			throw new AttemptConflictException("Attempt is locked to another device");
		}
		return toView(attempt);
	}

	@Override
	@Transactional(readOnly = true)
	public AttemptView requireAttempt(UUID institutionId, UUID attemptId) {
		ExamAttempt attempt = attemptRepository.findById(attemptId)
				.orElseThrow(() -> new NoSuchElementException("Attempt not found"));
		if (!institutionId.equals(attempt.getInstitutionId())) {
			throw new AccessDeniedException("Cross-institution attempt access is denied");
		}
		return toView(attempt);
	}

	@Override
	@Transactional(readOnly = true)
	public AttemptPage findAttemptsByExam(
			UUID institutionId,
			UUID examId,
			int page,
			int size
	) {
		Page<ExamAttempt> attempts = attemptRepository.findByInstitutionIdAndExamId(
				institutionId,
				examId,
				PageRequest.of(
						page,
						size,
						Sort.by(Sort.Order.asc("startedAt"), Sort.Order.asc("id"))
				)
		);
		return new AttemptPage(
				attempts.getContent().stream().map(AttemptMonitoringQueryService::toView).toList(),
				attempts.getNumber(),
				attempts.getSize(),
				attempts.getTotalElements(),
				attempts.getTotalPages(),
				attempts.isFirst(),
				attempts.isLast()
		);
	}

	private ExamAttempt requireForUpdate(UUID attemptId) {
		return attemptRepository.findByIdForUpdate(attemptId)
				.orElseThrow(() -> new NoSuchElementException("Attempt not found"));
	}

	private static void requireOwnership(
			ExamAttempt attempt,
			UUID institutionId,
			UUID candidateId
	) {
		if (!institutionId.equals(attempt.getInstitutionId())
				|| !candidateId.equals(attempt.getCandidateId())) {
			throw new AccessDeniedException("Attempt access is denied");
		}
	}

	private static void requireInProgress(ExamAttempt attempt) {
		if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
			throw new AttemptConflictException("Monitoring is closed for this attempt");
		}
	}

	private static AttemptView toView(ExamAttempt attempt) {
		return new AttemptView(
				attempt.getId(),
				attempt.getInstitutionId(),
				attempt.getExamId(),
				attempt.getCandidateId(),
				attempt.getStatus()
		);
	}

	private static boolean sameHash(String first, String second) {
		return MessageDigest.isEqual(
				first.getBytes(StandardCharsets.US_ASCII),
				second.getBytes(StandardCharsets.US_ASCII)
		);
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
