package nu.mine.mosher.gedcom;

import org.junit.jupiter.api.*;
import org.slf4j.*;

public class HtmlUtilsTest {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlUtilsTest.class);
    @Test
    void nominal() {
        LOG.info(HtmlUtils.appendHtml("<!doctype html><html><body><table><tbody><tr><td>Hello</td></tr></tbody></table></body></html>"));
        LOG.info(HtmlUtils.appendHtml(" Some text. "));
        LOG.info(HtmlUtils.appendHtml(" <div> closed element </div> "));
        LOG.info(HtmlUtils.appendHtml(" <div> element missing closing tag "));
        LOG.info(HtmlUtils.appendHtml(" text with </div> closing tag only"));
        LOG.info(HtmlUtils.appendHtml(" nominal self <br> closing tag "));
        LOG.info(HtmlUtils.appendHtml(" self <br></br> closing tag with closing tag "));
        LOG.info(HtmlUtils.appendHtml(" self <br> closing tag with </br> closing tag and content "));
        LOG.info(HtmlUtils.appendHtml(" html <html> tag in </html> doc "));
        LOG.info(HtmlUtils.appendHtml(" body <body> tag in </body> doc "));
        LOG.info(HtmlUtils.appendHtml(" two <body> body <body> tags in </body> doc </body> ument "));
        LOG.info(HtmlUtils.appendHtml(" javascript is <script> var script = 0; </script> removed "));
    }
}
