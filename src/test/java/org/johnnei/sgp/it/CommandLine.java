package org.johnnei.sgp.it;

import java.io.File;
import java.io.IOException;

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

	public Process start(String command) throws IOException {
		LOGGER.debug("Running: " + shell + " " + commandArgument + " " + command);

		return new ProcessBuilder()
			.directory(workingDirectory)
			.command(shell, commandArgument, command)
			.inheritIO()
			.start();
	}

	public void startAndAwait(String command) throws IOException {
		Process process = start(command);
		try {
			int returnCode = process.waitFor();
			if (returnCode != 0) {
				throw new RuntimeException("Process failed: " + returnCode);
			}
		} catch (InterruptedException e) {
			process.destroy();
		}
	}
}
