package org.johnnei.sgp.internal.gitlab;

import javax.annotation.Nonnull;

import org.sonar.api.batch.rule.Severity;

/**
 * Class which provides functional orientated methods to build a message in markdown format.
 */
public class MarkdownBuilder {

	@Nonnull
	private StringBuilder builder;

	public MarkdownBuilder() {
		builder = new StringBuilder();
	}

	public MarkdownBuilder startListItem() {
		builder.append("- ");
		return this;
	}

	public MarkdownBuilder endListItem() {
		addLineBreak();
		return this;
	}

	public MarkdownBuilder addText(String text) {
		builder.append(text);
		return this;
	}

	public MarkdownBuilder addLineBreak() {
		builder.append("\n");
		return this;
	}

	public MarkdownBuilder addSeverityIcon(Severity severity) {
		switch (severity) {
			case INFO:
				addText(":information_source:");
				break;
			case MINOR:
				addText(":grey_exclamation:");
				break;
			case MAJOR:
				addText(":exclamation:");
				break;
			case CRITICAL:
				addText(":bangbang:");
				break;
			case BLOCKER:
				addText(":negative_squared_cross_mark:");
				break;
		}
		addText(" ");
		return this;
	}

	@Override
	public String toString() {
		return builder.toString();
	}
}
