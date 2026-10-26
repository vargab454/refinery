(:Director)-[:DIRECTS]->(:Film)
(:Actor)-[:STARS_IN]->(:Film)
(:Film)-[:PLAYS_IN]->(:Theatre)

CONSTRAINT (a)-[STARS_IN]->(f) IMPLIES (d)-[DIRECTS]->(f)
CONSTRAINT (f)-[PLAYS_IN]->(t) IMPLIES (d)-[DIRECTS]->(f)
