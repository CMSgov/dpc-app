package gov.cms.dpc.attribution;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.testing.AbstractMultipleDAOTest;
import org.junit.jupiter.api.DisplayName;

/**
 * The tables in dpc-attribution all have relationships to one another, so you can't just pull in one for a test or
 * Hibernate will flip out when it can't find the others it references.  Instead, you have to pull in all of them.  This
 * class handles doing that so child DAO tests don't have to.
 */
public class AbstractAttributionDAOTest extends AbstractMultipleDAOTest {
	public AbstractAttributionDAOTest() {
		super(
			PatientEntity.class,
			OrganizationEntity.class,
			AttributionRelationship.class,
			RosterEntity.class,
			ProviderEntity.class,
			ContactEntity.class,
			EndpointEntity.class,
			ContactPointEntity.class
		);
	}
}
