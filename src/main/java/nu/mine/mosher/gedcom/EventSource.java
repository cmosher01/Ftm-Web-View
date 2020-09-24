package nu.mine.mosher.gedcom;



import org.slf4j.*;
import org.w3c.dom.Element;

import java.io.File;
import java.util.*;

import static nu.mine.mosher.gedcom.StringUtils.*;
import static nu.mine.mosher.gedcom.XmlUtils.*;


public record EventSource(
    int pkidSourceLink,
    Integer stars,
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
    String template,

    List<MediaFile> media
) {
    private static final Logger LOG = LoggerFactory.getLogger(EventSource.class);

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

    public void appendTo(Element divCitation, IndexedDatabase indexedDatabase) {
        // TODO use this.page to determine if TEI <bibl> or not
        // TODO look for URLs

        final String author = cleanCitationElement(author());
        if (is(author)) {
            t(divCitation, author);
        }

        final String title = filterTitle(cleanCitationElement(title()));
        if (is(author) && is(title)) {
            t(divCitation, ", ");
        }

        if (is(title)) {
            final Element cite = e(divCitation, "cite");
            cite.setTextContent(title());
        }

        final String place = no(safe(placePub()), "place");
        final String pub = no(safe(pub()), "publisher");
        final String date = no(safe(datePub()), "date");

        t(divCitation, String.format(" (%s: %s, %s)", place, pub, date));

        final String page = filterPage(cleanCitationElement(page()));
        if (is(page)) {
            t(divCitation, ", "+page);
        }

        t(divCitation, ".");

        final Optional<AncestryPersona> optAncestry = AncestryPersona.of(apid());
        if (optAncestry.isPresent()) {
            final AncestryPersona ancestry = optAncestry.get();
            if (ancestry.isLink()) {
                t(divCitation, " ");
                ancestry.appendAsAHref(divCitation);
            }
        }

        LOG.debug("============================================================ {}    [#media: {}]", title(), media().size());

        media().forEach(m -> {
            t(divCitation, " ");
            final Element a = e(divCitation, "a");
            final String dirMedia = indexedDatabase.dirMedia();
            final String path = m.file().replaceAll("\\\\", "/").replaceFirst("^~/", dirMedia + "/");
            a.setAttribute("href", "../ftm/"+path);
            a.setTextContent(new String(Character.toChars(0x1F5BB)));

        });

        // TODO: notes
    }

    private static String filterTitle(final String title) {
        return title;
        // TODO ? return title.replaceAll("Web:\\s*", "");
    }

    private static String filterPage(final String page) {
        return page
            /* Ancestry.com tends to use semi-colons in its citations */
            .replace(';', ',')
            .replaceAll("Page:", "p.")
            .replaceAll("Family History Library Film", "FHL microfilm")
            .replaceAll("Family History Film", "FHL microfilm");
    }
}
