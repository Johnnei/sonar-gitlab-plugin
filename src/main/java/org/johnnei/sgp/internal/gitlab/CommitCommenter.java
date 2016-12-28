package org.johnnei.sgp.internal.gitlab;

import javax.annotation.Nonnull;
import java.io.IOException;

import org.gitlab.api.GitlabAPI;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;

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
			.map(mappedIssue -> postComment(report, mappedIssue))
			// All match true
			.allMatch(result -> result);

		if (!allCommentsSucceeded) {
			throw new ProcessException("One or more comments failed to be added to the commit.");
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
}
