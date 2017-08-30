package org.johnnei.sgp.internal.gitlab.api.v4.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitLabSession {

	@JsonProperty("private_token")
	private String privateToken;

	public String getPrivateToken() {
		return privateToken;
	}
}
