package org.johnnei.sgp.internal.model;

import org.sonar.api.batch.postjob.issue.PostJobIssue;

/**
 * Represents an {@link PostJobIssue} and the file within the repository to which it is mapped.
 */
public class MappedIssue {

	private final PostJobIssue issue;

	private final String path;

	public MappedIssue(PostJobIssue issue, String path) {
		this.issue = issue;
		this.path = path;
	}

	public PostJobIssue getIssue() {
		return issue;
	}

	public String getPath() {
		return path;
	}

}
