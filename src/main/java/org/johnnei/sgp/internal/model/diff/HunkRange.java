package org.johnnei.sgp.internal.model.diff;

import java.util.Objects;

/**
 * Represent the hunk range information of either before of after.
 */
public class HunkRange {

	private final int start;

	private final int lineCount;

	public HunkRange(int start, int lineCount) {
		this.start = start;
		this.lineCount = lineCount;
	}

	/**
	 * @param line The line number to test
	 * @return <code>true</code> when the line is within the range. Otherwise <code>false</code>.
	 */
	public boolean containsLine(int line) {
		return line >= start && line < (start + lineCount);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof HunkRange)) {
			return false;
		}

		HunkRange hunkRange = (HunkRange) o;
		return start == hunkRange.start && lineCount == hunkRange.lineCount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, lineCount);
	}

	@Override
	public String toString() {
		return "HunkRange{start=" + start + ", lineCount=" + lineCount + '}';
	}
}
