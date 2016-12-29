package org.johnnei.sgp.it;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.gitlab.api.models.CommitComment;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import org.johnnei.sgp.it.framework.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by Johnnei on 2016-12-04.
 */
public class CommentOnCommitIT extends IntegrationTest {

	@Test
	public void testCommentsAreCreated() throws IOException {
		String commitHash = gitCommitAll();
		sonarAnalysis(commitHash);

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);
		List<String> comments = commitComments.stream()
			.filter(comment -> comment.getLine() != null)
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
	public void testSummaryIsCreated() throws IOException {
		final String expectedSummary = Files
			.readAllLines(getTestResource("sonarqube/summary.txt"))
			.stream()
			.reduce((a, b) -> a + "\n" + b)
			.orElseThrow(() -> new IllegalStateException("Missing Summary information"));

		String commitHash = gitCommitAll();
		sonarAnalysis(commitHash);

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);
		List<String> comments = commitComments.stream()
			.filter(comment -> comment.getLine() == null)
			.map(CommitComment::getNote)
			.collect(Collectors.toList());

		assertThat("Only 1 summary comment should be created", comments, IsCollectionWithSize.hasSize(1));
		assertThat("The summary doesn't match the expected summary.", comments.get(0), equalTo(expectedSummary));
	}
}
