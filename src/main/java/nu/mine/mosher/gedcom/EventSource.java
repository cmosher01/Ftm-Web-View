package nu.mine.mosher.gedcom;



import java.util.Objects;



public record EventSource(
    int pkidSourceLink,
    String apid,
    Integer stars,
    String just,

    Integer pkidCitation,
    String page,
    String comment,
    String footnote,
    String apidCitation,

    String author,
    String title,
    String placePub,
    String pub,
    String datePub,
    String callno,
    String source,
    String apidSource,
    String template
) {
    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof EventSource)) {
            return false;
        }
        final EventSource that = (EventSource)object;
        return this.pkidCitation() == that.pkidCitation();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pkidCitation());
    }
}
