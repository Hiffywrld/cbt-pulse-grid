package com.cbtpulsegrid.backend.examination;

import java.util.UUID;

import com.cbtpulsegrid.backend.examination.api.ExamActor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
class ExamAuthorization {

	UUID requireManagementAccess(ExamActor actor) {
		if (actor == null || actor.isSuperAdmin() || !actor.canManage()) {
			throw new AccessDeniedException("Exam management access is denied");
		}
		return requireInstitution(actor);
	}

	UUID requireReadAccess(ExamActor actor) {
		if (actor == null || actor.isSuperAdmin() || (!actor.canManage() && !actor.isInvigilator())) {
			throw new AccessDeniedException("Exam read access is denied");
		}
		return requireInstitution(actor);
	}

	void requireTenant(UUID expectedInstitutionId, UUID resourceInstitutionId) {
		if (!expectedInstitutionId.equals(resourceInstitutionId)) {
			throw new AccessDeniedException("Cross-institution exam access is denied");
		}
	}

	void requireReadableStatus(ExamActor actor, Exam exam) {
		requireExamOwnership(actor, exam);
		if (!actor.canManage() && actor.isInvigilator() && exam.getStatus() != ExamStatus.PUBLISHED) {
			throw new AccessDeniedException("Invigilators may read only PUBLISHED exams");
		}
	}

	void requireManagementOwnership(ExamActor actor, Exam exam) {
		requireExamOwnership(actor, exam);
	}

	ExamStatus resolveListStatus(ExamActor actor, ExamStatus requestedStatus) {
		if (actor.canManage()) {
			return requestedStatus;
		}
		if (requestedStatus != null && requestedStatus != ExamStatus.PUBLISHED) {
			throw new AccessDeniedException("Invigilators may list only PUBLISHED exams");
		}
		return ExamStatus.PUBLISHED;
	}

	private static UUID requireInstitution(ExamActor actor) {
		if (actor.institutionId() == null) {
			throw new AccessDeniedException("Institution context is required");
		}
		return actor.institutionId();
	}

	private static void requireExamOwnership(ExamActor actor, Exam exam) {
		if (actor.isExaminer() && !actor.userId().equals(exam.getCreatedBy())) {
			throw new AccessDeniedException("Examiner access is limited to examinations they created");
		}
	}
}
