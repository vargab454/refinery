(:Person { SSN :: int })
(:Student => Person { studentID :: int })
(:Person)-[:FRIEND]->(:Person)
(:Student)-[:ATTENDS]->(:Course)

MATCH (p1: Person)-[:FRIEND]->(p2)
MATCH (p1: Person)-[:ATTENDS]->(p2)
