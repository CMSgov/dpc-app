package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import edu.emory.mathcs.backport.java.util.Collections;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.api.tasks.tokens.DeleteToken;
import gov.cms.dpc.api.tasks.tokens.GenerateClientTokens;
import gov.cms.dpc.api.tasks.tokens.ListClientTokens;
import gov.cms.dpc.macaroons.MacaroonBakery;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@SuppressWarnings("unchecked")
@ExtendWith(BufferedLoggerHandler.class)
public class GenerateClientTokenTests {

    private TokenResource tokenResource = Mockito.mock(TokenResource.class);
    private static MacaroonBakery bakery = Mockito.mock(MacaroonBakery.class);
    private ArgumentCaptor<OrganizationPrincipal> principalCaptor = ArgumentCaptor.forClass(OrganizationPrincipal.class);
    private ArgumentCaptor<String> tokenLabelCaptor = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<Optional<OffsetDateTimeParam>> expirationCaptor = ArgumentCaptor.forClass(Optional.class);
    private final GenerateClientTokens gct;
    private final ListClientTokens lct;
    private final DeleteToken dct;
    private final ObjectMapper mapper;

    GenerateClientTokenTests() {
        this.gct = new GenerateClientTokens(bakery, tokenResource);
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
    void testTokenCreationNoOrg() throws Exception {
        Mockito.when(bakery.createMacaroon(Mockito.any())).thenAnswer(answer -> MacaroonsBuilder.create("", "", ""));
        final Map<String, List<String>> map = Map.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, times(1)).createMacaroon(eq(Collections.emptyList()));
        }
    }

    @Test
    void testTokenCreation() throws Exception {

        final TokenEntity response = Mockito.mock(TokenEntity.class);
        Mockito.when(response.getToken()).thenReturn("test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(principalCaptor.capture(), Mockito.isNull(), Mockito.isNull(), eq(Optional.empty()));
            assertEquals(id, principalCaptor.getValue().getID(), "Should have correct ID");
        }
    }

    @Test
    void testTokenCreationWithLabel() throws Exception {
        final String tokenLabel = "test-token-label";
        final TokenEntity response = Mockito.mock(TokenEntity.class);
        response.setLabel(tokenLabel);
        Mockito.when(response.getToken()).thenReturn("test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.matches(tokenLabel), Mockito.any())).thenReturn(response);

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()), "label", List.of(tokenLabel));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(Mockito.isNotNull(), Mockito.isNull(), tokenLabelCaptor.capture(), eq(Optional.empty()));
            assertEquals(tokenLabel, tokenLabelCaptor.getValue(), "Should have correct label");
        }
    }

    @Test
    void testTokenCreationWithMissingExpirationValue() throws Exception {
        TokenEntity response = new TokenEntity();
        response.setToken("random test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(response);

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()), "expiration", List.of(""));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(Mockito.isNotNull(), Mockito.isNull(), tokenLabelCaptor.capture(), eq(Optional.empty()));
        }
    }

    @Test
    void testTokenCreationWithExpiration() throws Exception {
        final String expires = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(12).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final Optional<OffsetDateTimeParam> optExpires = Optional.of(new OffsetDateTimeParam(expires));
        final TokenEntity response = Mockito.mock(TokenEntity.class);
        Mockito.when(response.getToken()).thenReturn("test token");
        Mockito.when(tokenResource.createOrganizationToken(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.isNotNull())).thenReturn(response);

        final UUID id = UUID.randomUUID();
        final Organization org = new Organization();
        org.setId(id.toString());

        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()), "expiration", List.of(expires));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(bakery, never()).createMacaroon(eq(Collections.emptyList()));
            Mockito.verify(tokenResource, times(1)).createOrganizationToken(Mockito.isNotNull(), Mockito.isNull(), Mockito.any(), expirationCaptor.capture());
            assertEquals(optExpires, expirationCaptor.getValue(), "Should have correct expiration");
        }
    }

    @Test
    void testTokenListNoOrg() throws IOException {
        final Map<String, List<String>> map = Map.of();
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

        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            lct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));

            @SuppressWarnings("rawtypes") final CollectionResponse response = this.mapper.readValue(new ByteArrayInputStream(bos.toByteArray()), CollectionResponse.class);
            assertTrue(response.getEntities().isEmpty(), "Should have a response, but no members");
        }
    }

    @Test
    void testTokenDeleteNoOrg() throws IOException {
        final Map<String, List<String>> map = Map.of();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dct.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have organization", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testTokenDeleteNoToken() throws IOException {
        final UUID id = UUID.randomUUID();
        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final WebApplicationException ex = assertThrows(WebApplicationException.class, () -> dct.execute(map, new PrintWriter(new OutputStreamWriter(bos))));
            assertEquals("Must have token", ex.getMessage(), "Should have correct message");
        }
    }

    @Test
    void testTokenDelete() throws IOException {
        final UUID id = UUID.randomUUID();
        final UUID keyID = UUID.randomUUID();
        final Map<String, List<String>> map = Map.of("organization", List.of(id.toString()), "token", List.of(keyID.toString()));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            dct.execute(map, new PrintWriter(new OutputStreamWriter(bos)));
            Mockito.verify(tokenResource, times(1)).deleteOrganizationToken(Mockito.any(), eq(keyID));
        }
    }
}
