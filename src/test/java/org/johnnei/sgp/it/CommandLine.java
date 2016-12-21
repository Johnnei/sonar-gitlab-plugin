package org.johnnei.sgp.it;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Johnnei on 2016-12-20.
 */
public class CommandLine {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);

	private final String shell;

	private final String commandArgument;

	private final File workingDirectory;

	public CommandLine(String shell, String commandArgument, File workingDirectory) {
		this.shell = shell;
		this.commandArgument = commandArgument;
		this.workingDirectory = workingDirectory;
	}

	private Process start(String command, boolean captureOutput) throws IOException {
		LOGGER.debug("Running: " + shell + " " + commandArgument + " " + command);

		ProcessBuilder builder = new ProcessBuilder()
			.directory(workingDirectory)
			.command(shell, commandArgument, command);

		if (!captureOutput) {
			builder.inheritIO();
		}

		return builder.start();
	}

	private void awaitExit(Process process) throws IOException {
		try {
			int returnCode = process.waitFor();
			if (returnCode != 0) {
				throw new RuntimeException("Process failed: " + returnCode);
			}
		} catch (InterruptedException e) {
			process.destroy();
		}
	}

	public void startAndAwait(String command) throws IOException {
		awaitExit(start(command, false));
	}

	public String startAndAwaitOutput(String command) throws IOException {
		Process process = start(command, true);

		StringBuilder builder = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			awaitExit(process);

			String line;
			boolean hasContent = false;
			while ((line = reader.readLine()) != null) {
				if (hasContent) {
					builder.append("\n");
				}

				LOGGER.debug("Read input line: {}", line);
				builder.append(line);
				hasContent = true;
			}
		}

		return builder.toString();
	}
}
