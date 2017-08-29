package org.johnnei.sgp.internal.gitlab.api.v4.model;

public class GitLabProject {

	private int id;

	private String name;

	private GitLabNamespace namespace;

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public GitLabNamespace getNamespace() {
		return namespace;
	}
}
