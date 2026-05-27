package io.codekontor.opencypher.xtext.transform;

import java.util.HashSet;
import java.util.Set;
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
		StringBuilder classesBuffer = new StringBuilder();
		StringBuilder predicatesBuffer = new StringBuilder();
		Set<String> declaredClasses = new HashSet<>();
		Set<String> referencedClasses = new HashSet<>();
		for (Statement statement : model.getStatements()) {
			if (statement instanceof NodeTypeDefinition && !(statement instanceof RelationshipTypeDefinition)) {
				NodeTypeDefinition nodeType = (NodeTypeDefinition) statement;
				String className = nodeType.getName();
				if (className == null) continue;
				declaredClasses.add(className);
				classesBuffer.append("class ").append(className);
				if (nodeType.getSuperTypes() != null && !nodeType.getSuperTypes().isEmpty()) {
					classesBuffer.append(" extends ");
					for (int i = 0; i < nodeType.getSuperTypes().size(); i++) {
						String parent = nodeType.getSuperTypes().get(i);
						referencedClasses.add(parent); // Hivatkozás gyűjtése
						classesBuffer.append(parent);
						if (i < nodeType.getSuperTypes().size() - 1) {
							classesBuffer.append(", ");
						}
					}
				}
				classesBuffer.append(" {\n");
				if (nodeType.getProperties() != null && nodeType.getProperties().getProperties() != null) {
					for (TypeProperty prop : nodeType.getProperties().getProperties()) {
						classesBuffer.append("    ").append(prop.getType())
								.append(" ").append(prop.getName()).append("\n");
					}
				}
				classesBuffer.append("}\n\n");
			}
		}
		for (Statement statement : model.getStatements()) {
			if (statement instanceof RelationshipTypeDefinition) {
				RelationshipTypeDefinition rel = (RelationshipTypeDefinition) statement;
				String sourceClass = rel.getLeft() != null ? rel.getLeft().getName() : null;
				String targetClass = rel.getRight();
				String relationName = rel.getRel();
				if (sourceClass == null || targetClass == null) continue;
				referencedClasses.add(sourceClass);
				referencedClasses.add(targetClass);
				if (relationName != null && relationName.startsWith(":")) {
					relationName = relationName.substring(1);
				}

				predicatesBuffer.append("pred ").append(relationName)
						.append("(").append(sourceClass).append(" left, ")
						.append(targetClass).append(" right).\n\n");
			}
		}
		StringBuilder autoDeclarationBuffer = new StringBuilder();
		boolean checkingNeeded = false;
		for (String refClass : referencedClasses) {
			if (!declaredClasses.contains(refClass)) {
				autoDeclarationBuffer.append("class ").append(refClass).append(" {}\n");
				checkingNeeded = true;
			}
		}
		autoDeclarationBuffer.append("\n");
		if (!checkingNeeded) {
			autoDeclarationBuffer.setLength(0);
		}
		return autoDeclarationBuffer.toString() + classesBuffer.toString() + predicatesBuffer.toString();
	}
}
