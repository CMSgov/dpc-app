package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.google.common.collect.ImmutableMultimap;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.emory.mathcs.backport.java.util.Collections;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.resources.v1.OrganizationResource;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.api.tasks.tokens.DeleteToken;
import gov.cms.dpc.api.tasks.tokens.GenerateClientTokens;
import gov.cms.dpc.api.tasks.tokens.ListClientTokens;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@SuppressWarnings("unchecked")
@ExtendWith(BufferedLoggerHandler.class)
public class GenerateClientTokenTests {

    private TokenResource tokenResource = Mockito.mock(TokenResource.class);
    private OrganizationResource orgResource = Mockito.mock(OrganizationResource.class);
    private static MacaroonBakery bakery = Mockito.mock(MacaroonBakery.class);
    private ArgumentCaptor<OrganizationPrincipal> principalCaptor = ArgumentCaptor.forClass(OrganizationPrincipal.class);
    private ArgumentCaptor<String> tokenLabelCaptor = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<Optional<OffsetDateTimeParam>> expirationCaptor = ArgumentCaptor.forClass(Optional.class);
    private final GenerateClientTokens gct;
    private final ListClientTokens lct;
    private final DeleteToken dct;
    private final ObjectMapper mapper;
    private final FhirContext ctx;

    GenerateClientTokenTests() {
        this.gct = new GenerateClientTokens(bakery, tokenResource, orgResource);
        this.lct = new ListClientTokens(tokenResource);
        this.dct = new DeleteToken(tokenResource);
        this.mapper = new ObjectMapper();
    }

    @AfterEach
    void cleanup() {
        Mockito.reset(bakery);
        Mockito.reset(tokenResource);
    }

    @Test
    void testTokenCreationNoOrg() throws IOException {
        Mockito.when(bakery.createMacaroon(Mockito.any())).thenAnswer(answer -> MacaroonsBuilder.create("", "", ""));
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, times(1)).createMacaroon(eq(Collections.emptyList()));
        }
    }

    @Test
    void testTokenCreation() throws IOException {

        final TokenEntity response = Mockito.mock(TokenEntity.class);
        Mockito.when(response.getToken()).thenReturn("test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

        final Bundle organizations = client
                .search()
                .forResource(Organization.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(principalCaptor.capture(), Mockito.isNull(), Mockito.isNull(), eq(Optional.empty()));
            assertEquals(id, principalCaptor.getValue().getID(), "Should have correct ID");
        }
    }

    @Test
    void testTokenCreationWithLabel() throws IOException {
        final String tokenLabel = "test-token-label";
        final TokenEntity response = Mockito.mock(TokenEntity.class);
        response.setLabel(tokenLabel);
        Mockito.when(response.getToken()).thenReturn("test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.matches(tokenLabel), Mockito.any())).thenReturn(response);

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString(), "label", tokenLabel);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(Mockito.isNotNull(), Mockito.isNull(), tokenLabelCaptor.capture(), eq(Optional.empty()));
            assertEquals(tokenLabel, tokenLabelCaptor.getValue(), "Should have correct label");
        }
    }

    @Test
    void testTokenCreationWithMissingExpirationValue() throws IOException {
        TokenEntity response = new TokenEntity();
        response.setToken("random test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString(), "expiration", "");
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(Mockito.isNotNull(), Mockito.isNull(), tokenLabelCaptor.capture(), eq(Optional.empty()));
        }
    }

    @Test
    void testTokenCreationWithExpiration() throws IOException {
        final String expires = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(12).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final Optional<OffsetDateTimeParam> optExpires = Optional.of(new OffsetDateTimeParam(expires));
        final TokenEntity response = Mockito.mock(TokenEntity.class);
        Mockito.when(response.getToken()).thenReturn("test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.isNotNull())).thenReturn(response);

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString(), "expiration", expires);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(Mockito.isNotNull(), Mockito.isNull(), Mockito.any(), expirationCaptor.capture());
            assertEquals(optExpires, expirationCaptor.getValue(), "Should have correct expiration");
        }
    }

    @Test
    void testTokenListNoOrg() throws IOException {
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> lct.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testTokenList() throws Exception {
        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());
        Mockito.when(this.tokenResource.getOrganizationTokens(Mockito.any())).thenAnswer(answer -> {
            assertEquals(id, ((OrganizationPrincipal) answer.getArgument(0)).getID(), "Should have correct ID");
            return new CollectionResponse<PublicKeyEntity>(new ArrayList<>());
        });

        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            lct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));

            @SuppressWarnings("rawtypes") final CollectionResponse response = this.mapper.readValue(new ByteArrayInputStream(bos.toByteArray()), CollectionResponse.class);
            assertTrue(response.getEntities().isEmpty(), "Should have a response, but no members");
        }
    }

    @Test
    void testTokenDeleteNoOrg() throws IOException {
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dct.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testTokenDeleteNoToken() throws IOException {
        final UUID id = UUID.randomUUID();
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dct.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have token", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testTokenDelete() throws IOException {
        final UUID id = UUID.randomUUID();
        final UUID keyID = UUID.randomUUID();
        final ImmutableMultimap<String, String> map = ImmutableMultimap.of("organization", id.toString(), "token", keyID.toString());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            dct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(tokenResource, times(1)).deleteOrganizationToken(Mockito.any(), eq(keyID));
        }
    }
}
