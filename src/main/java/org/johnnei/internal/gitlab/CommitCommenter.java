package org.johnnei.internal.gitlab;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.stream.Stream;

import org.gitlab.api.GitlabAPI;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.internal.sonar.SonarReport;

/**
 * Created by Johnnei on 2016-11-12.
 */
public class CommitCommenter {

	private static final Logger LOGGER = Loggers.get(CommitCommenter.class);

	@Nonnull
	private GitlabAPI gitlabApi;

	public CommitCommenter(@Nonnull GitlabAPI gitlabApi) {
		this.gitlabApi = gitlabApi;
	}

	public void process(SonarReport report) {
		boolean allCommentsSucceeded = report.getIssues()
			.flatMap(issue -> mapIssueToFile(report, issue))
			.map(mappedIssue -> postComment(report, mappedIssue))
			// All comments should succeed.
			.reduce((a, b) -> a && b)
			// When there are no comments nothing has failed.
			.orElse(true);

		if (!allCommentsSucceeded) {
			throw new ProcessException("One or more comments failed to be added to the commit.");
		}
	}

	private Stream<MappedIssue> mapIssueToFile(SonarReport report, PostJobIssue issue) {
		String path = report.getFilePath(issue.inputComponent());
		if (path != null) {
			return Stream.of(new MappedIssue(issue, path));
		} else {
			LOGGER.debug("Failed to find file for \"{}\" in \"{}\"", issue.message(), issue.inputComponent());
			return Stream.empty();
		}
	}

	private boolean postComment(SonarReport report, MappedIssue mappedIssue) {
		try {
			gitlabApi.createCommitComment(
				report.getProject().getId(),
				report.getCommitSha(),
				mappedIssue.getIssue().message(),
				mappedIssue.getPath(),
				Integer.toString(mappedIssue.getIssue().line()),
				"new"
			);
			return true;
		} catch (IOException e) {
			LOGGER.warn("Failed to create comment for in {}:{}.", mappedIssue.getPath(), mappedIssue.getIssue().line(), e);
			return false;
		}
	}

	private static class MappedIssue {

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
}
