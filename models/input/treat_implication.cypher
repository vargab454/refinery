(:Doctor)-[:TREATS]->(:Patient)
(:Doctor => Person)
(:Patient => Person)
(:Person)-[:LOCATED_IN]->(:City)

CONSTRAINT (d)-[TREATS]->(p) IMPLIES (d)-[LOCATED_IN]->(c: City)<-[LOCATED_IN]-(p)
