(:Teacher)
(:Student)
(:School)

(:Teacher)-[:TEACHES]->(:Student)
(:Student)-[:STUDIES_IN]->(:School)

CONSTRAINT (t: Teacher)-[TEACHES]->(s: Student) IMPLIES (s: Student)-[STUDIES_IN]->(u: School)
