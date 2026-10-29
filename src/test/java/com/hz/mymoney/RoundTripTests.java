package com.hz.mymoney;

import com.hz.mymoney.data.models.ledger.Ledger;
import com.hz.mymoney.data.utilities.FileSupport;
import com.hz.mymoney.data.utilities.LedgerParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
		String readFileContent = readFileContent(filename);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		Writer out = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

		FileSupport.writeBuffer(out, readFileAsLedger(filename).getEntries());
		String writeFileContent = baos.toString(StandardCharsets.UTF_8);

		assertEquals(readFileContent, writeFileContent);
	}

}
