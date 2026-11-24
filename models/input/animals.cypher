(:Animal)
(:Carnivore => Animal)
(:Herbivore => Animal)
(:Habitat)

(:Carnivore)-[:EATS]->(:Animal)
(:Animal)-[:LIVES_IN]->(:Habitat)

MATCH (a)-[EATS]->(b)

CONSTRAINT (c:Carnivore)-[:EATS]->(h:Herbivore)
IMPLIES (c)-[:LIVES_IN]->(loc:Habitat)<-[:LIVES_IN]-(h)

