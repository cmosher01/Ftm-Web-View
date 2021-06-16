package nu.mine.mosher.gedcom;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.*;
import org.slf4j.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Auth {
    private static final Logger LOG = LoggerFactory.getLogger(Auth.class);
    private static final NetHttpTransport TRANSPORT = new NetHttpTransport();

    public static class Failure extends Exception {
        Failure(final String message, final String token) {
            super("Authentication/authorization error: "+message+", for token "+token);
        }
    }

    public record RbacRole(String email, boolean loggedIn, boolean authorized) {
    }

    public static RbacRole auth(final HttpServletRequest req) {
        try {
            final String idStringOrNull = cookie(req, "idtoken");
            if (Objects.isNull(idStringOrNull) || idStringOrNull.isBlank()) {
//                throw new Failure("no idtoken cookie found", "");
                return new RbacRole("", false, false);
            }
            final GoogleIdToken idTokenOrNull = tokenVerifier().verify(idStringOrNull);
            if (Objects.isNull(idTokenOrNull)) {
                throw new Failure("token failed verification", idStringOrNull);
            }
            final String email = idTokenOrNull.getPayload().getEmail();
            if (Objects.isNull(email) || email.isBlank() || email.equalsIgnoreCase("error")) {
                throw new Failure("invalid email", idStringOrNull);
            }
            LOG.info("verified login: token={}, email={}", idTokenOrNull, email);
            return new RbacRole(email, true, emailIsAuthorized(email));
        } catch (final Throwable e) {
            LOG.warn(e.getMessage(), e);
            return new RbacRole("", false, false);
        }
    }

    private static String cookie(final HttpServletRequest req, final String name) {
        final Cookie[] cookies = req.getCookies();
        if (Objects.isNull(cookies) || cookies.length == 0) {
            return null;
        }
        final Optional<Cookie> optCookie = Arrays.stream(cookies).filter(c -> c.getName().equals(name)).findAny();
        if (optCookie.isEmpty()) {
            return null;
        }
        return optCookie.get().getValue();
    }

    private static GoogleIdTokenVerifier tokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(TRANSPORT, GsonFactory.getDefaultInstance()).setAudience(Collections.singleton(googleClientID())).build();
    }

    private static boolean emailIsAuthorized(final String email) {
        final boolean authorized = emailIsInFile(email);
        if (authorized) {
            LOG.warn("Authorized user: {}", email);
        }
        return authorized;
    }

    private static boolean emailIsInFile(final String email) {
        try {
            final String sdirDbs = Optional.ofNullable(System.getenv("ftm_dir")).orElse("/srv");
            final Path dirDbs = Path.of(sdirDbs).toAbsolutePath().normalize();
            return Files.lines(dirDbs.resolve("authorized.emails")).collect(Collectors.toSet()).contains(email);
        } catch (final Throwable e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
    }

    private static String googleClientID() {
        return System.getenv("CLIENT_ID");
    }
}
