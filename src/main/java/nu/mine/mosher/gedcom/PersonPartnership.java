package nu.mine.mosher.gedcom;

import java.util.UUID;


public record PersonPartnership(int id, UUID idPerson, String name, int nature) {
}
