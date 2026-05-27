(:Course) { courseCode :: string, credits :: int }
(:Department) { deptName :: string }
(:Student <: Person) { neptunCode :: string, enrollmentYear :: int }

(:Student)-[:ATTENDS]->(:Course)
(:Course)-[:BELONGS_TO]->(:Department)
