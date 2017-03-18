package org.johnnei.sgp.internal.sonar;

import java.io.IOException;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Bootstraps the Analysis of a Commit within GitLab.
 */
public class CommitAnalysisBuilder extends ProjectBuilder {

	private static final Logger LOGGER = Loggers.get(CommitAnalysisBuilder.class);

	private final AnalysisMode analysisMode;

	private final GitLabPluginConfiguration configuration;

	public CommitAnalysisBuilder(GitLabPluginConfiguration configuration, AnalysisMode analysisMode) {
		this.configuration = configuration;
		this.analysisMode = analysisMode;
	}

	@Override
	public void build(Context context) {
		if (!configuration.isEnabled()) {
			LOGGER.info("Disabling GitLab integration. No commit information has been supplied.");
			return;
		}

		ensureCorrectConfiguration();
	}

	private void ensureCorrectConfiguration() {
		ensureIssuesMode();
		try {
			configuration.initialiseProject();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to retrieve GitLab project.", e);
		}
	}

	private void ensureIssuesMode() {
		if (!analysisMode.isIssues()) {
			throw new IllegalStateException(String.format("GitLab plugin requires \"%s\" to be set to either %s or %s.",
					CoreProperties.ANALYSIS_MODE,
					CoreProperties.ANALYSIS_MODE_ISSUES,
					CoreProperties.ANALYSIS_MODE_PREVIEW
			));
		}
	}

}
