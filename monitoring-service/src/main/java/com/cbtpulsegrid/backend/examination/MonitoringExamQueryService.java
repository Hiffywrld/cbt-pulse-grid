package com.cbtpulsegrid.backend.examination;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MonitoringExamQueryService implements MonitoringExamQuery {

	private final ExamRepository examRepository;

	MonitoringExamQueryService(ExamRepository examRepository) {
		this.examRepository = examRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public void requireExam(UUID institutionId, UUID examId) {
		Exam exam = examRepository.findById(examId)
				.orElseThrow(() -> new NoSuchElementException("Exam not found"));
		if (!institutionId.equals(exam.getInstitutionId())) {
			throw new AccessDeniedException("Cross-institution exam access is denied");
		}
	}
}
