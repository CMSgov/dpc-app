package gov.cms.dpc.api;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.SingleValidationMessage;
import org.hl7.fhir.dstu3.model.BaseResource;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class APIHelpers {


    public static <T extends BaseResource> Bundle bulkResourceClient(Class<T> clazz, IGenericClient client, Consumer<T> entryConsumer, Bundle resourceBundle) {
        // We need to figure out how to validate the bundle entries
        resourceBundle
                .getEntry()
                .stream()
                .filter(Bundle.BundleEntryComponent::hasResource)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getClass().equals(clazz))
                .map(clazz::cast)
                .forEach(entryConsumer);

        final Parameters params = new Parameters();
        params.addParameter().setResource(resourceBundle);
        return client
                .operation()
                .onType(clazz)
                .named("submit")
                .withParameters(params)
                .returnResourceType(Bundle.class)
                .encodedJson()
                .execute();
    }

    public static boolean hasProfile(BaseResource value, String profileURI) {
        return value
                .getMeta()
                .getProfile()
                .stream()
                .anyMatch(pred -> pred.getValueAsString().equals(profileURI));
    }

    public static String formatValidationMessages(List<SingleValidationMessage> messages) {
        return messages
                .stream()
                .map(SingleValidationMessage::getMessage)
                .collect(Collectors.joining(", "));
    }
}
