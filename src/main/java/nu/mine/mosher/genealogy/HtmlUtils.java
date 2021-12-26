package nu.mine.mosher.genealogy;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.*;
import org.jdom2.JDOMException;
import org.jdom2.input.sax.SAXHandler;
import org.jdom2.output.*;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.*;
import org.jsoup.safety.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static nu.mine.mosher.genealogy.XmlUtils.XHTML_NAMESPACE;

public class HtmlUtils {
    public static org.w3c.dom.Node html(final String in) {
        final var jsoup = Jsoup.parseBodyFragment(in);
        configureJsoup(jsoup);
        return W3CDom.convert(jsoup).getFirstChild().getFirstChild().getNextSibling();
    }

    private static void configureJsoup(final Document jsoup) {
        jsoup.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        jsoup.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
        jsoup.outputSettings().charset(StandardCharsets.UTF_8);
        jsoup.outputSettings().prettyPrint(true);

        jsoup.getElementsByTag("body").first().tagName("div").attr("xmlns", XmlUtils.XHTML_NAMESPACE);
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
        domOutputter.setFormat(Format.getCompactFormat());
        final org.w3c.dom.Node tikaXml = domOutputter.output(jdoc).getFirstChild();
        return XmlUtils.cleanTikaXml(tikaXml);
    }
}
