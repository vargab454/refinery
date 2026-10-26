(:Person)
(:City)
(:Doctor => Person)
(:Patient => Person)
(:Doctor)-[:TREATS]->(:Patient)
(:Person)-[:LOCATED_IN]->(:City)

CONSTRAINT (d: Doctor)-[TREATS]->(p: Patient) IMPLIES (d: Doctor)-[LOCATED_IN]->(c: City)<-[LOCATED_IN]-(p: Patient)
