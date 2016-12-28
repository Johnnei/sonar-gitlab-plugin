package org.johnnei.sgp.internal.model.diff;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by Johnnei on 2016-12-28.
 */
public class HunkRangeTest {

	@Test
	public void containsLineStartLine() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("Start line is included", cut.containsLine(5), is(true));
	}

	@Test
	public void containsLineBeforeStart() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("Before start is excluded", cut.containsLine(4), is(false));
	}

	@Test
	public void containsLineAfterEnd() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("After end line is excluded", cut.containsLine(25), is(false));
	}

	@Test
	public void containsLineOneBeforeEndLine() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("One before end line is included", cut.containsLine(11), is(true));
	}

	@Test
	public void containsLineEndLine() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("End line is excluded", cut.containsLine(12), is(false));
	}

}