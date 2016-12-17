package org.johnnei.sgp;

import java.io.IOException;

import org.junit.Test;

import org.johnnei.sgp.it.IntegrationTest;

/**
 * Created by Johnnei on 2016-12-04.
 */
public class CommentOnCommitIT extends IntegrationTest {

	@Test
	public void testCommentsAreCreated() throws IOException {
		sonarAnalysis("5aa1219c40a736f4ab91b5c19cd3d7fa4746cf60");
	}
}
