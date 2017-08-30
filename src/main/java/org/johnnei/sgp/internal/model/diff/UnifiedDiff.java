package org.johnnei.sgp.internal.model.diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabCommitDiff;


/**
 * Represents the information of a parsed unified diff.
 */
public class UnifiedDiff {

	/**
	 * Pattern to match the information of a chunk header.
	 * <p>
	 * Copied from the Sonar GitHub plugin.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Diff_utility#Unified_format"></a>
	 */
	private static final Pattern HEADER_REGEX
		= Pattern.compile("@@\\p{IsWhite_Space}-[0-9]+(?:,[0-9]+)?\\p{IsWhite_Space}\\+([0-9]+)(?:,([0-9]+))?\\p{IsWhite_Space}@@.*");

	private final String filepath;

	private final String commitSha;

	private final Collection<HunkRange> ranges;

	public UnifiedDiff(String commitSha, GitLabCommitDiff commitDiff) {
		this.ranges = new ArrayList<>();
		this.commitSha = commitSha;
		this.filepath = commitDiff.getNewPath();

		parseDiff(commitDiff.getDiff());
	}

	private void parseDiff(String diff) {
		BufferedReader diffReader = new BufferedReader(new StringReader(diff));
		String line;

		try {
			while ((line = diffReader.readLine()) != null) {
				if (line.startsWith("@@")) {
					ranges.add(parseHeader(line));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read diff line.", e);
		}
	}

	private HunkRange parseHeader(String hunkHeader){
		Matcher matcher = HEADER_REGEX.matcher(hunkHeader);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Failed to parse hunk header: " + hunkHeader);
		}

		int begin = Integer.parseInt(matcher.group(1));
		int lines = 1;

		// The second group is not mandatory, I've only found cases in which it failed to on addition with size 1 so default the lines to 1.
		if (matcher.group(2) != null) {
			lines = Integer.parseInt(matcher.group(2));
		}

		return new HunkRange(begin, lines);
	}

	public String getCommitSha() {
		return commitSha;
	}

	public Collection<HunkRange> getRanges() {
		return Collections.unmodifiableCollection(ranges);
	}

	public String getFilepath() {
		return filepath;
	}
}
