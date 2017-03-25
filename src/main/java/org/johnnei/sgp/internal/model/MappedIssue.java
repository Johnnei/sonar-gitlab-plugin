package org.johnnei.sgp.internal.model;

import org.sonar.api.batch.postjob.issue.PostJobIssue;

import org.johnnei.sgp.internal.model.diff.UnifiedDiff;

/**
 * Represents an {@link PostJobIssue} and the file within the repository to which it is mapped.
 */
public class MappedIssue {

	private final PostJobIssue issue;

	private final UnifiedDiff diff;

	private final String path;

	public MappedIssue(PostJobIssue issue, UnifiedDiff diff, String path) {
		this.issue = issue;
		this.diff = diff;
		this.path = path;
	}

	public PostJobIssue getIssue() {
		return issue;
	}

	public UnifiedDiff getDiff() {
		return diff;
	}

	public String getCommitSha() {
		return diff.getCommitSha();
	}

	public String getPath() {
		return path;
	}

}
