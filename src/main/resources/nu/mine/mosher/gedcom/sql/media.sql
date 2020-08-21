/*
    given: a fact, a person, or a source
        1: FtmLinkTableID
        2: ID

    return: media files
*/

SELECT
    MediaLink.Private AS prvt_link,
    MediaLink.FileOrder,
    MediaFile.FileName,
    MediaFile.FileCaption,
    MediaFile.FileDescription,
    MediaFile.FileDate,
    MediaFile.Thumbnail,
    MediaFile.PID AS apid,
    MediaFile.IsPrivate AS prvt_file,
    MediaFile.WID,
    MediaFile.FileHash
FROM
    MediaLink LEFT OUTER JOIN
    MediaFile ON MediaFile.ID = MediaLink.MediaFileID
WHERE
    MediaLink.LinkTableID = ? AND
    MediaLink.LinkID = ?
