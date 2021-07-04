package nu.mine.mosher.genealogy;



import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;



public class PlaceListAbbrevTest {
    @Test
    void nominal() {
        final List<String> w = h("Wilmington, New Castle, Delaware, USA");
        final List<String> w2 = h("Wilmington");

        final List<List<String>> win = List.of(w,w);
        final List<List<String>> wa = new PlaceListAbbrev().abbrev(win);
        dump(win, wa);

        assertEquals(List.of(w,w2), wa);
    }

    private static void dump(final List<List<String>> win, final List<List<String>> wout) {
        for (int i = 0; i < Math.min(win.size(), wout.size()); ++i) {
            System.out.println("" + win.get(i) + " --> " + wout.get(i));
        }
    }

    @Test
    void nominal2() {
        final List<String> w = h("Wilmington, New Castle, Delaware, USA");
        final List<String> w2 = h("Wilmington");

        final List<List<String>> wa = new PlaceListAbbrev().abbrev(List.of(w,w,w));

        assertEquals(List.of(w,w2,w2), wa);
    }

    @Test
    void dup() {
        final List<String> w = h("Wilmington, New Castle, Delaware, USA");
        final List<String> w2 = h("Wilmington, New Hanover, North Carolina, USA");
        final List<String> w3 = h("Wilmington, New Castle");

        final List<List<String>> win = List.of(w, w2, w);
        final List<List<String>> wa = new PlaceListAbbrev().abbrev(win);
        dump(win, wa);

        assertEquals(List.of(w,w2,w3), wa);
    }

    @Test
    void sub() {
        final List<String> w = h("Wilmington, New Castle, Delaware, USA");
        final List<String> w2 = h("New Castle, Delaware, USA");
        final List<String> w3 = h("New Castle");

        final List<List<String>> win = List.of(w, w2);
        final List<List<String>> wa = new PlaceListAbbrev().abbrev(win);
        dump(win, wa);

        assertEquals(List.of(w,w3), wa);
    }

    @Test
    void sub1() {
        final List<String> w = h("Wilmington, New Castle, Delaware, USA");
        final List<String> w2 = h("Brandywine, New Castle, Delaware, USA");
        final List<String> w3 = h("Brandywine, New Castle");

        final List<List<String>> win = List.of(w, w2);
        final List<List<String>> wa = new PlaceListAbbrev().abbrev(win);
        dump(win, wa);

        assertEquals(List.of(w,w3), wa);
    }

    @Test
    void bug1() {
        final List<String> p1 = h("Oneida City Hospital, Oneida, Madison, New York, USA");
        final List<String> p2 = h("Spring Street, Hamilton, Madison, New York, USA");

        final List<String> p2a = h("Spring Street, Hamilton, Madison");
        final List<String> p2b = h("Spring Street");

        final List<List<String>> win = List.of(p1, p2, p2);
        final List<List<String>> wa = new PlaceListAbbrev().abbrev(win);
        dump(win, wa);

        assertEquals(List.of(p1,p2a,p2b), wa);
    }

    private static List<String> h(final String s) {
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isBlank()).toList();
    }
}
