package org.johnnei.sgp.it;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.gitlab.api.models.CommitComment;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

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
