package com.hz.mymoney.data.utilities;

import com.hz.mymoney.exceptions.FileSaveFailedException;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Log4j2
public final class FileSupport {
	private FileSupport() {}

	private static void renameFile(String source, String target, String contentType) {
		try {
			log.info("Renaming {} from temporary file {} to {}", contentType, source, target);
			Files.move(Path.of(source), Path.of(target), ATOMIC_MOVE, REPLACE_EXISTING);
			log.info("{} saved successfully", contentType);
		} catch (IOException e) {
			log.error("Failed to rename {} to {}", source, target, e);
			throw new FileSaveFailedException("Failed to Rename " + source + " to " + target + " exception is :" + e.getMessage(), e);
		}
	}

	public static void writeBuffer(Writer writer, List<?> contents) throws IOException {
		try (var out = new BufferedWriter(writer)) {
			for (Object entry : contents) {
				out.write(entry.toString());
			}
		}
	}

	public static void writeTempFile(String target, String contentType, List<?> contents) {
		Path path = Path.of(target + ".tmp");
		try {
			log.info("Writing {} to temporary file {}", contentType, path);
			writeBuffer(new FileWriter(path.toFile()), contents);
		} catch (IOException e) {
			log.error("Failed to Save {}", path, e);
			throw new FileSaveFailedException("Failed to Save " + contentType + " exception is :" + e.getMessage(), e);
		}
	}

	public static void renameTempFile(String target, String contentType) {
		renameFile(target + ".tmp", target, contentType);
	}

	public static void writeFile(String target, String contentType, List<?> contents) {
		writeTempFile(target, contentType, contents);
		renameTempFile(target, contentType);
	}
}
