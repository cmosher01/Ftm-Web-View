package nu.mine.mosher.genealogy;



import jakarta.servlet.http.HttpServletRequest;
import nu.mine.mosher.xml.*;
import org.apache.ibatis.annotations.AutomapConstructor;
import org.apache.tika.exception.TikaException;
import org.jdom2.JDOMException;
import org.slf4j.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static nu.mine.mosher.genealogy.FtmViewerServlet.urlQueryTreeSource;
import static nu.mine.mosher.genealogy.StringUtils.*;
import static nu.mine.mosher.genealogy.XmlUtils.*;


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

    String weblink,
    String note
) {
    private static final Logger LOG = LoggerFactory.getLogger(EventSource.class);
    private static final Random rand = new Random();

    public static EventSource fromNote(final String note) {
        return new EventSource(0,null,null,rand.nextInt(),null,null,note,null,null,null,null,null,null,null,null,null,new ArrayList<>(),null,null);
    }

    @AutomapConstructor
    public EventSource(int pkidSourceLink, Integer stars,String just,Integer pkidCitation,String page,String comment,String footnote,String apid,String author,String title,String placePub,String pub,String datePub,String callno,String source,String apidSource,String weblink,String note) {
        this(pkidSourceLink,stars,just,pkidCitation,page,comment,footnote,apid,author,title,placePub,pub,datePub,callno,source,apidSource,new ArrayList<>(),weblink,note);
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

    public void appendTo(final HttpServletRequest req, final Element parent, final IndexedDatabase indexedDatabase) throws JDOMException, SAXException, TikaException, IOException, TransformerException, ParserConfigurationException, URISyntaxException {
        if (!teiStyleIfPossible(req, safe(footnote()), parent)) {
            if (!teiStyleIfPossible(req, safe(page()), parent)) {
                final Node node;
                if (!safe(footnote()).isBlank()) {
                    node = HtmlUtils.tika(footnote());
                } else {
                    node = buildStandardCitationAsTei(req);
                }
                if (safe(node.getTextContent()).isBlank()) {
                    t(node, "[this note is blank]");
                }
                parent.appendChild(parent.getOwnerDocument().importNode(node, true));
            }
        }

        if (!safe(note()).isBlank()) {
            final Node node = HtmlUtils.tika(note());
            if (safe(node.getTextContent()).isBlank()) {
                t(node, "[this note is blank]");
            } else {
                t(parent, " ");
                parent.appendChild(parent.getOwnerDocument().importNode(node, true));
            }
        }

        appendLinksIcons(parent, indexedDatabase);
    }

    private Node buildStandardCitationAsTei(final HttpServletRequest req) throws IOException, TransformerException, ParserConfigurationException, SAXException {
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
            textWithTeiLinks(ePubPlace, sPlace);
            t(eBibl, ": ");
            final Element ePub = te(eBibl, "publisher");
            textWithTeiLinks(ePub, sPub);
            t(eBibl, ", ");
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

        TeiToXhtml5.runPipeline(req, p);

        return nodeFromDoc(p.accessDom());
    }

    private void appendLinksIcons(final Element parent, final IndexedDatabase indexedDatabase) throws URISyntaxException {
        media().forEach(m -> {
            t(parent, " ");
            final Element a = e(parent, "a");
            final String dirMedia = indexedDatabase.dirMedia();
            final String path = m.file().replaceAll("\\\\", "/").replaceFirst("^~/", dirMedia + "/");
            a.setAttribute("href", "../ftm/"+path);
            a.setTextContent(new String(Character.toChars(0x1F5BB)));
        });

        if (!safe(comment()).isBlank()) {
            t(parent, " ");
            final Element a = e(parent, "a");
            a.setAttribute("href", urlQueryTreeSource(indexedDatabase, pkidCitation()));
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
}
