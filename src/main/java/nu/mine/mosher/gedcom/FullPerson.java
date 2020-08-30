package nu.mine.mosher.gedcom;

import java.util.*;

public class FullPerson {
    public UUID id;
    public String nameWithSlashes;
    public ArrayList<PersonParent> parents = new ArrayList<>(2);
    public ArrayList<PersonPartnership> partnerships = new ArrayList<>(2);

    @Override
    public String toString() {
        return this.nameWithSlashes;
    }

    public List<PersonPartnership> getPartnerships() {
        if (Objects.isNull(this.partnerships)) {
            return Collections.emptyList();
        }
        return this.partnerships;
    }
}
