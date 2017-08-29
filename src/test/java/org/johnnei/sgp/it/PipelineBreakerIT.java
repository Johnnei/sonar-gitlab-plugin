package org.johnnei.sgp.it;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import org.johnnei.sgp.internal.gitlab.api.v4.model.GitlabCommitStatus;
import org.johnnei.sgp.it.framework.IntegrationTest;
import org.johnnei.sgp.it.framework.sonarqube.SonarQubeSupport;
import org.johnnei.sgp.sonar.GitLabPlugin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PipelineBreakerIT extends IntegrationTest {

	@Test
	public void testDisabledBreakFeature() throws Exception {
		prepareFeatureBranch();
		String commit = accessGit().commitAll();

		Map<String, String> settings = SonarQubeSupport.createDefaultSettings();
		settings.put(GitLabPlugin.GITLAB_BREAK_PIPELINE, "false");

		accessSonarQube().runAnalysis(commit, settings);

		GitlabCommitStatus status = accessGitlab().getCommitStatus(commit);
		assertThat("Status should not have been created", status, equalTo(null));
	}

	@Test
	public void testPostSuccess() throws Exception {
		prepareFeatureBranch();
		accessGit().add("src/main/java/org/johnnei/sgp/it/internal/NoIssue.java pom.xml");
		String commit = accessGit().commit();

		// Commit everything and switch to the previous commit to ensure a clean state with no issues in the files.
		accessGit().commitAll();
		accessGit().checkout(commit);

		accessSonarQube().runAnalysis(commit);

		GitlabCommitStatus status = accessGitlab().getCommitStatus(commit);
		assertThat("Status should have been created", status, CoreMatchers.notNullValue());
		assertThat("Status should have been success as there are no issues", status.getStatus(), equalTo("success"));
	}

	@Test
	public void testPostFailure() throws Exception {
		prepareFeatureBranch();
		String commit = accessGit().commitAll();

		accessSonarQube().runAnalysis(commit);

		GitlabCommitStatus status = accessGitlab().getCommitStatus(commit);
		assertThat("Status should have been created", status, CoreMatchers.notNullValue());
		assertThat("Status should have been failed as there are blocker issues", status.getStatus(), equalTo("failed"));
	}
}
