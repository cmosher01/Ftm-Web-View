package nu.mine.mosher.gedcom;



import org.slf4j.*;

import java.io.*;


public record IndexedDatabase(File file) {
    private static final Logger LOG =  LoggerFactory.getLogger(IndexedDatabase.class);

    public IndexedDatabase {
        this.file = canon(file);
    }

    private static File canon(final File file) {
        try {
            final File f =  file.getCanonicalFile();
            if (f.canRead()) {
                LOG.debug("will read file: {}", f);
            } else {
                LOG.warn("Cannot read file {}", file);
            }
            return f;
        } catch (final Throwable e) {
            LOG.warn("Cannot access file {}", file, e);
            return file;
        }
    }

    public String dirMedia() {
        final String[] ss = file().getName().split("\\.(?=[^.]*$)");
        final String dir = ss[0] + " Media";
        LOG.debug("will use media path: {}", dir);
        return dir;
    }
}
