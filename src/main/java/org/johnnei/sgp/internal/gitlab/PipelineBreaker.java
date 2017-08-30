package org.johnnei.sgp.internal.gitlab;

import java.io.IOException;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import org.johnnei.sgp.internal.gitlab.api.v4.GitLabApi;
import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;
import org.johnnei.sgp.internal.sonar.GitLabPluginConfiguration;

import static org.sonar.api.batch.InstantiationStrategy.PER_BATCH;

@BatchSide
@InstantiationStrategy(value = PER_BATCH)
public class PipelineBreaker {

	private final GitLabPluginConfiguration configuration;

	public PipelineBreaker(GitLabPluginConfiguration configuration) {
		this.configuration = configuration;
	}

	public void process(SonarReport report) {
		if (!configuration.isBreakPipelineEnabled()) {
			return;
		}

		GitLabApi gitlabAPI = configuration.createGitLabConnection();

		String status;
		String message;

		if (hasCriticalOrWorseIssue(report)) {
			status = "failed";
			message = "A critical or worse issue has been found.";
		} else {
			status = "success";
			message = "No critical (or worse) issues found.";
		}

		try {
			gitlabAPI.createCommitStatus(configuration.getProject().getId(), report.getBuildCommitSha(), status, "SonarQube", message);
		} catch (IOException e) {
			throw new ProcessException("Failed to set commit status.", e);
		}
	}

	private boolean hasCriticalOrWorseIssue(SonarReport report) {
		return report.getIssues()
			.map(MappedIssue::getIssue)
			.map(PostJobIssue::severity)
			.anyMatch(severity -> severity == Severity.CRITICAL || severity == Severity.BLOCKER);
	}
}
