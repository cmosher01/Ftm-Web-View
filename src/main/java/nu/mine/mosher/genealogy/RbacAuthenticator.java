package nu.mine.mosher.genealogy;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

public class RbacAuthenticator {
    private static final NetHttpTransport TRANSPORT = new NetHttpTransport();
    private static final String CLIENT_ID = System.getenv("CLIENT_ID");

    private final String jwt;

    public RbacAuthenticator(final String jwt) {
        this.jwt = jwt;
    }

    public Optional<RbacSubject> authenticate() throws GeneralSecurityException, IOException {
        if (Objects.isNull(jwt) || jwt.isBlank()) {
            return Optional.empty();
        }

        final var gid = Optional.ofNullable(tokenVerifier().verify(jwt));
        if (gid.isEmpty()) {
            return Optional.empty();
        }

        final var email = gid.get().getPayload().getEmail();
        if (Objects.isNull(email) || email.isBlank() || email.equalsIgnoreCase("error")) {
            return Optional.empty();
        }

        return Optional.of(new RbacSubject(email, gid.get().getPayload().getSubject()));
    }

    private static GoogleIdTokenVerifier tokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(TRANSPORT, GsonFactory.getDefaultInstance())
            .setAudience(Collections.singleton(CLIENT_ID))
            .build();
    }
}
