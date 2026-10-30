package gov.cms.dpc.attribution.resources.v3;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupResourceUnitTest {

    private GroupResource groupResource;

    @BeforeEach
    void setUp() {
        groupResource = new GroupResource();
    }

    @Test
    void testCreateRoster() {
        Response response = groupResource.createRoster();
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatus(), "Response status should have been NOT_IMPLEMENTED (501)");
    }

    @Test
    void testReplaceRoster() {
        Response response = groupResource.replaceRoster();
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatus(), "Response status should have been NOT_IMPLEMENTED (501)");
    }

    @Test
    void testDeleteRoster() {
        Response response = groupResource.deleteRoster();
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatus(), "Response status should have been NOT_IMPLEMENTED (501)");
    }

    @Test
    void testMemberAdd() {
        Response response = groupResource.memberAdd();
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatus(), "Response status should have been NOT_IMPLEMENTED (501)");
    }

    @Test
    void testMemberRemove() {
        Response response = groupResource.memberRemove();
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatus(), "Response status should have been NOT_IMPLEMENTED (501)");
    }
}
