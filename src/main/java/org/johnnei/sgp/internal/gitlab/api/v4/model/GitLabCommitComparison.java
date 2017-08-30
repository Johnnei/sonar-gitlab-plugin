package org.johnnei.sgp.internal.gitlab.api.v4.model;

import java.util.Collection;

public class GitLabCommitComparison {

	private Collection<GitLabCommit> commits;

	public Collection<GitLabCommit> getCommits() {
		return commits;
	}
}
