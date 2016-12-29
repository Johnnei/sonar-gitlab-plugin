package org.johnnei.sgp.internal.gitlab;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MarkdownBuilderTest {

	@Test
	public void testStartListItem() throws Exception {
		MarkdownBuilder cut = new MarkdownBuilder();
		cut.startListItem();

		assertThat("Start of a list item is a '-'", cut.toString(), equalTo("- "));
	}

	@Test
	public void testEndListItem() throws Exception {
		MarkdownBuilder cut = new MarkdownBuilder();
		cut.endListItem();

		assertThat("End of a list item is a linebreak.", cut.toString(), equalTo("\n"));
	}

	@Test
	public void testAddText() throws Exception {
		String text = "The answer to life, the universe and everything.";

		MarkdownBuilder cut = new MarkdownBuilder();
		cut.addText(text);

		assertThat("Text should be added without any extras.", cut.toString(), equalTo(text));
	}

	@Test
	public void testAddLineBreak() throws Exception {
		MarkdownBuilder cut = new MarkdownBuilder();
		cut.endListItem();

		assertThat("A linebreak is a linebreak. (duh?)", cut.toString(), equalTo("\n"));
	}

}