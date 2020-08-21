/*
    given: a person or a relationship
        1: FtmLinkTableID
        2: ID

    return: fact sources
*/

SELECT
    Fact.ID AS id_fact,
    SourceLink.ExternalID AS apid_fact,
    SourceLink.Stars AS stars,
    SourceLink.Justification AS just,
    Source.ID AS id_citation,
    Source.PageNumber AS citation,
    Source.Comment AS comment_citation,
    Source.Footnote AS footnote_citation,
    Source.PID AS apid_citation,
    MasterSource.Author AS author,
    MasterSource.Title AS title,
    MasterSource.PublisherLocation AS place_pub,
    MasterSource.PublisherName AS pub,
    MasterSource.PublishDate AS date_pub,
    MasterSource.CallNumber AS call_number,
    MasterSource.Comments AS comment_source,
    MasterSource.PID AS apid_source,
    MasterSource.TemplateData AS source_tmpl
FROM
    Fact LEFT OUTER JOIN
    SourceLink ON (SourceLink.LinkTableID = 2 AND SourceLink.LinkID = Fact.ID) LEFT OUTER JOIN
    Source ON Source.ID = SourceLink.SourceID LEFT OUTER JOIN
    MasterSource ON MasterSource.ID = Source.MasterSourceID
WHERE
    Fact.LinkTableID = ? AND
    Fact.LinkID = ?
ORDER BY
    Fact.ID
