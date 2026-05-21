package io.codekontor.opencypher.xtext.transform;

import io.codekontor.opencypher.xtext.openCypher.*;

/**
 * Dedicated compiler component responsible for generating the structural
 * blueprint of the Refinery schema representation.
 */
public class RefineryModelTransformer {

	/**
	 * Converts the parsed openCypher model statements into equivalent Refinery class declarations.
	 *
	 * @param model the root container of the parsed openCypher AST, may be null
	 * @return the generated declarative Refinery schema source code as a String
	 */
    public String convertToRefinery(Cypher model) {
        if (model == null) return "// Error: Model root container is empty.";
		StringBuilder refineryCode = new StringBuilder();
		// Iterate through all parsed statements in the input Cypher model
		for (Statement statement : model.getStatements()) {
			if (statement instanceof NodeTypeDefinition) {
				NodeTypeDefinition nodeType = (NodeTypeDefinition) statement;
				// Process class name definition
				refineryCode.append("class ").append(nodeType.getLabel().getLabelName());
				// Handle optional inheritance
				if (nodeType.getExtends() != null) refineryCode.append(" extends ").append(nodeType.getExtends().getLabel().getLabelName());
				// Process class properties if they exist
				if (nodeType.getProperties() != null) {
					refineryCode.append(" {\n");
					for (TypeProperty prop : nodeType.getProperties().getProperties()) {
						refineryCode.append("    ")
								.append(prop.getType())
								.append(" ")
								.append(prop.getName())
								.append("\n");
					}
					refineryCode.append("}\n");
				} else refineryCode.append(".");
				refineryCode.append("\n");
			} else if(statement instanceof RelationshipTypeDefinition) {
				RelationshipTypeDefinition rel = (RelationshipTypeDefinition) statement;
				String sourceClass = rel.getLeft().getLabel().getLabelName();
				String targetClass = rel.getRight().getLabel().getLabelName();
				String relationName = rel.getRel();
				if (relationName != null && relationName.startsWith(":")) relationName = relationName.substring(1);
				refineryCode.append("pred ").append(relationName).append("(").append(sourceClass).append(" left, ").append(targetClass).append(" right).\n\n");
			}
		}
		return refineryCode.toString();
    }
}
