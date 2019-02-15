package gov.cms.dpc.aggregation.bbclient;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BlueButtonClientTest {
    private BlueButtonClient bbc;

    @BeforeEach
    public void setupBlueButtonClient(){
        bbc = new BlueButtonClient();
    }

    @Test
    void testGetFHIRFromBeneficiaryID() {
        //System.setProperty("javax.net.ssl.keyStore", "/Users/isears/.keystore");
        System.out.println(System.getProperty("javax.net.ssl.keyStore"));
        UUID testUUID = UUID.randomUUID();
        bbc.getFHIRFromBeneficiaryID(testUUID);
    }

    @Test
    void testMutualTlsConnection() {
        try {
            String CERT_ALIAS = "bb-client-test";
            String CERT_PASSWORD = "password";

            KeyStore identityKeystore = KeyStore.getInstance("jks");
            FileInputStream identityKeystoreFile = new FileInputStream(new File("/Users/isears/Repos/hhs-all/bluebutton-backend-public/identity.jks"));
            identityKeystore.load(identityKeystoreFile, CERT_PASSWORD.toCharArray());

            //KeyStore trustKeystore = KeyStore.getInstance("jks");
            //FileInputStream trustKeystoreFile = new FileInputStream(new File("/Users/isears/Repos/hhs-all/bluebutton-backend-public/truststore.jks"));
            //trustKeystore.load(trustKeystoreFile, CERT_PASSWORD.toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(identityKeystore, CERT_PASSWORD.toCharArray(), new PrivateKeyStrategy() {
                        @Override
                        public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
                            return CERT_ALIAS;
                        }
                        //}).loadTrustMaterial(trustKeystore, null).build();
                    }).build();


            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{"TLSv1.2", "TLSv1.1"}, null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier()
            );

            CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();

            HttpGet get = new HttpGet("https://fhir.backend.bluebutton.hhsdevcloud.us/v1/fhir/ExplanationOfBenefit/?patient=20140000008325");
            get.setHeader("Accept", "application/json");
            HttpResponse response = client.execute(get);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = bufferedReader.readLine()) != null){
                System.out.println(line);
                break; // Brevity
            }

        } catch(Exception ex) {
            System.out.println("Fail: " + ex);
        }

    }

    @Test
    void testMutualTlsConnectionWithoutCustomSSL() throws IOException {
        URL url = new URL("https://fhir.backend.bluebutton.hhsdevcloud.us/v1/fhir/ExplanationOfBenefit/?patient=20140000008325");
        URLConnection urlConnection = url.openConnection();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String inputLine;
        while ((inputLine = bufferedReader.readLine()) != null) {
            System.out.println(inputLine);
            break;
        }
        bufferedReader.close();
    }
}