package com.cbtpulsegrid.backend.questionbank;

import java.util.UUID;

import com.cbtpulsegrid.backend.questionbank.api.QuestionBankActor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class QuestionBankAuthorization {

	public UUID requireSubjectReadAccess(QuestionBankActor actor) {
		UUID institutionId = requireInstitutionalActor(actor);
		if (!actor.isInstitutionAdmin() && !actor.isExaminer()) {
			throw new AccessDeniedException("Subject access is not permitted");
		}
		return institutionId;
	}

	public UUID requireSubjectManagementAccess(QuestionBankActor actor) {
		UUID institutionId = requireInstitutionalActor(actor);
		if (!actor.isInstitutionAdmin()) {
			throw new AccessDeniedException("Subject management is not permitted");
		}
		return institutionId;
	}

	public UUID requireQuestionManagementAccess(QuestionBankActor actor) {
		UUID institutionId = requireInstitutionalActor(actor);
		if (!actor.isInstitutionAdmin() && !actor.isExaminer()) {
			throw new AccessDeniedException("Question-bank access is not permitted");
		}
		return institutionId;
	}

	public void requireTenant(UUID expectedInstitutionId, UUID actualInstitutionId) {
		if (!expectedInstitutionId.equals(actualInstitutionId)) {
			throw new AccessDeniedException("Cross-institution access is denied");
		}
	}

	private static UUID requireInstitutionalActor(QuestionBankActor actor) {
		if (actor == null || actor.userId() == null || actor.institutionId() == null || actor.isSuperAdmin()) {
			throw new AccessDeniedException("Institutional question-bank access is required");
		}
		return actor.institutionId();
	}
}
