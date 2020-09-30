package nu.mine.mosher.gedcom;



import org.slf4j.*;
import org.w3c.dom.Element;

import java.util.*;

import static nu.mine.mosher.gedcom.StringUtils.*;
import static nu.mine.mosher.gedcom.XmlUtils.*;


public record EventSource(
    int pkidSourceLink,
    Integer stars, // TODO stars/justification (and/or calcflags)
    String just,

    Integer pkidCitation,
    String page,
    String comment,
    String footnote,
    String apid,

    String author,
    String title,
    String placePub,
    String pub,
    String datePub,
    String callno,
    String source,
    String apidSource,

    List<MediaFile> media,

    String weblink
) {
    private static final Logger LOG = LoggerFactory.getLogger(EventSource.class);

    public EventSource() {
        this(0,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,new ArrayList<>(),null);
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof EventSource that)) {
            return false;
        }
        return this.pkidCitation().equals(that.pkidCitation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pkidCitation());
    }

    public void appendTo(final Element parent, final IndexedDatabase indexedDatabase) {
        final boolean wasTei = teiStyleIfPossible(safe(page), parent);
        if (wasTei) {
            return;
        }




        // TODO look for URLs

        final String author = cleanCitationElement(author());
        if (is(author)) {
            t(parent, author);
        }

        final String title = filterTitle(cleanCitationElement(title()));
        if (is(author) && is(title)) {
            t(parent, ", ");
        }

        if (is(title)) {
            final Element cite = e(parent, "cite");
            cite.setTextContent(title());
        }

        final String place = no(safe(placePub()), "place");
        final String pub = no(safe(pub()), "publisher");
        final String date = no(safe(datePub()), "date");

        t(parent, String.format(" (%s: %s, %s)", place, pub, date));

        final String page = filterPage(cleanCitationElement(page()));
        if (is(page)) {
            t(parent, ", "+page);
        }

        t(parent, ".");

        media().forEach(m -> {
            t(parent, " ");
            final Element a = e(parent, "a");
            final String dirMedia = indexedDatabase.dirMedia();
            final String path = m.file().replaceAll("\\\\", "/").replaceFirst("^~/", dirMedia + "/");
            a.setAttribute("href", "../ftm/"+path);
            a.setTextContent(new String(Character.toChars(0x1F5BB)));
        });

        if (!safe(weblink()).isBlank()) {
            t(parent, " ");
            final Element a = e(parent, "a");
            a.setAttribute("href", safe(weblink()));
            a.setTextContent(new String(Character.toChars(0x1F517)));
        }

        final Optional<AncestryPersona> optAncestry = AncestryPersona.of(apid());
        if (optAncestry.isPresent()) {
            final AncestryPersona ancestry = optAncestry.get();
            if (ancestry.isLink()) {
                t(parent, " ");
                ancestry.appendAsAHref(parent);
            }
        }

        // TODO: notes
    }

    private static String filterTitle(final String title) {
        return title;
        // TODO ? return title.replaceAll("Web:\\s*", "");
    }

    private static String filterPage(final String page) {
        return page;
        // TODO smarter way to clean up a page reference
//            /* Ancestry.com tends to use semi-colons in its citations */
//            .replace(';', ',')
//            .replaceAll("Page:", "p.")
//            .replaceAll("Family History Library Film", "FHL microfilm")
//            .replaceAll("Family History Film", "FHL microfilm");
    }
}
