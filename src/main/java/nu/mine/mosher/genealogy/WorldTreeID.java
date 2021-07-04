package nu.mine.mosher.genealogy;

import java.net.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

/**
 * Represents IDs in some de-facto standard global family trees, such as WikiTree.com , or FamilySearch.org .
 *
 * TODO allow these IDs to be specified in the URL, and search for the corresponding record
 */
public enum WorldTreeID {
    ANCESTRY(WorldTreeID::pAncestry),
    FAMILYSEARCH(WorldTreeID::pFamilySearch),
    // TODO Ancestral File  https://www.familysearch.org/ark:/61903/2:1:MWB4-W1C
    WIKITREE(WorldTreeID::pWikiTree),
    GENI(WorldTreeID::pGeni),
    GENEANET(WorldTreeID::pGeneanet),
    MYHERITAGE(WorldTreeID::pMyHeritage),
    RODOVID(WorldTreeID::pRodovid),
    WERELATE(WorldTreeID::pWeRelate)
    ;

    //  ??? https://www.genesreunited.co.uk/relatives/profile/overview/683682479




    public Optional<URL> urlFor(final String value) {
        return build(this.parser.apply(value));
    }



    private final Function<String, String> parser;

    WorldTreeID(final Function<String, String> parser) {
        this.parser = parser;
    }






    private static String pGeneanet(final String value) {
        //  https://gw.geneanet.org/cmosher01_w?p=hugh&n=mosher
        //  https://gw.geneanet.org/{value}
        return String.format("https://www.geneanet.org/%s", value);
    }

    private static String pGeni(final String value) {
        //  https://www.geni.com/people/Henry-Harrison/6000000145359867849
        //  https://www.geni.com/people/{value}
        return String.format("https://www.geni.com/people/%s", value);
    }

    private static String pWikiTree(final String value) {
        //  https://www.wikitree.com/wiki/Mosher-171
        //  https://www.wikitree.com/wiki/{value}
        return String.format("https://www.wikitree.com/wiki/%s", value);
    }

    private static String pFamilySearch(final String value) {
        //  https://www.familysearch.org/tree/person/details/LBF9-6X3
        //  https://www.familysearch.org/tree/person/details/{value}
        return String.format("https://www.familysearch.org/tree/person/details/%s", value);
    }

    // Note: this one is handled differently, because we can get the ID from the sync_person table
    // pass in here as, for example, "115769354/100146083641", in the form "T/P" (tree ID, person ID)
    private static String pAncestry(final String value) {
        final Pattern pattern = Pattern.compile("^\\d+/\\d+$");
        final Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            return "";
        }

        //  https://www.ancestry.com/family-tree/person/tree/{T}/person/{P}/
        return String.format("https://www.ancestry.com/family-tree/person/tree/%s/person/%s/", matcher.group(1), matcher.group(2));
    }

    private static String pMyHeritage(final String value) {
        //  https://www.myheritage.com/person-2000101_552123051_552123051/
        //  https://www.myheritage.com/{value}/
        return String.format("https://www.myheritage.com/wiki/%s/", value);
    }

    private static String pRodovid(final String value) {
        //  https://en.rodovid.org/wk/Person:624297
        //  https://en.rodovid.org/wk/Person:{value}
        return String.format("https://en.rodovid.org/wk/Person:%s", value);
    }


    private static String pWeRelate(final String value) {
        //  https://www.werelate.org/wiki/Person:Hugh_Mosher_(6)
        //  https://www.werelate.org/wiki/Person:{value}
        return String.format("https://www.werelate.org/wiki/Person:%s", value);
    }



    private static Optional<URL> build(final String url) {
        if (Objects.isNull(url) || url.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(URI.create(url).toURL());
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }
}
