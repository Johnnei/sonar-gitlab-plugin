package org.johnnei.sgp.internal.gitlab;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CommitComment;
import org.sonar.api.batch.rule.Severity;
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
	 *
	 * @param report The report to comment into GitLab.
	 */
	public void process(SonarReport report) {
		List<CommitComment> existingComments = report.getCommitShas()
			.flatMap(commit -> fetchCommitComments(report, commit).stream())
			.collect(Collectors.toList());

		commentIssuesInline(existingComments, report);
		commentSummary(existingComments, report);
	}

	private List<CommitComment> fetchCommitComments(SonarReport report, String commit) {
		try {
			return gitlabApi.getCommitComments(report.getProject().getId(), commit);
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Failed to fetch existing comments for commit %s.", commit), e);
		}
	}

	/**
	 * Creates a commit comment which contains a summary of all the issues.
	 *
	 * @param existingComments The comments which are already there.
	 * @param report The report to comment into GitLab.
	 */
	private void commentSummary(List<CommitComment> existingComments, SonarReport report) {
		String summary = buildSummary(report);

		boolean hasExistingSummary = existingComments.stream()
			.filter(comment -> comment.getLine() == null)
			.anyMatch(comment -> comment.getNote().equals(summary));

		if (!hasExistingSummary) {
			try {
				gitlabApi.createCommitComment(report.getProject().getId(), report.getBuildCommitSha(), summary, null, null, null);
			} catch (IOException e) {
				throw new ProcessException("Failed to post summary comment.", e);
			}
		}
	}

	private String buildSummary(SonarReport report) {
		List<Severity> severitiesInOrder = Arrays.asList(
			Severity.BLOCKER,
			Severity.CRITICAL,
			Severity.MAJOR,
			Severity.MINOR,
			Severity.INFO
		);

		MarkdownBuilder summary = new MarkdownBuilder();
		summary
			.addText(String.format("SonarQube analysis reported %d issues.", report.getIssueCount()))
			.addLineBreak();

		for (Severity severity : severitiesInOrder) {
			long count = report.countIssuesWithSeverity(severity);
			if (count == 0) {
				continue;
			}

			summary.startListItem()
				.addText(String.format("%d %s", count, severity.name().toLowerCase()))
				.endListItem();

		}

		summary
			.addLineBreak()
			.addText("Watch the comments in this conversation to review them.");
		return summary.toString();
	}

	/**
	 * Creates the inline comments based on the given {@link SonarReport}.
	 *
	 * @param existingComments The comments which are already there.
	 * @param report The report to comment into GitLab.
	 */
	private void commentIssuesInline(List<CommitComment> existingComments, SonarReport report) {
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
		LOGGER.debug("isExisting(issue[path={}, line={}, message={}])", issue.getPath(), issue.getIssue().line(), issue.getIssue().message());
		existingComments.forEach(comment -> LOGGER.debug("comment(path={}, line={}, message={}])", comment.getPath(), comment.getLine(), comment.getNote()));
		return existingComments.stream()
			.filter(comment -> comment.getPath() != null)
			.filter(comment -> Objects.equals(comment.getPath(), issue.getPath()))
			.filter(comment -> Objects.equals(comment.getLine(), Integer.toString(issue.getIssue().line())))
			.anyMatch(comment -> Objects.equals(comment.getNote(), issue.getIssue().message()));
	}

	/**
	 * Creates an inline comment on the commit.
	 *
	 * @param report The Sonar report information.
	 * @param mappedIssue The issue which should be reported.
	 * @return <code>true</code> when the comment was successfully created. Otherwise <code>false</code>.
	 */
	private boolean postComment(SonarReport report, MappedIssue mappedIssue) {

		MarkdownBuilder messageBuilder = new MarkdownBuilder();
		messageBuilder.addSeverityIcon(mappedIssue.getIssue().severity());
		messageBuilder.addText(mappedIssue.getIssue().message());

		try {
			gitlabApi.createCommitComment(
				report.getProject().getId(),
				mappedIssue.getCommitSha(),
				messageBuilder.toString(),
				mappedIssue.getPath(),
				formatLineNumber(mappedIssue),
				"new"
			);
			return true;
		} catch (IOException e) {
			LOGGER.warn("Failed to create comment for in {}:{}.", mappedIssue.getPath(), mappedIssue.getIssue().line(), e);
			return false;
		}
	}

	private String formatLineNumber(MappedIssue mappedIssue) {
		int line;

		if (mappedIssue.getIssue().line() == null) {
			line = mappedIssue.getDiff()
				.getRanges()
				.stream()
				.findAny()
				.orElseThrow(() -> new IllegalStateException(String.format("Diff without ranges for file: %s", mappedIssue.getPath())))
				.getStart();
		} else {
			line = mappedIssue.getIssue().line();
		}

		return Integer.toString(line);
	}
}
