package nu.mine.mosher.genealogy;

import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class UrlTest {
    @Test
    void nominal() throws URISyntaxException {
        assertEquals("?tree=a.ftm&person_uuid=12345",
            new URIBuilder().addParameter("tree","a.ftm").addParameter("person_uuid", "12345").build().toString());
    }
}
