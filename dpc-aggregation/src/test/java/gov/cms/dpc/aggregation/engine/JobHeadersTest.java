package gov.cms.dpc.aggregation.engine;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobHeadersTest {

    private  String requestingIP;
    private  String jobID;
    private  String providerNPI;
    private  final String  transactionTime = "2022-12-27T19:41:33.038Z";
    private  boolean isBulk;
    private JobHeaders jobHeaders;
    private String randomUUID;

    private Map<String, String> headers = new HashMap<>();

    @BeforeEach
    void setUp() {
        this.requestingIP="127.0.0.1";
        this.randomUUID = UUID.randomUUID().toString();
        this.jobID=this.randomUUID;
        this.providerNPI=this.randomUUID;
    }

    @AfterEach
    void tearDown() {
        this.jobHeaders=null;
        this.headers=null;
    }

    @Test
    void buildHeaders() {
        this.isBulk=true;
        this.jobHeaders = new JobHeaders(this.requestingIP,this.randomUUID , this.randomUUID, this.transactionTime,this.isBulk);
        assertThat(this.requestingIP).isNotNull().isEqualTo("127.0.0.1");
        assertThat(this.jobID).isNotNull().isEqualTo(this.randomUUID);
        assertThat(this.providerNPI).isNotNull().isEqualTo(this.randomUUID);
        assertThat(this.transactionTime).isNotNull();
        headers = jobHeaders.buildHeaders();
        assertTrue(Maps.difference(headers, headers).areEqual());
        tearDown();
        this.isBulk=false;
        this.jobHeaders = new JobHeaders(this.requestingIP,this.randomUUID , this.randomUUID, this.transactionTime,this.isBulk);
        assertThat(this.requestingIP).isNotNull().isEqualTo("127.0.0.1");
        assertThat(this.jobID).isNotNull().isEqualTo(this.randomUUID);
        assertThat(this.providerNPI).isNotNull().isEqualTo(this.randomUUID);
        assertThat(this.transactionTime).isNotNull();
        headers = jobHeaders.buildHeaders();
        assertTrue(Maps.difference(headers, headers).areEqual());
    }

}