package org.johnnei.sgp.internal.gitlab.api.v4.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitLabCommit {

	@JsonProperty("short_id")
	private String shortId;

	public String getShortId() {
		return shortId;
	}
}
