package org.johnnei.sgp.it.framework.git;

import java.io.IOException;

import org.johnnei.sgp.it.framework.CommandLine;

public class GitSupport {

	private final CommandLine commandLine;

	private String branch;

	public GitSupport(CommandLine commandLine) {
		this.commandLine = commandLine;
		this.branch = "master";
	}

	public void createBranch(String branch) throws IOException {
		this.branch = branch;
		commandLine.startAndAwait(String.format("git checkout -b %s", branch));
	}

	public void checkoutBranch(String branch) throws IOException {
		this.branch = branch;
		checkout(branch);
	}

	public void checkout(String commitHash) throws IOException {
		commandLine.startAndAwait("git checkout " + commitHash);
	}

	public void add(String paths) throws IOException {
		commandLine.startAndAwait("git add " + paths);
	}

	public String commit() throws IOException {
		return commit("My commit message");
	}

	public String commit(String message) throws IOException {
		commandLine.startAndAwait(String.format("git commit -m \"%s\"", message));
		commandLine.startAndAwait(String.format("git push -u origin %s", branch));
		return getLastCommit();
	}

	public String commitAll() throws IOException {
		add(".");
		return commit();
	}

	private String getLastCommit() throws IOException {
		return commandLine.startAndAwaitOutput("git log -n 1 --format=%H");
	}
}
