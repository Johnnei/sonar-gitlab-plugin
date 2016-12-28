package org.johnnei.sgp.it;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.gitlab.api.models.CommitComment;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Ignore;
import org.junit.Test;

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
	@Ignore("To be built in SGP-21")
	public void testDoubleAnalysis() throws Exception {
		String commitHash = gitCommitAll();
		sonarAnalysis(commitHash);
		sonarAnalysis(commitHash);

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);
		List<String> comments = commitComments.stream()
			.map(CommitComment::getNote)
			.collect(Collectors.toList());

		List<String> messages = Files.readAllLines(getTestResource("sonarqube/issues.txt"));

		for (String message : messages) {
			assertThat(comments, IsCollectionContaining.hasItem(equalTo(message)));
			remoteMatchedComment(comments, message);
		}

		assertThat(
			String.format("%s Issues have been reported and thus comments should be there.", messages.size()),
			comments,
			IsEmptyCollection.empty()
		);
	}

	@Test
	public void testIncrementalAnalysis() throws Exception {
		gitAdd("src/main/java/org/johnnei/sgp/it/api/sources/Main.java");
		gitAdd("pom.xml");
		String commitHash = gitCommit("Initial commit.");
		String secondCommitHash = gitCommitAll();

		gitCheckout(commitHash);
		sonarAnalysis(commitHash);

		gitCheckout(secondCommitHash);
		sonarAnalysis(secondCommitHash);

		assertComments(commitHash, "sonarqube/incremental-1.txt");
		assertComments(secondCommitHash, "sonarqube/incremental-2.txt");
	}

	private void assertComments(String commitHash, String issueFile) throws IOException {
		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);
		int commentCount = commitComments.size();
		List<String> comments = commitComments.stream()
			.map(CommitComment::getNote)
			.collect(Collectors.toList());

		List<String> messages = Files.readAllLines(getTestResource(issueFile));

		for (String message : messages) {
			assertThat(comments, IsCollectionContaining.hasItem(equalTo(message)));
			remoteMatchedComment(comments, message);
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

	private void remoteMatchedComment(List<String> comments, String message) {
		// Remove a single matched comment.
		Iterator<String> commentsIterator = comments.iterator();
		while (commentsIterator.hasNext()) {
			String comment = commentsIterator.next();
			if (comment.equals(message)) {
				commentsIterator.remove();
				return;
			}
		}

		throw new IllegalStateException("Matcher passed but didn't remove message.");
	}
}
