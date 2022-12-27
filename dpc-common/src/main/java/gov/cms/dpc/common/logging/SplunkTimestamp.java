package gov.cms.dpc.common.logging;
import java.time.Instant;

public class SplunkTimestamp {    
    public static String getSplunkTimestamp() {
        return Instant.now().toString().replace("T", " ").substring(0, 22);
    }
}
