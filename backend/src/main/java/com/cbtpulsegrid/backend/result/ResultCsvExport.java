package com.cbtpulsegrid.backend.result;

public record ResultCsvExport(String filename, byte[] content) {
	public ResultCsvExport {
		content = content.clone();
	}

	@Override
	public byte[] content() {
		return content.clone();
	}
}
