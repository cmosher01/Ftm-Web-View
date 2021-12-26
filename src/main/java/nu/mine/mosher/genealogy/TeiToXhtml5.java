package nu.mine.mosher.genealogy;

import jakarta.servlet.http.HttpServletRequest;
import nu.mine.mosher.xml.XsltPipeline;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.*;

public class TeiToXhtml5 {
    public static void transform(final HttpServletRequest req, final BufferedInputStream inTei, final Node appendTo) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        final XsltPipeline pipeline = new XsltPipeline();
        pipeline.dom(inTei);
        runPipeline(req, pipeline);
        appendTo.appendChild(appendTo.getOwnerDocument().importNode(pipeline.accessDom().getFirstChild(), true));
    }

    public static void runPipeline(final HttpServletRequest req, final XsltPipeline pipeline) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        pipeline.xslt(lib(req,  "xslt/tei-copyOf.xslt"));
        pipeline.xslt(lib(req,  "xslt/tei-facs.xslt"));
        pipeline.xslt(lib(req,  "xslt/tei-norm-text.xslt"));
        pipeline.xslt(lib(req,  "xslt/tei-teiattr.xslt"));
        pipeline.xslt(lib(req,  "xslt/tei-xhtml-specific.xslt"));
        pipeline.xslt(lib(req,  "xslt/tei-xhtml-general.xslt"));
    }

    private static URL lib(HttpServletRequest req, final String path) throws MalformedURLException {
        return req.getServletContext().getResource("/WEB-INF/" + path);
    }
}
