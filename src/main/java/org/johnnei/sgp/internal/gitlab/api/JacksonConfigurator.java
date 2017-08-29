package org.johnnei.sgp.internal.gitlab.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

public class JacksonConfigurator extends ResteasyJackson2Provider {

	public JacksonConfigurator() {
		super();
		setMapper(new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
	}
}
