package nu.mine.mosher.gedcom;

import org.w3c.dom.Element;

import static nu.mine.mosher.gedcom.StringUtils.safe;

/*
 * Keep these in sync with css files
 */
public final class Styles {
    public enum Render {
        smaller("rend-smaller"),
        nowrap("rend-nowrap"),
        indent("rend-indent"),
        hanging("rend-hanging"),
        eqlines("rend-eqlines"),
        hi0("rend-hi-0"),
        hi1("rend-hi-1"),
        hi2("rend-hi-2"),
        hi3("rend-hi-3"),
        hiauth("rend-hi-auth"),
        hiunauth("rend-hi-unauth");

        private final String cssClass;

        Render(final String cssClass) {
            this.cssClass = cssClass;
        }

        @Override
        public String toString() {
            return this.cssClass;
        }
    }

    public enum Layout {
        cn("cn"),
        c2Wrap("c2-wrap"),
        c2Left("c2-left"),
        c2Right("c2-right"),
        indent("layout-indent");

        private final String cssClass;

        Layout(final String cssClass) {
            this.cssClass = cssClass;
        }

        @Override
        public String toString() {
            return this.cssClass;
        }
    }

    public enum Links {
        hilite("link-hilite"),
        link("link"),
        button("button");

        private final String cssClass;

        Links(final String cssClass) {
            this.cssClass = cssClass;
        }

        @Override
        public String toString() {
            return this.cssClass;
        }
    }

    public static void add(final Element element, final Enum style) {
        add(element, style.toString());
    }

    public static void add(final Element element, final String style) {
        final String orig = safe(element.getAttribute("class"));
        element.setAttribute("class", orig+" "+style);
    }
}
