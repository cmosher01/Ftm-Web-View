/*
    given: a person
        1: PersonID

    return: parents
*/

WITH Param AS (
    SELECT
        ? AS PersonID
)
SELECT
    Person.ID AS id,
    Person.FullNameReversed AS name,
    ChildRelationship.Nature1 AS nature, -- note: nature 1
    Relationship.Private AS prvt_parent,
    ChildRelationship.Private AS prvt_child,
    ChildRelationship.Preferred AS pref
FROM
    Param LEFT OUTER JOIN
    ChildRelationship ON (ChildRelationship.PersonID = Param.PersonID) LEFT OUTER JOIN
    Relationship ON (Relationship.ID = ChildRelationship.RelationshipID) LEFT OUTER JOIN
    Person ON (Person.ID = Relationship.Person1ID) -- note: parent 1
UNION ALL
SELECT
    Person.ID,
    Person.FullNameReversed,
    ChildRelationship.Nature2, -- note: nature 2
    Relationship.Private,
    ChildRelationship.Private,
    ChildRelationship.Preferred
FROM
    Param LEFT OUTER JOIN
    ChildRelationship ON (ChildRelationship.PersonID = Param.PersonID) LEFT OUTER JOIN
    Relationship ON (Relationship.ID = ChildRelationship.RelationshipID) LEFT OUTER JOIN
    Person ON (Person.ID = Relationship.Person2ID) -- note: parent 2
