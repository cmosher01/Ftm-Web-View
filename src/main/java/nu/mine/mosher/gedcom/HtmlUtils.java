package nu.mine.mosher.gedcom;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.*;
import org.jdom2.JDOMException;
import org.jdom2.input.sax.SAXHandler;
import org.jdom2.output.DOMOutputter;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.*;
import org.jsoup.safety.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static nu.mine.mosher.gedcom.XmlUtils.XHTML_NAMESPACE;

public class HtmlUtils {
    public static org.w3c.dom.Node  html(final String input) {
        final Cleaner cleaner = new Cleaner(Whitelist.relaxed());
        final Document document = cleaner.clean(Jsoup.parseBodyFragment(input));
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        document.outputSettings().charset(StandardCharsets.UTF_8);
        document.outputSettings().prettyPrint(false);

        document.getElementsByTag("body").first().tagName("div").attr("xmlns", XHTML_NAMESPACE);

        final org.w3c.dom.Document dom = W3CDom.convert(document);

        return dom.getFirstChild().getFirstChild().getNextSibling();
    }

    public static boolean looksLikeHtml(final String input) {
        final String low = input.trim().toLowerCase();
        return
            low.startsWith("<html") ||
            low.startsWith("<!doctype html") ||
            low.contains("<table") ||
            low.contains("<img") ||
            low.contains("<p>") ||
            low.contains("<br") ||
            low.contains("<div") ||
            low.contains("<span") ||
            low.contains("<i>") ||
            low.contains("<u>") ||
            low.contains("<hr") ||
            low.contains(" href=");
    }

    public static org.w3c.dom.Node tika(final String input) throws TikaException, SAXException, IOException, JDOMException, TransformerException, ParserConfigurationException {
        final String safe;
        if (Objects.isNull(input) || input.isBlank()) {
            safe = "";
        } else {
            safe = input;
        }
        final InputStream instream = new ByteArrayInputStream(safe.getBytes(StandardCharsets.UTF_8));
        final SAXHandler handler = new SAXHandler();
        final AutoDetectParser parser = new AutoDetectParser();
        final Metadata metadata = new Metadata();
        metadata.add(Metadata.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
        parser.parse(instream, handler, metadata, new ParseContext());
        final org.jdom2.Document jdoc = handler.getDocument();
        final DOMOutputter domOutputter = new DOMOutputter();
        final org.w3c.dom.Node tikaXml = domOutputter.output(jdoc).getFirstChild();
        return XmlUtils.cleanTikaXml(tikaXml);
    }
}
