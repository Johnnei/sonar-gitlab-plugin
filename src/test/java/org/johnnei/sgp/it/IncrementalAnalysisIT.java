package org.johnnei.sgp.it;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import org.johnnei.sgp.it.framework.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the no duplicate comments are created when a single project is analysed twice.
 */
public class IncrementalAnalysisIT extends IntegrationTest {

	/**
	 * Tests that when doing two analysis on the same commit that there are no duplicate comments in GitLab.
	 */
	@Test
	public void testDoubleAnalysis() throws Exception {
		prepareFeatureBranch();
		String commitHash = accessGit().commitAll();
		accessSonarQube().runAnalysis(commitHash);
		accessSonarQube().runAnalysis(commitHash);

		List<String> comments = accessGitlab().getCommitComments(commitHash);
		List<String> summary = accessGitlab().getCommitSummary(commitHash);
		List<String> messages = Files.readAllLines(getTestResource("sonarqube/issues.txt"));

		for (String message : messages) {
			assertThat(comments, IsCollectionContaining.hasItem(equalTo(message)));
			removeMatchedComment(comments, message);
		}

		assertThat(String.format("%s Issues have been reported and thus comments should be there.", messages.size()), comments, IsEmptyCollection.empty());
		assertThat("Only 1 summary comment should be created.", summary, IsCollectionWithSize.hasSize(1));
	}

	@Test
	public void testIncrementalAnalysis() throws Exception {
		prepareFeatureBranch();
		accessGit().add("src/main/java/org/johnnei/sgp/it/api/sources/Main.java");
		accessGit().add("pom.xml");
		String commitHash = accessGit().commit("Initial commit.");
		String secondCommitHash = accessGit().commitAll();

		accessGit().checkout(commitHash);
		accessSonarQube().runAnalysis(commitHash);

		accessGit().checkout(secondCommitHash);
		accessSonarQube().runAnalysis(secondCommitHash);

		assertComments(commitHash, "sonarqube/incremental-1.txt");
		assertComments(secondCommitHash, "sonarqube/incremental-2.txt");
	}

	private void assertComments(String commitHash, String issueFile) throws IOException {
		int commentCount = accessGitlab().getAllCommitComments(commitHash).size();
		List<String> comments = accessGitlab().getCommitComments(commitHash);

		List<String> messages = Files.readAllLines(getTestResource(issueFile));

		for (String message : messages) {
			assertThat(comments, IsCollectionContaining.hasItem(equalTo(message)));
			removeMatchedComment(comments, message);
		}

		assertThat(
			String.format(
				"%s Issues have been reported. However %s comments have been created. The following were unexpected:",
				messages.size(),
				commentCount
			),
			comments,
			IsEmptyCollection.empty()
		);
	}
}
