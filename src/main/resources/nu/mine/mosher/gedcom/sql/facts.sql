/*
    given: a person or a relationship
        1: FtmLinkTableID
        2: ID

    return: facts
*/

SELECT
    Fact.ID AS id,
    Fact.Date AS ftm_date,
    Fact.Text AS description,
    Fact.Private AS prvt,
    Fact.Preferred AS pref,
    FactType.Name AS type,
    Place.Name AS ftm_place
FROM
    Fact LEFT OUTER JOIN
    FactType ON FactType.ID = Fact.FactTypeID LEFT OUTER JOIN
    Place ON Place.ID = Fact.PlaceID
WHERE
    Fact.LinkTableID = ? AND
    Fact.LinkID = ?
ORDER BY
    Fact.ID
