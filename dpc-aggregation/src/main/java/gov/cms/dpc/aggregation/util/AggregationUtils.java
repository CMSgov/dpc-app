package gov.cms.dpc.aggregation.util;

import org.bouncycastle.jcajce.provider.digest.SHA256;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class AggregationUtils {

    private AggregationUtils() {
        // static methods only
    }

    public static byte[] generateChecksum(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return new SHA256.Digest().digest(fileInputStream.readAllBytes());
        }
    }
}
