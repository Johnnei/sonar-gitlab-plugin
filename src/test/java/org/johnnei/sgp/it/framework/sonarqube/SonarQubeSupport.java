package org.johnnei.sgp.it.framework.sonarqube;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.sgp.it.framework.CommandLine;
import org.johnnei.sgp.it.framework.gitlab.GitLabSupport;
import org.johnnei.sgp.sonar.GitLabPlugin;

public class SonarQubeSupport {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Logger LOGGER = Loggers.get(SonarQubeSupport.class);

	private final GitLabSupport gitlab;

	private final CommandLine commandLine;

	private final String host;

	public SonarQubeSupport(GitLabSupport gitlab, CommandLine commandLine, String host) {
		this.gitlab = gitlab;
		this.commandLine = commandLine;
		this.host = host;
	}

	public static Map<String, String> createDefaultSettings() {
		Map<String, String> settings = new HashMap<>();
		settings.put(GitLabPlugin.GITLAB_BREAK_PIPELINE, "true");
		return settings;
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
		String profile = getQualityProfile();

		Request enableRequest = new Request.Builder()
			.url(String.format(host + "/api/qualityprofiles/activate_rule?profile_key=%s&rule_key=%s", profile, ruleKey))
			.header("Authorization", Credentials.basic("admin", "admin"))
			.post(RequestBody.create(MediaType.parse("application/json"), ""))
			.build();


		OkHttpClient client = new OkHttpClient();

		String result = client.newCall(enableRequest).execute().body().string();

		LOGGER.debug("Enabled {} on {}: {}", ruleKey, profile, result);

		return () -> disableRule(client, profile, ruleKey);
	}

	private void disableRule(OkHttpClient client, String profile, String ruleKey) throws IOException {
		Request disableRequest = new Request.Builder()
			.url(String.format(host + "/api/qualityprofiles/deactivate_rule?profile_key=%s&rule_key=%s", profile, ruleKey))
			.header("Authorization", Credentials.basic("admin", "admin"))
			.post(RequestBody.create(MediaType.parse("application/json"), ""))
			.build();

		String result = client.newCall(disableRequest).execute().body().string();

		LOGGER.debug("Disabled {} on {}: {}", ruleKey, profile, result);
	}

	private String getQualityProfile() throws IOException {
		Request profileRequest = new Request.Builder()
			.url(host + "/api/qualityprofiles/search?language=java")
			.build();

		OkHttpClient client = new OkHttpClient();

		return MAPPER.readValue(client.newCall(profileRequest).execute().body().string(), SearchQualityProfiles.class)
			.getProfiles()
			.stream()
			.map(QualityProfile::getKey)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Failed to find Java Quality Profile."));
	}
}
