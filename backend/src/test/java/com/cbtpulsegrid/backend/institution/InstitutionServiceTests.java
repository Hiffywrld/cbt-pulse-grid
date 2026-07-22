package com.cbtpulsegrid.backend.institution;

import java.util.NoSuchElementException;
import java.util.UUID;

import com.cbtpulsegrid.backend.institution.api.CreateInstitutionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTests {

	@Mock
	private InstitutionRepository institutionRepository;

	@InjectMocks
	private InstitutionService institutionService;

	@Test
	void createsAnActiveInstitutionWithNormalizedCode() {
		when(institutionRepository.existsByCodeIgnoreCase("CBT-01")).thenReturn(false);
		when(institutionRepository.saveAndFlush(any(Institution.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = institutionService.create(
				new CreateInstitutionRequest("  Central College  ", "  cbt-01  ")
		);

		ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
		verify(institutionRepository).saveAndFlush(captor.capture());
		assertEquals("Central College", response.name());
		assertEquals("CBT-01", response.code());
		assertEquals(InstitutionStatus.ACTIVE, response.status());
		assertEquals("CBT-01", captor.getValue().getCode());
	}

	@Test
	void rejectsDuplicateInstitutionCode() {
		when(institutionRepository.existsByCodeIgnoreCase("CBT-01")).thenReturn(true);

		assertThrows(
				DuplicateKeyException.class,
				() -> institutionService.create(new CreateInstitutionRequest("Central College", "cbt-01"))
		);
		verify(institutionRepository, never()).saveAndFlush(any(Institution.class));
	}

	@Test
	void reportsMissingInstitution() {
		UUID id = UUID.randomUUID();
		when(institutionRepository.findById(id)).thenReturn(java.util.Optional.empty());

		assertThrows(NoSuchElementException.class, () -> institutionService.get(id));
	}

	@Test
	void rejectsPageSizesAboveOneHundred() {
		assertThrows(
				IllegalArgumentException.class,
				() -> institutionService.list(null, null, 0, 101)
		);
	}
}
