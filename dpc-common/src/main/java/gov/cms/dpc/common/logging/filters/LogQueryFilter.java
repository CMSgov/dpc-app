package gov.cms.dpc.common.logging.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.MDC;

import java.util.List;

public class LogQueryFilter implements ContainerRequestFilter {
	private final List<String> queryParms;

	public LogQueryFilter(List<String> queryParms) {
		this.queryParms = queryParms;
	}

	@Override
	public void filter(ContainerRequestContext requestContext) {
		// Pull all query parameters from the request
		UriInfo uriInfo = requestContext.getUriInfo();
		MultivaluedMap<String, String> queryParmMap = uriInfo.getQueryParameters();

		// Loop through the list of parameters we care about and add them to the MDC if they exist
		queryParms.forEach( parm -> {
			if( queryParmMap.containsKey(parm) ) {
				// Handle the case where they send the same parameter multiple times
				String value = String.join(",", queryParmMap.get(parm));
				MDC.put(parm, value);
			}
		});
	}
}
