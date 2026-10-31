(:Doctor => Person)
(:Patient => Person)
(:Hospital)
(:City)

(:Doctor)-[:TREATS]->(:Patient)
(:Person)-[:LOCATED_IN]->(:City)
(:Hospital)-[:IS_IN]->(:City)
(:Doctor)-[:WORKS_AT]->(:Hospital)

CONSTRAINT (d: Doctor)-[TREATS]->(p: Patient) IMPLIES (d: Doctor)-[LOCATED_IN]->(c: City)<-[LOCATED_IN]-(p: Patient)
CONSTRAINT (d: Doctor)-[WORKS_AT]->(h: Hospital) IMPLIES (d: Doctor)-[LOCATED_IN]->(c: City)<-[IS_IN]-(h: Hospital)
