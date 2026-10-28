package com.hz.mymoney.data.utilities;

import com.hz.mymoney.exceptions.FileSaveFailedException;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Log4j2
public class FileSupport {
	private FileSupport() {}

	public static void writeTempFile(String target, String contentType, List<?> contents) {
		Path path = Path.of(target + ".tmp");
		log.info("Saving {} to temporary file {}", contentType, path);
		try (var out = new BufferedWriter(new FileWriter(path.toFile()))) {
			for (Object entry : contents) {
				out.write(entry.toString());
			}
		} catch (IOException e) {
			log.error("Failed to Save {}", path, e);
			throw new FileSaveFailedException("Failed to Save " + contentType + " exception is :" + e.getMessage(), e);
		}
	}

	public static void renameTempFile(String target, String contentType) {
		renameFile(target + ".tmp", target, contentType);
	}

	public static void renameFile(String source, String target, String contentType) {
		try {
			Files.move(Path.of(source), Path.of(target), ATOMIC_MOVE, REPLACE_EXISTING);
			log.info("{} saved successfully", contentType);
		} catch (IOException e) {
			log.error("Failed to rename {} to {}", source, target, e);
			throw new FileSaveFailedException("Failed to Rename " + source + " to " + target + " exception is :" + e.getMessage(), e);
		}
	}
}
