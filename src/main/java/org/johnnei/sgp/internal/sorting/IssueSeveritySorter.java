package org.johnnei.sgp.internal.sorting;

import java.util.Comparator;

import org.sonar.api.batch.postjob.issue.PostJobIssue;

import org.johnnei.sgp.internal.model.MappedIssue;

/**
 * Compares two issues based on {@link PostJobIssue#severity()}.
 */
public class IssueSeveritySorter implements Comparator<MappedIssue> {

	@Override
	public int compare(MappedIssue o1, MappedIssue o2) {
		int difference = o1.getIssue().severity().ordinal() - o2.getIssue().severity().ordinal();
		if (difference < 0) {
			return -1;
		} else if (difference > 0) {
			return 1;
		} else {
			return 0;
		}
	}
}
