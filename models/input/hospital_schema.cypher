(:Person) { ssn :: int, name :: string }
(:Employee => Person) { employeeId :: string }
(:Doctor => Employee) { specialization :: string, licenseNumber :: int }
(:Patient => Person) { medicalHistoryId :: string, admissionDate :: string }
(:Room) { roomNumber :: int, floor :: int }

(:Doctor)-[:TREATS => { time :: int }]->(:Patient)
(:Patient)-[:ASSIGNED_TO]->(:Room)
