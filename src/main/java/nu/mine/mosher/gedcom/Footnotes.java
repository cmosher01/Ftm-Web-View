package nu.mine.mosher.gedcom;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;



/**
 * Repository for footnotes.
 * Add notable items in order (keeping track of the returned footnote index for each item).
 * Use the index to display the footnote number (1 through n).
 * Retrieve notable by index, or retrieve index by notable.
 * Thread safe.
 */
public final class Footnotes<T> {
    private final List<T> list = new ArrayList<>();
    private final Map<T,Integer> map = new HashMap<>();
    private int next = 1;

    public synchronized int putFootnote(final T notable) {
        if (this.map.containsKey(Objects.requireNonNull(notable))) {
            return this.map.get(notable);
        }

        this.list.add(notable);
        this.map.put(notable, this.next);
        return this.next++;
    }

    public synchronized T getFootnote(final int i) {
        return this.list.get(i-1);
    }

    public synchronized int size() {
        return this.next-1;
    }
}
