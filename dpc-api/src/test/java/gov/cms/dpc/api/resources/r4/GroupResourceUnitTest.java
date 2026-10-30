package gov.cms.dpc.api.resources.r4;

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
    void testGetRoster() {
        Response response = groupResource.getRoster();
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatus(), "Response status should have been NOT_IMPLEMENTED (501)");
    }

    @Test
    void testUpdateRoster() {
        Response response = groupResource.updateRoster();
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
