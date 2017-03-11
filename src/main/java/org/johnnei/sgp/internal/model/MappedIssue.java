package org.johnnei.sgp.internal.model;

import org.sonar.api.batch.postjob.issue.PostJobIssue;

/**
 * Represents an {@link PostJobIssue} and the file within the repository to which it is mapped.
 */
public class MappedIssue {

	private final PostJobIssue issue;

	private final String commitSha;

	private final String path;

	public MappedIssue(PostJobIssue issue, String commitSha, String path) {
		this.issue = issue;
		this.commitSha = commitSha;
		this.path = path;
	}

	public PostJobIssue getIssue() {
		return issue;
	}

	public String getCommitSha() {
		return commitSha;
	}

	public String getPath() {
		return path;
	}

}
