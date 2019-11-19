package gov.cms.dpc.api.core;

import gov.cms.dpc.common.annotations.ExportPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;

public class FileManager {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    private final String fileLocation;

    @Inject
    FileManager(@ExportPath String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public File getFile(String fileID) {
        final java.nio.file.Path path = Paths.get(String.format("%s/%s.ndjson", fileLocation, fileID));
        logger.debug("Streaming file {}", path.toString());
        return new File(path.toString());
    }
}
