package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.IOException;

public class RingBufferLogger extends ConsoleAppender<ILoggingEvent> {

    private final CircularFifoQueue<ILoggingEvent> ringBuffer;
    private String dumpWith;

    public RingBufferLogger() {
        this.ringBuffer = new CircularFifoQueue<>(100);
    }

    @Override
    protected synchronized void append(ILoggingEvent event) {
        final boolean offer = this.ringBuffer.offer(event);
        if (!offer) {
            addError("Unable to add message to queue");
        }
        if (event.getLevel().levelStr.equals(this.dumpWith.toUpperCase())) {
            dumpLogMessages();
        }
    }

    public synchronized void dumpLogMessages() {
        ringBuffer
                .iterator()
                .forEachRemaining(event -> {
                    try {
                        super.writeOut(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        // Empty the buffer
        this.ringBuffer.clear();
    }

    @Override
    public void start() {
        this.ringBuffer.clear();
        super.start();
    }

    public String getDumpWith() {
        return dumpWith;
    }

    public void setDumpWith(String dumpWith) {
        this.dumpWith = dumpWith;
    }
}
