package nu.mine.mosher.gedcom;

import nu.mine.mosher.xml.TeiToXhtml5;
import org.slf4j.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class XmlUtils {
    public static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    private static final Logger LOG = LoggerFactory.getLogger(XmlUtils.class);

    public static Element e(final Node parent, final String tag) {
        final Document dom;
        if (parent instanceof Document) {
            dom = (Document)parent;
        } else {
            dom = parent.getOwnerDocument();
        }

        final Element element = dom.createElementNS(XHTML_NAMESPACE, tag);
        parent.appendChild(element);
        return element;
    }

    public static Text t(final Node parent, final String text) {
        final Document dom;
        if (parent instanceof Document) {
            dom = (Document)parent;
        } else {
            dom = parent.getOwnerDocument();
        }

        final Text t = dom.createTextNode(text);
        parent.appendChild(t);
        return t;
    }

    public static Document empty() throws ParserConfigurationException {
        return factory(false, Collections.emptyList()).newDocumentBuilder().newDocument();
    }

    public static DocumentBuilderFactory factory(final boolean validate, final List<URL> schemas) throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setValidating(validate);
        factory.setFeature("http://apache.org/xml/features/validation/schema", validate);

        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(false);
        factory.setIgnoringComments(false);

        factory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);
        factory.setFeature("http://apache.org/xml/features/warn-on-duplicate-entitydef", true);
        factory.setFeature("http://apache.org/xml/features/standard-uri-conformant", true);
        factory.setFeature("http://apache.org/xml/features/xinclude", true);
        factory.setFeature("http://apache.org/xml/features/validate-annotations", true);
        factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
        factory.setFeature("http://apache.org/xml/features/validation/warn-on-duplicate-attdef", true);
        factory.setFeature("http://apache.org/xml/features/validation/warn-on-undeclared-elemdef", true);
        factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);

        //These options often crash Xerces (as of 2.12.0):
        //factory.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
        //factory.setFeature("http://apache.org/xml/features/scanner/notify-builtin-refs", true);

        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", XMLConstants.W3C_XML_SCHEMA_NS_URI);

        if (!schemas.isEmpty()) {
            factory.setAttribute(
                "http://java.sun.com/xml/jaxp/properties/schemaSource",
                schemas.stream().sequential().map(URL::toExternalForm).toArray(String[]::new));
        }

        return factory;
    }

    public static void serialize(final Node dom, final BufferedOutputStream to, final boolean pretty, final boolean xmldecl) throws IOException, TransformerException {
        final DOMSource source = new DOMSource(dom, dom.getBaseURI());
        final StreamResult result = new StreamResult(to);
        result.setSystemId(dom.getBaseURI());
        final Transformer transformIdentity = TransformerFactory.newInstance().newTransformer();
        configTransformer(transformIdentity, pretty, xmldecl);
        transformIdentity.transform(source, result);
        to.flush();
    }

    public static void configTransformer(final Transformer transform, final boolean pretty, final boolean xmldecl) {
        transform.setOutputProperty(OutputKeys.METHOD, "xml");
        transform.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, xmldecl ? "no" : "yes");

        if (pretty) {
            transform.setOutputProperty(OutputKeys.INDENT, "yes");
            transform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }
    }

    public static boolean looksLikeTei(final String s) {
        return s.startsWith("<bibl") || s.startsWith("<text") || s.contains("<teiHeader");
    }

    public static boolean teiStyleIfPossible(final String tei, Element divCitation) {
        if (!looksLikeTei(tei)) {
            return false;
        }

        if (tei.startsWith("<bibl")) {
            return teiStyleIfPossible(wrapTeiBibl(tei), divCitation);
        }

        if (tei.startsWith("<text")) {
            return teiStyleIfPossible(wrapTeiText(tei), divCitation);
        }

        if (tei.contains("<teiHeader")) {
            try {
                putTeiThroughPipeline(tei, divCitation);
            } catch (final Throwable e) {
                LOG.warn("Error in TEI document; using raw XML instead.", e);
                divCitation.setTextContent(tei);
            }
            return true;
        }

        return false;
    }

    private static void putTeiThroughPipeline(final String tei, final Element element) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        final BufferedInputStream inXml = new BufferedInputStream(new ByteArrayInputStream(tei.getBytes(StandardCharsets.UTF_8)));
        TeiToXhtml5.transform(inXml, element);
    }

    private static String wrapTeiText(final String text) {
        return
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<TEI xml:lang=\"en\" xmlns=\"http://www.tei-c.org/ns/1.0\">" +
            "<teiHeader>" +
            "<fileDesc>" +
            "<titleStmt/>" +
            "<publicationStmt/>" +
            "<sourceDesc/>" +
            "</fileDesc>" +
            "</teiHeader>" +
            text +
            "</TEI>";
    }

    private static String wrapTeiBibl(final String bibl) {
        return wrapTeiText(
            "<text>" +
            "<body>" +
            "<ab>" +
            bibl +
            "</ab>" +
            "</body>" +
            "</text>");
    }
}
