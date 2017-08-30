package org.johnnei.sgp.it.framework.sonarqube;

import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.sgp.internal.gitlab.api.JacksonConfigurator;
import org.johnnei.sgp.it.framework.CommandLine;
import org.johnnei.sgp.it.framework.gitlab.GitLabSupport;
import org.johnnei.sgp.sonar.GitLabPlugin;

public class SonarQubeSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(SonarQubeSupport.class);

	private static final String CUSTOM_QUALITY_PROFILE_NAME = "it-profile";

	private final GitLabSupport gitlab;

	private final CommandLine commandLine;

	private final String host;

	private final SonarQubeApi api;

	private int customRulesActive = 0;

	public SonarQubeSupport(GitLabSupport gitlab, CommandLine commandLine, String host) {
		this.gitlab = gitlab;
		this.commandLine = commandLine;
		this.host = host;
		this.api = new ResteasyClientBuilder()
			.register(JacksonConfigurator.class)
			.register(new BasicAuthentication("admin", "admin"))
			.register((ClientRequestFilter) requestContext -> LOGGER.debug("Request: {}", requestContext.getUri()))
			.build()
			.target(host)
			.proxy(SonarQubeApi.class);
	}

	public static Map<String, String> createDefaultSettings() {
		Map<String, String> settings = new HashMap<>();
		settings.put(GitLabPlugin.GITLAB_BREAK_PIPELINE, "true");
		return settings;
	}

	public void ensureDefaultQualityProfile() throws IOException {
		LOGGER.info("Ensuring that Sonar Way is the default Quality Profile.");
		QualityProfile sonarWay = getSonarWayQualityProfile();
		if (!sonarWay.isDefault()) {
			api.setDefaultQualityProfile(sonarWay.getKey());
		}

	}

	public void runAnalysis() throws IOException {
		runAnalysis(null, false, createDefaultSettings());
	}

	public void runAnalysis(String commitHash) throws IOException {
		runAnalysis(commitHash, true, createDefaultSettings());
	}

	public void runAnalysis(String commitHash, Map<String, String> settings) throws IOException {
		runAnalysis(commitHash, true, settings);
	}

	private void runAnalysis(String commitHash, boolean incremental, Map<String, String> setttings) throws IOException {
		LOGGER.info("Starting SonarQube Analysis.");

		String argument = "mvn -B" +
			// Ensure a clean state
			" clean" +
			// Provide binaries
			" compile " +
			// Invoke sonar analysis
			" sonar:sonar" +
			// Enable Maven debug to not suppress the Sonar debug logging
			" --debug" +
			// Enable Sonar debug logging to analyse test failures.
			" -Dsonar.log.level=DEBUG" +
			// The host at which the SonarQube instance with our plugin is running.
			" -Dsonar.host.url=" + host;

		if (incremental) {
			// Run analysis in issues mode in order to process the issues on the scanner side
			argument += " -Dsonar.analysis.mode=issues" +
				// The host at which our target gitlab instance is running.
				" -Dsonar.gitlab.uri=" + gitlab.getUrl() +
				// The authentication token to access the project within Gitlab
				" -Dsonar.gitlab.auth.token=" + gitlab.getSonarUserToken() +
				// The project to comment on
				" -Dsonar.gitlab.analyse.project=" + gitlab.getProjectName();

			for (Map.Entry<String, String> entry : setttings.entrySet()) {
				argument += " -D" + entry.getKey() + "=" + entry.getValue();
			}

			if (commitHash != null) {
				// The commit we're analysing.
				argument += " -Dsonar.gitlab.analyse.commit=" + commitHash;
			}
		}

		commandLine.startAndAwait(argument);
	}

	public AutoCloseable enableRule(String ruleKey) throws IOException {
		QualityProfile profile = getCustomQualityProfile();

		if (customRulesActive == 0) {
			LOGGER.debug("Setting custom Quality Profile as default profile.");
			api.setDefaultQualityProfile(profile.getKey());
		}

		customRulesActive++;
		api.activateRule(profile.getKey(), ruleKey);
		LOGGER.debug("Enabled {} on {}", ruleKey, profile);

		return () -> disableRule(profile, ruleKey);
	}

	private void disableRule(QualityProfile profile, String ruleKey) throws IOException {
		api.deactivateRule(profile.getKey(), ruleKey);
		LOGGER.debug("Disabled {} on {}", ruleKey, profile);

		customRulesActive--;
		if (customRulesActive == 0) {
			LOGGER.debug("Setting Sonar Way Quality Profile as default profile.");
			api.setDefaultQualityProfile(getSonarWayQualityProfile().getKey());
		}
	}

	private QualityProfile getCustomQualityProfile() throws IOException {
		return api.searchQualityProfile("java")
			.getProfiles()
			.stream()
			.filter(profile -> CUSTOM_QUALITY_PROFILE_NAME.equalsIgnoreCase(profile.getName()))
			.findFirst()
			.orElseGet(() -> api.createQualityProfile("java", CUSTOM_QUALITY_PROFILE_NAME).getProfile());
	}

	private QualityProfile getSonarWayQualityProfile() throws IOException {
		return api.searchQualityProfile("java")
			.getProfiles()
			.stream()
			.filter(profile -> !CUSTOM_QUALITY_PROFILE_NAME.equalsIgnoreCase(profile.getName()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Sonar Way quality profile is missing."));
	}
}
