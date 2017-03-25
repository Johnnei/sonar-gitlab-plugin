package org.johnnei.sgp.internal.model.diff;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;

/**
 * Created by Johnnei on 2016-12-28.
 */
public class HunkRangeTest {

	@Test
	public void testGetStart() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("Start line is as inputted", cut.getStart(), is(5));
	}

	@Test
	public void testContainsLineStartLine() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("Start line is included", cut.containsLine(5), is(true));
		assertThat("Start line is as inputted", cut.getStart(), is(5));
	}

	@Test
	public void testContainsLineBeforeStart() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("Before start is excluded", cut.containsLine(4), is(false));
	}

	@Test
	public void testContainsLineAfterEnd() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("After end line is excluded", cut.containsLine(25), is(false));
	}

	@Test
	public void testContainsLineOneBeforeEndLine() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("One before end line is included", cut.containsLine(11), is(true));
	}

	@Test
	public void testContainsLineEndLine() throws Exception {
		HunkRange cut = new HunkRange(5, 7);

		assertThat("End line is excluded", cut.containsLine(12), is(false));
	}

	@Test
	public void testEquals() {
		HunkRange cutOne = new HunkRange(5, 7);
		HunkRange cutTwo = new HunkRange(5, 5);
		HunkRange cutThree = new HunkRange(4, 7);

		assertThat("Same instance must be equal", cutOne, equalTo(cutOne));
		assertThat("Line count different", cutOne, not(equalTo(cutTwo)));
		assertThat("Start line different", cutOne, not(equalTo(cutThree)));
		assertThat("Never equal to null", cutOne, not(equalTo(null)));
		assertThat("Different type", cutOne, not(equalTo("Steve")));

	}

	@Test
	public void testHashcode() {
		HunkRange cutOne = new HunkRange(5, 7);
		HunkRange cutTwo = new HunkRange(5, 5);
		HunkRange cutThree = new HunkRange(4, 7);

		assertThat("Same instance must be equal", cutOne.hashCode(), equalTo(cutOne.hashCode()));
		assertThat("Line count different", cutOne.hashCode(), not(equalTo(cutTwo.hashCode())));
		assertThat("Start line different", cutOne.hashCode(), not(equalTo(cutThree.hashCode())));
	}

	@Test
	public void testToString() {
		HunkRange cutOne = new HunkRange(5, 7);

		assertThat("Must start with class name", cutOne.toString(), startsWith(HunkRange.class.getSimpleName()));
	}

}
