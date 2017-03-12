package org.johnnei.sgp.it.framework.sonarqube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QualityProfile {

	private String key;

	private String name;

	private String language;

	private String languageName;

	@JsonProperty("isInherited")
	private boolean inherited;

	@JsonProperty("isDefault")
	private boolean isDefault;

	private int activeRuleCount;

	private String rulesUpdatedAt;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getLanguageName() {
		return languageName;
	}

	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}

	public boolean isInherited() {
		return inherited;
	}

	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean aDefault) {
		isDefault = aDefault;
	}

	public int getActiveRuleCount() {
		return activeRuleCount;
	}

	public void setActiveRuleCount(int activeRuleCount) {
		this.activeRuleCount = activeRuleCount;
	}

	public String getRulesUpdatedAt() {
		return rulesUpdatedAt;
	}

	public void setRulesUpdatedAt(String rulesUpdatedAt) {
		this.rulesUpdatedAt = rulesUpdatedAt;
	}
}
