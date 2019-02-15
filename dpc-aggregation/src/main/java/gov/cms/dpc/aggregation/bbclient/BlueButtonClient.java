package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.aggregation.AggregationEngine;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.hl7.fhir.dstu3.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.UUID;

public class BlueButtonClient {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);

    public BlueButtonClient(){

    }

    public int getFHIRFromBeneficiaryID(UUID beneficiaryID) {
        // From http://hapifhir.io/doc_rest_client.html

        try {
            InputStream keyStoreStream = new FileInputStream("/Users/isears/.keystore");
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, "changeit".toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, "changeit".toCharArray())
                    .build();

            HttpClient mutualTlsHttpClient = HttpClients.custom().setSSLContext(sslContext).build();
            //HttpClient mutualTlsHttpClient = HttpClients.createDefault();
            //HttpResponse response = mutualTlsHttpClient.execute(new HttpGet("https://fhir.backend.bluebutton.hhsdevcloud.us/v1/fhir/ExplanationOfBenefit?patient=20140000008325"));
            //System.out.println(response.getStatusLine().getStatusCode());

            FhirContext ctx = FhirContext.forDstu3();
            String serverBase = "https://fhir.backend.bluebutton.hhsdevcloud.us/v1/fhir/";
            String searchURL = "ExplanationOfBenefit?patient=20140000008325";

            ctx.getRestfulClientFactory().setHttpClient(mutualTlsHttpClient);
            IGenericClient client = ctx.newRestfulGenericClient(serverBase);
            Bundle results = client.search()
                    .byUrl(searchURL)
                    .returnBundle(Bundle.class)
                    .execute();

            System.out.println(results);
        }catch(Exception ex){
            System.out.println("Fail");
            System.out.println(ex.toString());
        }


        return 1;
    }

}
