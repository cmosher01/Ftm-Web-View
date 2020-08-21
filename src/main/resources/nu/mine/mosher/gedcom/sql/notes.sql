/*
    given: a person
        1: FtmLinkTableID
        2: ID

    return: notes
*/

SELECT
    Note.ID AS id,
    Note.NoteText AS note,
    Note.Private AS private_note
FROM
    Note
WHERE
    Note.LinkTableID = ? AND
    Note.LinkID = ?
