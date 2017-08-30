package org.johnnei.sgp.internal.gitlab.api.v4.model;

public enum GitLabAccessLevel {

	DEVELOPER(30);

	private final int level;

	GitLabAccessLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
}
