(:Doctor => Person)
(:Patient => Person)
(:Doctor)-[:TREATS]->(:Patient)
(:Person)-[:LOCATED_IN]->(:City)

CONSTRAINT (d)-[TREATS]->(p) IMPLIES (d)-[LOCATED_IN]->(c: City)<-[LOCATED_IN]-(p)
