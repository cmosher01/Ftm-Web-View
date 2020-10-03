package nu.mine.mosher.gedcom;

import java.util.Objects;

public class StringUtils {
    public static String cleanCitationElement(final String s) {
// TODO re-examine this: replaceFirst("[.,;:]$","");
        return safe(s);
    }

    public static boolean is(final String s) {
        return Objects.nonNull(s) && !s.isBlank();
    }

    public static String safe(final String s) {
        if (Objects.isNull(s) || s.isBlank()) {
            return "";
        }
        return s.trim();
    }

    public static String safe(final Object object) {
        if (Objects.isNull(object)) {
            return "";
        }
        return safe(object.toString());
    }

    public static String no(String s, String name) {
        if (s.isBlank()) {
            return "no "+name;
        }
        return s;
    }
}
