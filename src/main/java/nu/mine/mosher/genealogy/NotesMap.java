package nu.mine.mosher.genealogy;



import java.util.List;


public interface NotesMap {
    List<Note> select(FtmLink link);
}
