package nu.mine.mosher.genealogy;


import org.slf4j.*;

import java.io.File;


public class IndexedDatabase {
    private static final Logger LOG =  LoggerFactory.getLogger(IndexedDatabase.class);

    private final File file;
    private final String dir;

    public IndexedDatabase(final File file) {
        this.file = canon(file);
        this.dir = dirMedia(file);
        LOG.debug("database: sqlite-file=\"{}\", media-directory=\"{}\"", this.file, this.dir);
    }

    public File file() {
        return this.file;
    }

    public String dirMedia() {
        return this.dir;
    }

    private static File canon(final File file) {
        try {
            final File f =  file.getCanonicalFile();
            if (f.canRead()) {
            } else {
                LOG.warn("Cannot read file {}", file);
            }
            return f;
        } catch (final Throwable e) {
            LOG.warn("Cannot access file {}", file, e);
            return file;
        }
    }

    private static String dirMedia(final File file) {
        final String[] ss = file.getName().split("\\.(?=[^.]*$)");
        return ss[0] + " Media";
    }

    @Override
    public String toString() {
        return String.format("database: sqlite-file=\"%s\", media-directory=\"%s\"", this.file.getPath(), this.dir);
    }
}
