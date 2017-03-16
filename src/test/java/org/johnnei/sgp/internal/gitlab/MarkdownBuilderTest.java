package org.johnnei.sgp.internal.gitlab;

import org.junit.Test;
import org.sonar.api.batch.rule.Severity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MarkdownBuilderTest {

	private  MarkdownBuilder cut = new MarkdownBuilder();

	@Test
	public void testStartListItem() throws Exception {
		cut.startListItem();

		assertThat("Start of a list item is a '-'", cut.toString(), equalTo("- "));
	}

	@Test
	public void testEndListItem() throws Exception {
		cut.endListItem();

		assertThat("End of a list item is a linebreak.", cut.toString(), equalTo("\n"));
	}

	@Test
	public void testAddText() throws Exception {
		String text = "The answer to life, the universe and everything.";

		cut.addText(text);

		assertThat("Text should be added without any extras.", cut.toString(), equalTo(text));
	}

	@Test
	public void testAddLineBreak() throws Exception {
		cut.endListItem();

		assertThat("A linebreak is a linebreak. (duh?)", cut.toString(), equalTo("\n"));
	}

	@Test
	public void testAddSeverityIconInfo() throws Exception {
		cut.addSeverityIcon(Severity.INFO);

		assertThat("Incorrect emoji for Info level severity.", cut.toString(), equalTo(":information_source: "));
	}

	@Test
	public void testAddSeverityIconMinor() throws Exception {
		cut.addSeverityIcon(Severity.MINOR);

		assertThat("Incorrect emoji for Minor level severity.", cut.toString(), equalTo(":grey_exclamation: "));
	}

	@Test
	public void testAddSeverityIconMajor() throws Exception {
		cut.addSeverityIcon(Severity.MAJOR);

		assertThat("Incorrect emoji for Major level severity.", cut.toString(), equalTo(":exclamation: "));
	}

	@Test
	public void testAddSeverityIconCritical() throws Exception {
		cut.addSeverityIcon(Severity.CRITICAL);

		assertThat("Incorrect emoji for Critical level severity.", cut.toString(), equalTo(":bangbang: "));
	}

	@Test
	public void testAddSeverityIconBlocker() throws Exception {
		cut.addSeverityIcon(Severity.BLOCKER);

		assertThat("Incorrect emoji for Blocker level severity.", cut.toString(), equalTo(":negative_squared_cross_mark: "));
	}
}
