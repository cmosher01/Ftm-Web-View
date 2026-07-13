package nu.mine.mosher.genealogy;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;



public class FtmPlaceTest {
    private static void assertHier(List<String> expected, String in) {
        var uut = FtmPlace.fromFtmPlace(in);
        assertEquals(expected, uut.getHierarchy());
    }

    /*
        /////England/3251/0.917865/-0.02550045
        /////USA/2//

        ////New York/USA/35/0.7461816/-1.295425

        ///Steuben/New York/USA/2794/0.73765/-1.35063
        ///Yorkshire//England/5292/0.9425181/-0.01923769

        //Westfield/Tioga/Pennsylvania/USA/15518/0.7316287/-1.353309
        //Stainton (near Middlesbrough)/Yorkshire//England/1701400/0.9515654/-0.0219476
        //Hartlepool/Durham//England/83443/0.9544527/-0.02116212

        /Gainsborough Studios, 222 Central Park South/Manhattan/New York/New York/USA/-11127/0.7115228/-1.291202
        /another place / with slashes | and  bars, but, resolved, in///Connecticut/USA/-9//

        /New Granada|//
        /Curaçao|/0.2129302/-1.204277
        /Hamilton, Madison, New York, USA|/0.7474722/-1.318502
        /Room 401, Flint Hall, Syracuse University/Syracuse/Onondaga/New York/USA/11269/0.7513314/-1.329023
        /Place, Name w/some slash/es | and, vertical | bars|//
    */

    @Test
    void nominal() {
        assertHier(
            List.of("Orangetown", "Rockland", "New York", "USA"),
            "//Orangetown/Rockland/New York/USA/11603/0.716503/-1.290635");
    }

    @Test
    void nominalP4() {
        assertHier(
            List.of("England"),
            "/////England/3251/0.917865/-0.02550045");
    }

    @Test
    void nominalP3() {
        assertHier(
            List.of("New York", "USA"),
            "////New York/USA/35/0.7461816/-1.295425");
    }

    @Test
    void nominalP2() {
        assertHier(
            List.of("Steuben", "New York", "USA"),
            "///Steuben/New York/USA/2794/0.73765/-1.35063");
    }

    @Test
    void nominalP1() {
        assertHier(
            List.of("Westfield", "Tioga", "Pennsylvania", "USA"),
            "//Westfield/Tioga/Pennsylvania/USA/15518/0.7316287/-1.353309");
    }

    @Test
    void nominalP0() {
        // parse (simple) CSV in p0 as elements of the hierarchy
        assertHier(
            List.of("Room 401", "Flint Hall", "Syracuse University", "Syracuse", "Onondaga", "New York", "USA"),
            "/Room 401, Flint Hall, Syracuse University/Syracuse/Onondaga/New York/USA/11269/0.7513314/-1.329023");
    }

    @Test
    void emptyP3withP2() {
        // empty internal place in input is not added to hierarchy
        assertHier(
            List.of("Yorkshire", "England"),
        "///Yorkshire//England/5292/0.9425181/-0.01923769");
    }

    @Test
    void unresolvedSingle() {
        assertHier(
            List.of("Curaçao"),
        "/Curaçao|/0.2129302/-1.204277");
    }

    @Test
    void unresolvedSingleNoCoords() {
        assertHier(
            List.of("New Granada"),
            "/New Granada|//");
    }

    @Test
    void unresolvedHier() {
        assertHier(
            List.of("Hamilton", "Madison", "New York", "USA"),
            "/Hamilton, Madison, New York, USA|/0.7474722/-1.318502");
    }

    @Test
    void slashesInP0() {
        assertHier(
            List.of("a/b/c/d", "Westfield", "Tioga", "Pennsylvania", "USA"),
            "/a/b/c/d/Westfield/Tioga/Pennsylvania/USA/15518/0.7316287/-1.353309");
    }

    @Test
    void csvSimple() {
        assertHier(
            List.of("X", "Y", "Z"),
            "/X, Y, Z|//");
    }

    @Test
    void csvInternalQuotes() {
        assertHier(
            List.of(
                "School of Art",
                "Cooper Union \"Foundation Building\" [1859]",
                "Cooper Square",
                "7 East 7th Street corner 3rd Avenue",
                "Manhattan",
                "New York",
                "New York",
                "USA"),
            "/School of Art, Cooper Union \"Foundation Building\" [1859], Cooper Square, 7 East 7th Street cor"+
                "ner 3rd Avenue/Manhattan/New York/New York/USA/-11127/0.7108623/-1.291377");
    }

    @Disabled("TODO use csv processor?")
    @Test
    void csvQuotesMaskCommas() {
        assertHier(List.of("Test \"A,B,C\" Test"), "/Test \"A,B,C\" Test|//");
    }

    @Test
    void csvPunctuation() {
        // test all ASCII punctuation but comma and double-quote
        assertHier(List.of("Test !#$%&'()*+-./:;<=>?@[\\]^_`{|}~ Test"), "/Test !#$%&'()*+-./:;<=>?@[\\]^_`{|}~ Test|//");
    }
}
