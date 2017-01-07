package org.johnnei.sgp.it;

import java.io.IOException;
import java.util.List;

import org.gitlab.api.models.CommitComment;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;

import org.johnnei.sgp.it.framework.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;

public class DisableIT extends IntegrationTest {

	@Test
	public void testDisableWhenMissingCommitHash() throws IOException {
		String commitHash = gitCommitAll();
		sonarAnalysis(null);

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);

		assertThat("GitLab integration was disabled. Should not have added any comments.", commitComments, IsEmptyCollection.empty());
	}
}
