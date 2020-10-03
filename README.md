# Ftm-Web-View






## Web Pages

### Trees
    /
* list of trees

### Tree Index
    /{tree}/persons/
    /*?tree={tree.ftm}
* list of people in tree

### Person
    /{tree}/persons/{uuid}/
    /*?tree={tree.ftm}&person_uuid={uuid}
    /*?person_uuid={uuid}
The first links are canonical, the last finds person in random tree.
If canonical link: person or tree not found, then try searching in all trees.
TODO: add some artificial intelligence to determine which entries are stubs,
so we can redirect to the non-stub tree.
* links to same person in other trees
* parents (link, name, anc/desc indicator, relation (adopted))
* NAME
* events (each event has:)
  * footnotes (link to citation)
  * notes (converted to citations)
* notes (converted to events/citations)
* partnerships (each partnership has:)
  * partner (link, name)
  * events (each event has:)
    * footnotes (link to citation)
  * children (link, name, anc/desc indicator, relation (adopted), birthdate)
* citations








## FTM database info

dates:

      3=003 00000011
    128=080 10000000
    131=083 10000011
    192=0C0 11000000
    195=0C3 11000011



    ABT D M Y => D   M Y | 0003
    ABT   M Y => 1   M Y | 0083
    ABT     Y => 1 JAN Y | 00C3

    D M Y => D   M Y | 0000 (DAY)
      M Y => 1   M Y | 0080 (MONTH)
        Y => 1 JAN Y | 00C0 (YEAR)


There are 9 flag bits:

    100000000 = calculated
    010000000 = unknown day
    001000000 = unknown month
    000100000 = unknown year
    000010000 = dual date
    000001000 = quarter??? (can't reproduce) (is this UK only?)
    000000100 = calculated??? (can't reproduce)
    000000010 = after
    000000001 = before

    000000011 = ("both before and after" means "about")






    // 0x0003FD90: MyFamily.Shared.Properties.DateParserStrings.resources (792 bytes, Embedded, Public)


    // 0x0003FF7A: About = "about,abt,circa,cca,cir,ca,estimated,est,c,approximately,approx.,approx,a"
    // 0x0003FF74: AD = "a.d."
    // 0x0003FFC5: After = "after:,post,aft.,after,aft"
    // 0x0003FFE1: And = "and"
    // 0x0003FFE6: BC = "b.c."
    // 0x0003FFEC: BCE = "b.c.e."
    // 0x0003FFF4: Before = "before:,ante,bef.,by,before,bef,B. ,b. ,B ,b "
    // 0x00040023: Between = "estbetween:,estbetween,between:,between,bet.,bet,btw.,btw"
    // 0x00040064: Calculated = "calculated,cal"
    // 0x0004005E: CE = "c.e."
    // 0x00040074: Or = "or"
    // 0x00040078: Quarter = "quarter,qtr.,qtr"
    // 0x0004008A: Range = "range:,from,to,and,range"
    // 0x000400A4: To = "to"




                                                        AAAAAAAABBBBBBBB CCCCCCCCDDDDDDDD
    internal const uint MODIFIER_MASK = 511U;                            0000000111111111
    internal const uint PROXIMITY_MASK = 7U;                                     00000111
    internal const uint KEYWORD_MASK = 65535U;                           1111111111111111

    internal const uint SDN_MASK = 2147483136U;         0111111111111111 1111111000000000
    internal const uint ENCODING_MASK = 2147483648U;    1000000000000000 0000000000000000
    internal const uint ENCODING_KEYWORD = 2147483648U; 1000000000000000 0000000000000000
    internal const uint ENCODING_SDN = 0U;                                       00000000
    internal const int SDN_SHIFT = 9;

    internal const uint DAYMISSING_MASK = 128U;                                  10000000
    internal const uint MONTHMISSING_MASK = 64U;                                 01000000
    internal const uint YEARMISSING_MASK = 32U;                                  00100000
    internal const uint DUALDATE_MASK = 16U;                                     00010000
    internal const uint QUARTER_DATE_MASK = 8U;                                  00001000
    internal const uint MODIFIER_CALCULATED = 4U;                                00000100
    internal const uint MODIFIER_AFTER = 2U;                                     00000010
    internal const uint MODIFIER_BEFORE = 1U;                                    00000001
    internal const uint MODIFIER_ABOUT = 3U;                                     00000011
    internal const uint MODIFIER_NONE = 0U;                                      00000000




    DateSort1      DateSort2      DateModifier1  DateModifier2  Date                       Text
    -------------  -------------  -------------  -------------  -------------------------  --------------------------------------------------------------------------------
                                                                                           "" no date
    880809152                     0                             880809152                  "3 BC" year era
    881744064                     0                             881744064                  "3 AD" year era
    1199307792                    0                             1199307792                 "02 Mar 1700/01" day month dual-year
    1252012224     1252386496     0              0              1252012224:1252386496      "Bet. 1983–1985" between year and year
    1252012224     1252479104     0              0              1252012224:1252479104      "Bet. 1983–Jul 1985" between year and month year
    1252012224     1252480000     0              0              1252012224:1252480000      "Bet. 1983–03 Jul 1985" between year and day month year
    1252104832     1252386496     0              0              1252104832:1252386496      "Bet. Jul 1983–1985" between month year and year
    1252104832     1252479104     0              0              1252104832:1252479104      "Bet. Jul 1983–Jul 1985" between month year and month year
    1252104832     1252480000     0              0              1252104832:1252480000      "Bet. Jul 1983–03 Jul 1985" between month year and day month year
    1252105728     1252386496     0              0              1252105728:1252386496      "Bet. 03 Jul 1983–1985" between day month year and year
    1252105728     1252479104     0              0              1252105728:1252479104      "Bet. 03 Jul 1983–Jul 1985" between day month year and month year
    1252105728     1252480000     0              0              1252105728:1252480000      "Bet. 03 Jul 1983–03 Jul 1985" between day month year and day month year
    1252199104                    0                             1252199104                 "1984" year
    1252199105                    -1                            1252199105                 "Bef. 1984" before year
    1252199106                    1                             1252199106                 "Aft. 1984" after year
    1252199107                    -1                            1252199107                 "Abt 1984" about year
    1252292224                    0                             1252292224                 "Jul 1984" month year
    1252292225                    -1                            1252292225                 "Bef. Jul 1984" before month year
    1252292226                    1                             1252292226                 "Aft. Jul 1984" after month year
    1252292227                    -1                            1252292227                 "Abt Jul 1984" about month year
    1252293120                    0                             1252293120                 "03 Jul 1984" day month year
    1252293121                    -1                            1252293121                 "Bef. 03 Jul 1984" before day month year
    1252293122                    1                             1252293122                 "Aft. 03 Jul 1984" after day month year
    1252293123                    -1                            1252293123                 "Abt 03 Jul 1984" about day month year
    1252293376                    0                             1252293376                 "Cal. 03 Jul 1984" calculated
    2147293824                    0                             112800                     "Jul" month
    2147294720                    0                             113696                     "03 Jul" day month
    2147387392                    0                             2147483665                 "Unknown" unknown
    2147387392                    0                             3 invalid format           "3 invalid format" invalid



Allowed date range: 1 JAN 4713 BC through 31 DEC 6000 AD, inclusive


select strftime('%Y-%m-%d',date/512) from fact;


TODO: research:
* dates: "between", modifiers, "abt", other string, other calendars?
* notes: on media?, on other?
* multiple names
* tasks
* source templates


lat/long: r*(180/pi), +:N/E, -:S/W

### metalinks

parent      |child
---         |---
person      |fact
relationship|fact
fact        |note
person      |note
relationship|note
mediafile   |note
source      |note
fact        |sourcelink
fact        |medialink
person      |medialink
relationship|medialink
source      |medialink
mastersource|medialink
source      |weblink
fact        |task
task        |taglink
mediafile   |taglink

parent      |child
---         |---
fact        |medialink
fact        |note
fact        |sourcelink
fact        |task
mediafile   |taglink
mediafile   |note
person      |fact
person      |medialink
person      |note
relationship|fact
relationship|note
relationship|medialink
mastersource|medialink
source      |note
source      |medialink
source      |weblink
task        |taglink





    Person
    Relationship (family: parents)
    ChildRelationship (family: children)



    Place
    FactType
    Fact (event, attribute)



    MediaFile
    MediaFileOriginal
    Watermark
    MediaLink



    Repository
    MasterSource (source)
    Source (citation)
    SourceLink (groups citations across facts)



    Note

    WebLink

    Task

    Tag
    TagLink

    DnaMatch

    FamilySearchMatches



    PersonExternal (?)




    Publication (saved charts)




    Deleted
    ChangeCommand
    ChangeMacroCommand
    HistoryList



    Sync_Artifact
    Sync_ArtifactReference
    Sync_Citation
    Sync_CitationReference
    Sync_Fact
    Sync_FactDelete
    Sync_Media
    Sync_Note
    Sync_Person
    Sync_Relationship
    Sync_Repository
    Sync_Setting
    Sync_Source
    Sync_State
    Sync_Weblink



    DynamicFilter
    DynamicFilterItem
    DynamicFilterResultSet



    Setting








CreateDate and UpdateDate columns are declared DATETIME type, and stored as
INTEGERs representing Unix Time, the number of seconds since 1970-01-01 00:00:00 UTC

SELECT datetime(UpdateDate,'unixepoch') FROM MasterSource;
UPDATE MasterSource set UpdateDate = strftime('%s','now') WHERE xxxxxx;

need to update SyncVersion column (somehow) in order for FTM to sync it with Ancestry
(seems to be "select ID+4 from sync_state;" ???)

    LinkTableID refers to tables (defined in FTM.Data.DB.dll: FTM.Data.DB/TableID enum):
      0 Assertion,
      1 ChildRelationship,
      2 Fact,
      3 FactType,
      4 Note,
      5 Person,
      6 Place,
      7 Relationship,
      8 Setting,
      9 Task,
     10 MasterSource,
     11 Category,
     12 Repository,
     13 MediaFile,
     14 MediaLink,
     15 FileCategoryRel,
     16 Source,
     17 SourceLink,
     18 TaskCategory,
     19 TaskCategoryRel,
     20 History,
     21 Publication,
     22 HistoryList,
     23 Deleted,
     24 Cache,
     25 WebLink,
     26 Tag,
     27 TagLink,
     28 MediaFileBookmark,
     29 SettingURLBookmark,
     30 PersonExternal,
     31 ChangeMacroCommand,
     32 ChangeCommand,
     33 DynamicFilter,
     34 DynamicFilterItem,
     35 Watermark,
     36 DnaMatch,
     37 CompactChangesForUndo,
     38 MediaFileOriginal,


    Tables with LinkTableID column:
    Fact
    MediaLink
    Note
    SourceLink
    TagLink
    Task
    WebLink

    common tables linked to:
     2 Fact
     5 Person
     7 Relationship
     9 Task
    13 MediaFile
    16 Source

    select linktableid as parent, max('fact') as child from fact group by linktableid union
    select linktableid, max('medialink') from medialink group by linktableid union
    select linktableid, max('note') from note group by linktableid union
    select linktableid, max('sourcelink') from sourcelink group by linktableid union
    select linktableid, max('taglink') from taglink group by linktableid union
    select linktableid, max('task') from task group by linktableid union
    select linktableid, max('weblink') from weblink group by linktableid;




