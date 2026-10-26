(:Director)
(:Actor)
(:Film)
(:Theatre)

(:Director)-[:DIRECTS]->(:Film)
(:Actor)-[:STARS_IN]->(:Film)
(:Film)-[:PLAYS_IN]->(:Theatre)

CONSTRAINT (a: Actor)-[STARS_IN]->(f: Film) IMPLIES (d: Director)-[DIRECTS]->(f: Film)
CONSTRAINT (f: Film)-[PLAYS_IN]->(t: Theatre) IMPLIES (d: Director)-[DIRECTS]->(f: Film)
