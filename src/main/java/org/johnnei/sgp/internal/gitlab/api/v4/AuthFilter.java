package org.johnnei.sgp.internal.gitlab.api.v4;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Objects;

public class AuthFilter implements ClientRequestFilter {

	private final String authToken;

	public AuthFilter(String authToken) {
		this.authToken = Objects.requireNonNull(authToken);
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		requestContext.getHeaders().add("PRIVATE-TOKEN", authToken);
	}
}
