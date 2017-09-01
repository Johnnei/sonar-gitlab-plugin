package org.johnnei.sgp.it.framework.sonarqube;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SonarQubeApi {

	// In 6.5 profile_key and rule_key are renamed in profileKey and ruleKey
	@POST
	@Path("/qualityprofiles/activate_rule")
	void activateRule(@QueryParam("profile_key") String key, @QueryParam("rule_key") String rule);

	// In 6.5 profile_key and rule_key are renamed in profileKey and ruleKey
	@POST
	@Path("/qualityprofiles/deactivate_rule")
	void deactivateRule(@QueryParam("profile_key") String key, @QueryParam("rule_key") String rule);

	@GET
	@Path("/qualityprofiles/search")
	SearchQualityProfiles searchQualityProfile(@QueryParam("language") String language);

	// In 6.1 name is renamed to profileName
	@POST
	@Path("/qualityprofiles/create")
	QualityProfileCreation createQualityProfile(@QueryParam("language") String language, @QueryParam("name") String name);

	// In 6.4 profileKey is renamed to profile.
	@POST
	@Path("/qualityprofiles/set_default")
	void setDefaultQualityProfile(@QueryParam("profileKey") String key);
}
