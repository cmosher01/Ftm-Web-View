package nu.mine.mosher.genealogy;

import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.*;

import java.net.URL;
import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record GeoCoords(
    double radiansLatitude,
    double radiansLongitude,
    double degreesLatitude,
    double degreesLongitude,
    URL urlGoogleMaps) {
    private static final Logger LOG = LoggerFactory.getLogger(GeoCoords.class);

    public static Optional<GeoCoords> parse(final String radLat, final String radLon) {
        final Optional<Double> odrLat = parseRadians(radLat);
        if (odrLat.isEmpty()) {
            return Optional.empty();
        }
        final Optional<Double> oddLat = degreesFromRadians(odrLat);
        if (oddLat.isEmpty()) {
            return Optional.empty();
        }

        final Optional<Double> odrLon = parseRadians(radLon);
        if (odrLon.isEmpty()) {
            return Optional.empty();
        }
        final Optional<Double> oddLon = degreesFromRadians(odrLon);
        if (oddLon.isEmpty()) {
            return Optional.empty();
        }

        final Optional<URL> ou = buildUrl(oddLat.get(), oddLon.get());
        return ou.map(url -> new GeoCoords(odrLat.get(), odrLon.get(), oddLat.get(), oddLon.get(), url));
    }

    private static Optional<URL> buildUrl(final double oddLat, final double oddLon) {
        /*
         *      https://www.google.com/maps/search/?api=1&query=-33.712206,150.311941
         */

        try {
            final String pair = String.format("%f,%f", oddLat, oddLon);
            return Optional.of(
                new URIBuilder().
                setScheme("https").
                setHost("www.google.com").
                setPathSegments("maps", "search", "").
                setParameter("api", "1").
                setParameter("query", pair).
                build().
                toURL());
        } catch (final Throwable e) {
            LOG.warn("Tried to build invalid URL", e);
        }
        return Optional.empty();
    }

    private static Optional<Double> degreesFromRadians(final Optional<Double> radians) {
        /*
        I performed extensive testing of coordinates all around the globe.
        I pinpointed landmarks in FTM, using the map on the "Places" tab.
        And then I viewed all of them here in Ftm-Web-View, linking to Google Maps,
        and they were all identically displayed on Google Maps as they were on FTM's map.
         */
        return radians.map(Math::toDegrees);
    }

    private static Optional<Double> parseRadians(final String s) {
        if (Objects.isNull(s) || s.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(s.strip()));
        } catch (final Throwable e) {
            LOG.warn("Invalid number format for geographic coordinate: {}", s, e);
            return Optional.empty();
        }
    }
}
