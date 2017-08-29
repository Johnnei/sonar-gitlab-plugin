package org.johnnei.sgp.internal.gitlab.api.v4.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitLabCommitDiff {

	@JsonProperty("new_path")
	private String newPath;

	private String diff;

	@JsonProperty("old_path")
	private String oldPath;

	@JsonProperty("renamed_file")
	private boolean renamedFile;

	@JsonProperty("deleted_file")
	private boolean deletedFile;

	@JsonProperty("a_mode")
	private String AMode;

	@JsonProperty("b_mode")
	private String BMode;

	@JsonProperty("new_file")
	private boolean newFile;

	public String getNewPath() {
		return newPath;
	}

	public String getDiff() {
		return diff;
	}

	public String getOldPath() {
		return oldPath;
	}

	public boolean getRenamedFile() {
		return renamedFile;
	}

	public boolean getDeletedFile() {
		return deletedFile;
	}

	public String getAMode() {
		return AMode;
	}

	public String getBMode() {
		return BMode;
	}

	public boolean getNewFile() {
		return newFile;
	}
}
