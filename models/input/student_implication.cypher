(:Teacher)-[:TEACHES]->(:Student)
(:Student)-[:STUDIES_IN]->(:School)

CONSTRAINT (t)-[TEACHES]->(s) IMPLIES (s)-[STUDIES_IN]->(u)
