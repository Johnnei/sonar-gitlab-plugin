package org.johnnei.sgp.internal.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import org.gitlab.api.models.GitlabProject;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.scan.filesystem.PathResolver;

import org.johnnei.sgp.internal.sorting.IssueSeveritySorter;

/**
 * Created by Johnnei on 2016-11-12.
 */
public class SonarReport {

	@Nonnull
	private final Collection<PostJobIssue> issues;

	@Nonnull
	private final GitlabProject project;

	@Nonnull
	private final String commitSha;

	@Nonnull
	private File gitBaseDir;

	@Nonnull
	private PathResolver pathResolver = new PathResolver();

	public SonarReport(@Nonnull Builder builder) {
		gitBaseDir = Objects.requireNonNull(builder.gitBaseDir, "Git sources are required to be able to create inline comments.");
		commitSha = Objects.requireNonNull(builder.commitSha, "Commit hash is required to know which commit to comment on.");
		project = Objects.requireNonNull(builder.project, "Project is required to know where the commit is.");
		issues = Objects.requireNonNull(builder.issues, "Issues are required to be a nonnull collection in order to be able to comment.");
	}

	public Stream<PostJobIssue> getIssues() {
		return issues.stream().sorted(new IssueSeveritySorter());
	}

	@CheckForNull
	public String getFilePath(@CheckForNull InputComponent inputComponent) {
		if (inputComponent == null || !inputComponent.isFile()) {
			return null;
		}

		InputFile inputFile = (InputFile) inputComponent;
		return pathResolver.relativePath(gitBaseDir, inputFile.file());
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
		private Collection<PostJobIssue> issues;

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

		public Builder setIssues(Collection<PostJobIssue> issues) {
			this.issues = issues;
			return this;
		}

		public SonarReport build() {
			return new SonarReport(this);
		}

	}
}
