(:Doctor { specialty :: string, experience :: int })
(:Patient { age :: int, needsSurgery :: bool })
(:Hospital { isCertified :: bool, tier :: int })

(:Doctor)-[:TREATS { urgency :: int }]->(:Patient)
(:Doctor)-[:OPERATES_IN]->(:Hospital)

CONSTRAINT (d)-[t:TREATS]->(p)
WHERE d.specialty = "Surgery" AND p.needsSurgery = true AND t.urgency >= 3
IMPLIES (d)-[:OPERATES_IN]->(h)
WHERE h.isCertified = true AND h.tier <> 1
