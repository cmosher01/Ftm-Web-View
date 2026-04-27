package nu.mine.mosher.genealogy.xy.metrics;

import org.junit.jupiter.api.Test;

import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.*;

public final class HeadlessWordWrapTest {
    // visually inspect output to verify word wrap
    @Test
    void nominal() {
        final var noto18plain = new HashMap<TextAttribute, Object>();
        noto18plain.put(TextAttribute.FAMILY, "Noto Sans");
        noto18plain.put(TextAttribute.SIZE, 18);

        final var rawVanGough = """
            It\u2019s believed that Vincent van Gogh painted his best works \
            during the longwordlongwordlongwordlongword two\u2010year period he spent in Provence. Here is where he \
            painted \u201CThe Starry Night\u201D\u2014which some consider to be his greatest \
            work of all. However, as his artistic brilliance reached new \
            heights in Provence, his physical and mental health plummeted.\
            """;

        final var vanGogh = new AttributedString(rawVanGough, noto18plain);
        vanGogh.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, 19, 19+16);
        vanGogh.addAttribute(TextAttribute.SIZE, 24, 48, 48+4);
        vanGogh.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, 36, 37);

        final var maxWidth = 199.0F;
        final var w = new HeadlessWordWrap(vanGogh, maxWidth);
        dump(maxWidth, w, rawVanGough);
    }

    private static void dump(float maxWidth, HeadlessWordWrap w, String rawVanGough) {
        System.out.printf("max width given: %13.8f\n", maxWidth);
        final var endings = w.endings();
        final var instrs = w.instrumentation();
        int posBegin = 0;
        double lastY = 0.0D;
        for (int i = 0; i < endings.size(); ++i) {
            final var posEnd = endings.get(i);
            final var instr = instrs.get(i);
            final var s = rawVanGough.substring(posBegin, posEnd);
            posBegin = posEnd;

            System.out.printf("%3d. %13.8f  %13.8f  %13.8f  %13.8f  %13.8f  %13.8f  %13.8f  %3d  \"%s\"\n",
                    i,
                    instr.advanceVisible,
                    instr.ascent,
                    instr.descent,
                    instr.leading,
                    instr.ascent+instr.descent+instr.leading,
                    instr.baselineY-lastY,
                    instr.baselineY,
                    posEnd,
                    s);

            lastY = instr.baselineY;
        }
        System.out.printf("%3d  %13.8f  %13.8f\n", endings.size(), w.width(), w.height());
    }

    @Test
    void other() {
//        final var raw = "//Bethnal Green/Middlesex//England/85831/0.8991355/-0.0008726646";
//        final var raw = "//Westminster/Middlesex//England/85875/0.8988445/-0.002036218";
//        final var raw = "William (Westminster printer) Withers";
        final var raw = "William (and Sarah of Pitfield St.) Withers";
        final var maxWidth = 128F;
        final var a = Map.of(
            TextAttribute.FAMILY, "Noto Sans",
            TextAttribute.SIZE, 8,
            TextAttribute.KERNING, TextAttribute.KERNING_ON);
        final var atrs = new AttributedString(raw, a);
//        atrs.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, 0, 33);
        final var w = new HeadlessWordWrap(atrs, maxWidth);
        dump(maxWidth, w, raw);
    }
}
