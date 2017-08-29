package org.johnnei.sgp.internal.gitlab.api.v4.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitLabUser {

	private int id;

	private String username;

	private String email;

	@JsonProperty("projects_limit")
	private int projectsLimit;

	public String getUsername() {
		return username;
	}

	public int getProjectsLimit() {
		return projectsLimit;
	}

	public int getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}
}
