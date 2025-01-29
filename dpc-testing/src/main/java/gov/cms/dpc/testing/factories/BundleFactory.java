package gov.cms.dpc.testing.factories;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Helper class to build a {@link Bundle} out of a group of resources.
 */
public class BundleFactory {
	private BundleFactory() {
		// Not used
	}

	public static Bundle createBundle(Resource... resources){
		final Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.COLLECTION);

		if(resources != null) {
			for(Resource resource:resources){
				bundle.addEntry(new Bundle.BundleEntryComponent().setResource(resource));
			}
		}
		return bundle;
	}
}
