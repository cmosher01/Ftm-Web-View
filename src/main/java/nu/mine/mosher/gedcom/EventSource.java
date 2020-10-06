package nu.mine.mosher.gedcom;



import nu.mine.mosher.xml.*;
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
    String source, // MasterSource.Comments TODO what to do with this?
    String apidSource,

    List<MediaFile> media,

    String weblink
) {
    private static final Logger LOG = LoggerFactory.getLogger(EventSource.class);
    private static final Random rand = new Random();

    public EventSource() {
        this(0,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,new ArrayList<>(),null);
    }

    public EventSource(final String note) {
        this(0,null,null,rand.nextInt(),null,null,note,null,null,null,null,null,null,null,null,null,new ArrayList<>(),null);
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
                final Node node;
                if (!safe(footnote()).isBlank()) {
                    node = HtmlUtils.tika(footnote());
                } else {
                    node = buildStandardCitationAsTei();
                }
                parent.appendChild(parent.getOwnerDocument().importNode(node, true));
            }
        }

        appendLinksIcons(parent, indexedDatabase);
    }

    private static final String TEI_NAMESPACE = "http://www.tei-c.org/ns/1.0";

    private Node buildStandardCitationAsTei() throws IOException, TransformerException, ParserConfigurationException, SAXException {
        final boolean published = !safe(pub()).isBlank();
        final String sAuthor = safe(author());
        final String sTitle = safe(title());
        final String sPlace = no(safe(placePub()), "place");
        final String sPub = no(safe(pub()), "publisher");
        final String sDate = no(safe(datePub()), "date");
        final String sPage = safe(page());


        final XsltPipeline p = new XsltPipeline();
        p.dom();
        final Node doc = p.accessDom();
        final Element eTEI = te(doc, "TEI");
        final Element eTeiHeader = te(eTEI, "teiHeader");
        final Element eFileDesc = te(eTeiHeader, "fileDesc");
        te(eFileDesc, "titleStmt");
        te(eFileDesc, "publicationStmt");
        te(eFileDesc, "sourceDesc");
        final Element eText = te(eTEI, "text");
        final Element eBody = te(eText, "body");
        final Element eAb = te(eBody, "ab");
        final Element eBibl = te(eAb, "bibl");

        final Element eAuthor = te(eBibl, "author");
        eAuthor.setTextContent(sAuthor);

        if (is(sAuthor) && is(sTitle)) {
            t(eBibl, ", ");
        }

        if (is(sTitle)) {
            final Element eTitle = te(eBibl, "title");
            if (published) {
                eTitle.setAttribute("level", "m");
                eTitle.setTextContent(sTitle);
            } else {
                eTitle.setAttribute("level", "u");
                eTitle.setTextContent("\u201C"+sTitle+"\u201D");
            }
        }

        if (published) {
            t(eBibl, " (");
            final Element ePubPlace = te(eBibl, "pubPlace");
            ePubPlace.setTextContent(sPlace);
            t(eBibl, ": ");
            final Element ePublisher = te(eBibl, "publisher");
            ePublisher.setTextContent(sPub);
            t(eBibl, ": ");
            final Element eDate = te(eBibl, "date");
            eDate.setTextContent(sDate);
            t(eBibl, ")");
        }

        if (is(sPage)) {
            t(eBibl, ", ");
            final Element eCitedRange = te(eBibl, "citedRange");
            eCitedRange.setTextContent(sPage);
            if (!sPage.endsWith(".")) {
                t(eBibl, ".");
            }
        } else {
            t(eBibl, ".");
        }

        TeiToXhtml5.runPipeline(p);

        return nodeFromDoc(p.accessDom());
    }

    private void appendLinksIcons(final Element parent, final IndexedDatabase indexedDatabase) {
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

//    TODO links
//    public static String links(final String s) {
//        return s.
//            replaceAll("\\b(\\w+?://\\S+?)(\\s|[<>{}\"|\\\\^`\\]]|$)", "<a href=\"$1\">$1</a>$2").
//            replaceAll("([^/.]www\\.[a-zA-Z]\\S*?)(\\s|[<>{}\"|\\\\^`\\]]|$)", "<a href=\"http://$1\">$1</a>$2");
//    }

    public static Element te(final Node parent, final String tag) {
        final Document dom;
        if (parent instanceof Document) {
            dom = (Document)parent;
        } else {
            dom = parent.getOwnerDocument();
        }

        final Element element = dom.createElementNS(TEI_NAMESPACE, tag);
        parent.appendChild(element);
        return element;
    }
}
