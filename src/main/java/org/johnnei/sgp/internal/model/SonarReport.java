package org.johnnei.sgp.internal.model;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import org.gitlab.api.models.GitlabProject;

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

	@Nonnull
	private File gitBaseDir;

	public SonarReport(@Nonnull Builder builder) {
		gitBaseDir = Objects.requireNonNull(builder.gitBaseDir, "Git sources are required to be able to create inline comments.");
		commitSha = Objects.requireNonNull(builder.commitSha, "Commit hash is required to know which commit to comment on.");
		project = Objects.requireNonNull(builder.project, "Project is required to know where the commit is.");
		issues = Objects.requireNonNull(builder.issues, "Issues are required to be a nonnull collection in order to be able to comment.");
	}

	public Stream<MappedIssue> getIssues() {
		return issues.stream().sorted(new IssueSeveritySorter());
	}

	@Nonnull
	public GitlabProject getProject() {
		return project;
	}

	@Nonnull
	public String getCommitSha() {
		return commitSha;
	}

	public static class Builder {

		private File gitBaseDir;
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

		public Builder setProjectBaseDir(File projectBaseDir) {
			this.gitBaseDir = projectBaseDir;
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
