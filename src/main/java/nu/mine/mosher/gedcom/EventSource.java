package nu.mine.mosher.gedcom;



import org.apache.tika.exception.TikaException;
import org.jdom2.JDOMException;
import org.slf4j.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.*;

import static nu.mine.mosher.gedcom.FtmViewerServlet.urlQueryTreeSource;
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

    public void appendTo(final Element parent, final IndexedDatabase indexedDatabase) throws JDOMException, SAXException, TikaException, IOException, TransformerException, ParserConfigurationException {
        if (!teiStyleIfPossible(safe(footnote), parent)) {
            if (!teiStyleIfPossible(safe(page), parent)) {
                if (!safe(footnote()).isBlank()) {
                    final Node note = HtmlUtils.tika(footnote());
                    final Node newNote = parent.getOwnerDocument().importNode(note, true);
                    parent.appendChild(newNote);
                } else {
                    appendStandardCitation(parent);
                }
            }
        }

        appendLinksIcons(parent, indexedDatabase);
    }

    private void appendStandardCitation(Element parent) {
        // TODO look for URLs

        final String author = cleanCitationElement(author());
        if (is(author)) {
            t(parent, author);
        }

        final String title = filterTitle(cleanCitationElement(title()));
        if (is(author) && is(title)) {
            t(parent, ", ");
        }

        final String place = no(safe(placePub()), "place");
        final String pub = no(safe(pub()), "publisher");
        final String date = no(safe(datePub()), "date");

        if (is(title)) {
            final boolean published = !safe(pub()).isBlank();
            if (published) {
                final Element cite = e(parent, "cite");
                cite.setTextContent(title());
            } else {
                final Element unpub = e(parent, "span");
                unpub.setTextContent("\u201C"+title()+"\u201D");
            }
        }

        t(parent, String.format(" (%s: %s, %s)", place, pub, date));

        final String page = filterPage(cleanCitationElement(page()));
        if (is(page)) {
            t(parent, ", "+page);
            // TODO if page ends with a period, then don't append one below:
        }

        t(parent, ".");
    }

    private void appendLinksIcons(Element parent, IndexedDatabase indexedDatabase) {
        media().forEach(m -> {
            t(parent, " ");
            final Element a = e(parent, "a");
            final String dirMedia = indexedDatabase.dirMedia();
            final String path = m.file().replaceAll("\\\\", "/").replaceFirst("^~/", dirMedia + "/");
            a.setAttribute("href", "../ftm/"+path);
            a.setTextContent(new String(Character.toChars(0x1F5BB)));
        });

        if (!safe(comment).isBlank()) {
            t(parent, " ");
            final Element a = e(parent, "a");
            a.setAttribute("href", urlQueryTreeSource(indexedDatabase, pkidCitation));
            a.setTextContent(new String(Character.toChars(0x1F5D0)));
        }

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
