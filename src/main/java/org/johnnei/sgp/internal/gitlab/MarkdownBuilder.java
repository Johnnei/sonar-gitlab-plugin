package org.johnnei.sgp.internal.gitlab;

import javax.annotation.Nonnull;

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

	@Override
	public String toString() {
		return builder.toString();
	}
}
