/*
    given: a person
        1: ID

    return: children
*/

WITH Param AS (
    SELECT
        ? AS PersonID
)
SELECT
    Relationship.ID AS id,
    Person.ID AS id_person,
    Person.FullNameReversed AS name,
    Person.BirthDate AS ftmdate_birth
FROM
    Param LEFT OUTER JOIN
    Relationship ON (Relationship.Person1ID = Param.PersonID) LEFT OUTER JOIN -- note: person 1
    ChildRelationship ON (ChildRelationship.RelationshipID = Relationship.ID) LEFT OUTER JOIN
    Person ON (Person.ID = ChildRelationship.PersonID)
WHERE
    Person.ID IS NOT NULL
UNION ALL
SELECT
    Relationship.ID,
    Person.ID,
    Person.FullNameReversed,
    Person.BirthDate
FROM
    Param LEFT OUTER JOIN
    Relationship ON (Relationship.Person2ID = Param.PersonID) LEFT OUTER JOIN -- note: person 2
    ChildRelationship ON (ChildRelationship.RelationshipID = Relationship.ID) LEFT OUTER JOIN
    Person ON (Person.ID = ChildRelationship.PersonID)
WHERE
    Person.ID IS NOT NULL
ORDER BY
    Relationship.ID
