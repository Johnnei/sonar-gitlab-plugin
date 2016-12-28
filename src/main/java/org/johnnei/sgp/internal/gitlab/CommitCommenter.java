package org.johnnei.sgp.internal.gitlab;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CommitComment;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;

/**
 * Action class which is responsible for updating/creating comments in GitLab.
 */
public class CommitCommenter {

	private static final Logger LOGGER = Loggers.get(CommitCommenter.class);

	@Nonnull
	private GitlabAPI gitlabApi;

	public CommitCommenter(@Nonnull GitlabAPI gitlabApi) {
		this.gitlabApi = gitlabApi;
	}

	/**
	 * Creates new comments in GitLab based on the given {@link SonarReport}.
	 * @param report The report to comment into GitLab.
	 */
	public void process(SonarReport report) {
		List<CommitComment> existingComments;

		try {
			existingComments = gitlabApi.getCommitComments(report.getProject().getId(), report.getCommitSha());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to fetch existing comments.", e);
		}

		boolean allCommentsSucceeded = report.getIssues()
			.filter(issue -> !isExisting(issue, existingComments))
			.map(mappedIssue -> postComment(report, mappedIssue))
			// All match true
			.allMatch(result -> result);

		if (!allCommentsSucceeded) {
			throw new ProcessException("One or more comments failed to be added to the commit.");
		}
	}

	/**
	 * @param issue The issue to check for duplicates.
	 * @param existingComments The comments which are already existing.
	 * @return <code>true</code> when a comment with the same text on the same line has been found.
	 */
	private static boolean isExisting(MappedIssue issue, List<CommitComment> existingComments) {
		return existingComments.stream()
			.filter(comment -> comment.getPath().equals(issue.getPath()))
			.filter(comment -> comment.getLine().equals(Integer.toString(issue.getIssue().line())))
			.anyMatch(comment -> comment.getNote().equals(issue.getIssue().message()));
	}

	/**
	 * Creates an inline comment on the commit.
	 * @param report The Sonar report information.
	 * @param mappedIssue The issue which should be reported.
	 * @return <code>true</code> when the comment was successfully created. Otherwise <code>false</code>.
	 */
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
}
