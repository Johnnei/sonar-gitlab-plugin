package org.johnnei.sgp.internal.model;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import org.gitlab.api.models.GitlabProject;
import org.sonar.api.batch.rule.Severity;

import org.johnnei.sgp.internal.sorting.IssueSeveritySorter;

/**
 * Created by Johnnei on 2016-11-12.
 */
public class SonarReport {

	@Nonnull
	private final Collection<MappedIssue> issues;

	@Nonnull
	private final GitlabProject project;

	@Nonnull
	private final String commitSha;

	private SonarReport(@Nonnull Builder builder) {
		commitSha = Objects.requireNonNull(builder.commitSha, "Commit hash is required to know which commit to comment on.");
		project = Objects.requireNonNull(builder.project, "Project is required to know where the commit is.");
		issues = Objects.requireNonNull(builder.issues, "Issues are required to be a nonnull collection in order to be able to comment.");
	}

	public Stream<MappedIssue> getIssues() {
		return issues.stream().sorted(new IssueSeveritySorter());
	}

	/**
	 * Counts the amount of issues with the given severity.
	 * @param severity The severity to filter on.
	 * @return The amount of issues with the given severity.
	 */
	public long countIssuesWithSeverity(Severity severity) {
		return issues.stream()
			.filter(mappedIssue -> mappedIssue.getIssue().severity() == severity)
			.count();
	}

	@Nonnull
	public GitlabProject getProject() {
		return project;
	}

	@Nonnull
	public String getCommitSha() {
		return commitSha;
	}

	/**
	 * @return The amount of issues reported by SonarQube.
	 */
	public int getIssueCount() {
		return issues.size();
	}

	public static class Builder {

		private String commitSha;
		private GitlabProject project;
		private Collection<MappedIssue> issues;

		public Builder setCommitSha(String commitSha) {
			this.commitSha = commitSha;
			return this;
		}

		public Builder setProject(GitlabProject project) {
			this.project = project;
			return this;
		}

		public Builder setIssues(Collection<MappedIssue> issues) {
			this.issues = issues;
			return this;
		}

		public SonarReport build() {
			return new SonarReport(this);
		}

	}
}
