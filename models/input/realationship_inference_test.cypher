(:Animal)
(:Carnivore => Animal)
(:Herbivore => Animal)
(:Habitat)

(:Carnivore)-[:EATS]->(:Animal)
(:Animal)-[:LIVES_IN]->(:Habitat)

MATCH (c:Carnivore)-[:TRACKED_BY]->(r:Ranger)
MATCH (h:Herbivore)-[:TRACKED_BY]->(d:Drone)

CONSTRAINT (c:Carnivore)-[:EATS]->(h:Herbivore)
IMPLIES (c)-[:OBSERVED_WITH]->(h)
