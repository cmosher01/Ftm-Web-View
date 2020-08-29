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
    /persons/{uuid}/
    /*?person_uuid={uuid}
The first link is canonical, the other two find person and redirect to random tree.
If canonical link: person or tree not found, then try searching in all trees.
TODO: add some artificial intelligence to determine which entries are stubs,
so we can redirect to the non-stub tree.
* links to same person in other trees
* parents (link, name, anc/desc indicator)
* NAME
* events (each event has:)
  * footnotes (link to citation)
  * notes (converted to citations)
* notes (converted to events/citations)
* partnerships (each partnership has:)
  * partner (link, name)
  * events (each event has:)
    * footnotes (link to citation)
  * children (birth, link, name, anc/desc indicator)
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



    10000000 = unknown day
    01000000 = unknown month
    00100000 = ?
    00010000 = ?
    00001000 = ?
    00000100 = ?
    00000011 = ABT



TODO: research:
* dates: "between", modifiers, "abt", other string, other calendars?
* notes: on media?, on other?
* multiple names
* tasks
* source templates


lat/long: N*(180/pi), +:N/E, -:S/W

### metalinks

|parent      |child
|:---------- |:--------
|person      |fact
|relationship|fact
|fact        |note
|person      |note
|fact        |sourcelink
|fact        |medialink
|person      |medialink
|source      |medialink
|source      |weblink
|fact        |task
|task        |taglink
|mediafile   |taglink

|child       |parent
|:---------- |:--------
|fact        |medialink
|fact        |note
|fact        |sourcelink
|fact        |task
|mediafile   |taglink
|person      |fact
|person      |medialink
|person      |note
|relationship|fact
|source      |medialink
|source      |weblink
|task        |taglink





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




