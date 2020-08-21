package nu.mine.mosher.gedcom;



public record Citation(
    String author,
    String title,
    String place_pub,
    String pub,
    String date_pub,
    String citation
) {
}
