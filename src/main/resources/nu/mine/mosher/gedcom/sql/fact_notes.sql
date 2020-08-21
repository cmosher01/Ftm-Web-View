/*
    given: a person or a relationship
        1: FtmLinkTableID
        2: ID

    return: fact notes
*/

SELECT
    Fact.ID AS id_fact,
    Note.ID AS id_note,
    Note.NoteText AS note,
    Note.Private AS private_note
FROM
    Fact LEFT OUTER JOIN
    Note ON (Note.LinkTableID = 2 AND Note.LinkID = Fact.ID)
WHERE
    Fact.LinkTableID = ? AND
    Fact.LinkID = ?
ORDER BY
    Fact.ID
