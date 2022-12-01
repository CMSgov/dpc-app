package gov.cms.dpc.queue;
import java.time.Instant;

public class QueueHelpers {
    public static String getSplunkTimestamp() {
        return Instant.now().toString().replace("T", " ").substring(0, 22);
    }
}
