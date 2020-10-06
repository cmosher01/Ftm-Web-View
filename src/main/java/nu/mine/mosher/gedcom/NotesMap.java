package nu.mine.mosher.gedcom;



import java.util.List;


public interface NotesMap {
    List<Note> select(FtmLink link);
}
