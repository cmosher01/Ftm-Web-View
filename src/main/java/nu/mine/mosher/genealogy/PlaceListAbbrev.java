package nu.mine.mosher.genealogy;


import org.slf4j.*;

import java.util.*;



public class PlaceListAbbrev {
    private static final Logger LOG = LoggerFactory.getLogger(PlaceListAbbrev.class);

    private final Map<List<String>, List<String>> mapPlaceToAbbrev = new HashMap<>();
    private final Map<List<String>, List<String>> mapAbbrevToPlace = new HashMap<>();
    private final List<List<String>> dupAbbrevs = new ArrayList<>();

    public List<List<String>> abbrev(List<List<String>> places) {
        for (final List<String> place : places) {
            if (1 < place.size()) {
                for (int i = place.size()-2; 0 <= i; --i) {
                    know(place.subList(i,place.size()));
                }
            }
        }
        LOG.debug("abbrev-->place: {}", this.mapAbbrevToPlace);
        LOG.debug("place-->abbrev: {}", this.mapPlaceToAbbrev);

        final List<List<String>> ret = new ArrayList<>();
        final Set<List<String>> seen = new HashSet<>();
        for (final List<String> place : places) {
            List<String> p = new ArrayList<>(place);
            abbrevIfSeen(ret, seen, place, new ArrayList<>(), p);
        }
        LOG.debug("seen: {}", seen);
        return ret;
    }

    private void know(final List<String> place) {
        final List<String> abbrev = place.subList(0, 1);
        if (!dupAbbrevs.contains(abbrev)) {
            if (mapAbbrevToPlace.containsKey(abbrev) && !mapAbbrevToPlace.get(abbrev).equals(place)) {
                mapPlaceToAbbrev.remove(mapAbbrevToPlace.get(abbrev));
                mapAbbrevToPlace.remove(abbrev);
                dupAbbrevs.add(abbrev);
            } else {
                mapAbbrevToPlace.put(abbrev, place);
                mapPlaceToAbbrev.put(place, abbrev);
            }
        }
    }

    private void abbrevIfSeen(
        final List<List<String>> ret,
        final Set<List<String>> seen,
        final List<String> place,
        final List<String> pre,
        final List<String> p) {
        if ((seen.contains(place) || seen.contains(p)) && mapPlaceToAbbrev.containsKey(p)) {
            pre.addAll(mapPlaceToAbbrev.get(p));
            ret.add(pre);
        } else {
            if (p.size() <= 1) {
                ret.add(place);
            } else {
                pre.add(p.get(0));
                abbrevIfSeen(ret, seen, place, pre, p.subList(1, p.size()));
            }
            seen(p, seen);
        }
    }

    private static void seen(final List<String> place, final Set<List<String>> seen) {
        for (int i = place.size()-1; 0 <= i; --i) {
            seen.add(place.subList(i,place.size()));
        }
    }
}
