package com.cbtpulsegrid.backend.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultCsvSecurityTests {

	@Test
	void quotesCommasQuotesAndNewlines() {
		assertEquals("\"Ada, \"\"Ace\"\"\r\nStudent\"", ResultService.csvCell("Ada, \"Ace\"\r\nStudent"));
	}

	@Test
	void neutralizesSpreadsheetFormulaPrefixes() {
		assertEquals("'=2+2", ResultService.csvCell("=2+2"));
		assertEquals("'  @SUM(A1:A2)", ResultService.csvCell("  @SUM(A1:A2)"));
		assertEquals("'+441234", ResultService.csvCell("+441234"));
	}
}
