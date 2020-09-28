package nu.mine.mosher.gedcom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.safety.*;

import java.nio.charset.StandardCharsets;

public class HtmlUtils {
    public static String appendHtml(final String html) {
        final Cleaner cleaner = new Cleaner(Whitelist.relaxed());
        final Document document = cleaner.clean(Jsoup.parseBodyFragment(html));
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        document.outputSettings().charset(StandardCharsets.UTF_8);
        document.outputSettings().prettyPrint(false);

        final Element body = document.getElementsByTag("body").first();
        return body.tagName("div").outerHtml();
    }

    public static boolean looksLikeHtml(final String s) {
        final String low = s.trim().toLowerCase();
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
}
