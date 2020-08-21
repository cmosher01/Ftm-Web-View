/*
    given: a person
        1: ID

    return: partnerships
*/

WITH Param AS (
    SELECT
        ? AS PersonID
)
SELECT
    Relationship.ID AS id,
    Person.ID AS id_person,
    Person.FullNameReversed AS name,
    Person.BirthDate AS ftmdate_birth,
    Relationship.Private AS prvt,
    Relationship.RelType
FROM
    Param LEFT OUTER JOIN
    Relationship ON (Relationship.Person1ID = Param.PersonID) LEFT OUTER JOIN -- note: person 1
    Person ON (Person.ID = Relationship.Person2ID) -- note: person 2
WHERE
    Person.ID IS NOT NULL
UNION ALL
SELECT
    Relationship.ID,
    Person.ID,
    Person.FullNameReversed,
    Person.BirthDate,
    Relationship.Private,
    Relationship.RelType
FROM
    Param LEFT OUTER JOIN
    Relationship ON (Relationship.Person2ID = Param.PersonID) LEFT OUTER JOIN -- note: person 2
    Person ON (Person.ID = Relationship.Person1ID) -- note: person 1
WHERE
    Person.ID IS NOT NULL
ORDER BY
    Relationship.ID
