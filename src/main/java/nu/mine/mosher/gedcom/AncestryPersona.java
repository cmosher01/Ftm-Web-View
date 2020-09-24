package nu.mine.mosher.gedcom;

import org.w3c.dom.Element;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nu.mine.mosher.gedcom.XmlUtils.*;

/**
 * Represents a persona in an Ancestry.com database record.
 * Ancestry.com exports this as _APID tag.
 */
public class AncestryPersona {
    private final int db;
    private final long indi;
    private final URI link;



    public AncestryPersona(final int db, final long indi) {
        this.db = db;
        this.indi = indi;
        this.link = buildLink(db, indi);
    }

    private static final Pattern PAT_APID = Pattern.compile("(?:(\\d+),)?(\\d+)::?(\\d+)(.*)");

    public static Optional<AncestryPersona> of(final String apid) {
        if (!StringUtils.is(apid)) {
            return Optional.empty();
        }
        final Matcher m = PAT_APID.matcher(apid);
        if (!m.matches()) {
//            Jul.log().log(Level.WARNING, "Unknown _APID format: "+apid);
            return Optional.empty();
        }
        try {
            return Optional.of(new AncestryPersona(Integer.parseInt(m.group(2)), Long.parseLong(m.group(3))));
        } catch (final Throwable ignore) {
//            Jul.log().log(Level.WARNING, "Unknown _APID format: "+apid, ignore);
            return Optional.empty();
        }
    }



    public int getDb() {
        return this.db;
    }

    public long getIndi() {
        return this.indi;
    }

    public Optional<URI> getLink() {
        return Optional.ofNullable(this.link);
    }

    public boolean isLink() {
        return this.link != null;
    }



    private static URI buildLink(int db, long indi) {
        try {
            return new URI(formatLink(db, indi));
        } catch (final Throwable e) {
            return null;
        }
    }

    private static String formatLink(final int db, final long indi) {
        return String.format("http://search.ancestry.com/cgi-bin/sse.dll?indiv=1&dbid=%d&h=%d", db, indi);
    }

    public void appendAsAHref(final Element parent) {
        final Element a = e(parent, "a");
        a.setAttribute("href", this.link.toASCIIString());
        a.setTextContent(new String(Character.toChars(0x1F33F)));
    }
}
