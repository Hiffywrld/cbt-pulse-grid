package com.cbtpulsegrid.backend.audit;

import com.cbtpulsegrid.backend.IdentityServiceApplication;
import com.cbtpulsegrid.backend.audit.api.AuditController;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IdentityServiceApplication.class)
class AuditControllerRegistrationTests {

	@Autowired
	private AuditController auditController;

	@Test
	void identityServiceRegistersAuditController() {
		assertThat(auditController).isNotNull();
	}
}
