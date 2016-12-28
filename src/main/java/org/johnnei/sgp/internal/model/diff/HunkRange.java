package org.johnnei.sgp.internal.model.diff;

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
}
