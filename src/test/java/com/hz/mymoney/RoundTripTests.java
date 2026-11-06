package com.hz.mymoney;

import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.utilities.FileSupport;
import com.hz.mymoney.data.utilities.LedgerParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundTripTests {

	private String readFileContent(String fileName) throws IOException {
		Resource resource = new ClassPathResource(fileName);
		return resource.getContentAsString(StandardCharsets.UTF_8);
	}

	private Ledger readFileAsLedger(String fileName) throws IOException {
		Resource resource = new ClassPathResource(fileName);
		return new LedgerParser().loadLedger(resource.getInputStream());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"test.ledger",
			"test-schedules.ledger"
	})
	void testLedgerFileReadWriteMatches(String filename) throws IOException {
		Path filePath = Files.createTempFile(filename, ".tmp");

		String readFileContent = readFileContent(filename);

		FileSupport.writeBuffer(filePath, readFileAsLedger(filename).getEntries());
		String writeFileContent = Files.readString(filePath);

		assertEquals(readFileContent, writeFileContent);
	}

}
