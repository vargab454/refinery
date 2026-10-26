/*
 * openCypher Xtext - Slizaa Static Software Analysis Tools
 * Copyright © ${year} Code-Kontor GmbH and others (slizaa@codekontor.io)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Code-Kontor GmbH - initial API and implementation
 */
package io.codekontor.opencypher.xtext.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.util.CancelIndicator;
import io.codekontor.opencypher.xtext.openCypher.*;

/**
 * Dedicated compiler component responsible for generating the structural
 * blueprint of the Refinery schema representation.
 */
public class RefineryModelTransformer {

	private int matchCounter = 1;
	private int constraintCounter = 1;
	private int anonVarCounter = 1;

	private static class PatternInfo {
		List<String> parameters = new ArrayList<>();
		List<String> conditions = new ArrayList<>();
	}

	private static class CheckInfo {
		String name;
		List<String> parameters;
		CheckInfo(String name, List<String> parameters) {
			this.name = name;
			this.parameters = parameters;
		}
	}

	public String convertToRefinery(Cypher model) {
		if (model == null) return "// Error: Model root container is empty.";

		// Reset counters so numbering starts fresh for each file/conversion run
		this.matchCounter = 1;
		this.constraintCounter = 1;

		StringBuilder out = new StringBuilder();

		// 1. Extract and append ALL validations (Syntax + Semantic) with line numbers via Xtext Validator
		Set<String> errors = new HashSet<>();
		Set<String> warnings = new HashSet<>();

		if (model.eResource() instanceof XtextResource xtextResource) {
			IResourceServiceProvider serviceProvider = xtextResource.getResourceServiceProvider();
			if (serviceProvider != null) {
				IResourceValidator validator = serviceProvider.get(IResourceValidator.class);
				if (validator != null) {
					List<Issue> issues = validator.validate(xtextResource, CheckMode.ALL, CancelIndicator.NullImpl);
					for (Issue issue : issues) {
						String lineInfo = issue.getLineNumber() != null ? " (Line " + issue.getLineNumber() + ")" : "";
						String issueText = issue.getMessage() + lineInfo;

						String severityName = issue.getSeverity() != null ? issue.getSeverity().name() : "";
						if ("ERROR".equals(severityName)) {
							errors.add(issueText);
						} else if ("WARNING".equals(severityName)) {
							warnings.add(issueText);
						}
					}
				}
			}
		}

		// Print captured issues as comments
		if (!errors.isEmpty() || !warnings.isEmpty()) {
			if (!errors.isEmpty()) {
				out.append("// --- Syntax / Semantic Errors ---\n");
				for (String err : errors) {
					out.append("// ERROR: ").append(err).append("\n");
				}
			}
			if (!warnings.isEmpty()) {
				out.append("// --- Warnings ---\n");
				for (String warn : warnings) {
					out.append("// WARNING: ").append(warn).append("\n");
				}
			}
			out.append("// -------------------------------\n\n");
		}

		out.append("class Node.\n\n");

		Map<String, List<String>> propagationRules = new LinkedHashMap<>();
		Set<String> declaredNodes = new HashSet<>();

		// Track references separately: parent classes MUST be classes, while inferred relations are predicates
		Set<String> referencedParentClasses = new HashSet<>();
		Set<String> referencedNodes = new HashSet<>();

		StringBuilder classesBuffer = new StringBuilder();
		StringBuilder relationshipsBuffer = new StringBuilder();
		boolean hasRelationships = false;

		// 2. Process Node and Relationship Definitions
		for (Statement statement : model.getStatements()) {

			if (statement instanceof NodeTypeDefinition nodeType && !(nodeType instanceof RelationshipTypeDefinition)) {
				String className = nodeType.getName();
				if (className == null) continue;

				declaredNodes.add(className);

				// Handle primary native inheritance (Index 0)
				String parentClass = "Node";
				if (nodeType.getSuperTypes() != null && !nodeType.getSuperTypes().isEmpty()) {
					parentClass = cleanName(nodeType.getSuperTypes().getFirst());
					referencedParentClasses.add(parentClass);
				}

				classesBuffer.append("class ").append(className).append(" extends ").append(parentClass).append(" {\n");
				appendProperties(classesBuffer, nodeType.getProperties());
				classesBuffer.append("}\n\n");

				// Handle secondary supertypes (Index 1..N) via propagation rules and predicates
				if (nodeType.getSuperTypes() != null && nodeType.getSuperTypes().size() > 1) {
					List<String> additionalSupertypes = nodeType.getSuperTypes().subList(1, nodeType.getSuperTypes().size());
					propagationRules.put(className, additionalSupertypes);
					for (String superType : additionalSupertypes) {
						referencedNodes.add(cleanName(superType));
					}
				}
			}
			else if (statement instanceof RelationshipTypeDefinition rel) {
				hasRelationships = true;
				String sourceClass = rel.getLeft() != null ? rel.getLeft().getName() : null;
				String targetClass = rel.getRight();
				String relationName = rel.getRel();

				if (sourceClass == null || targetClass == null) continue;

				// Track nodes for type inference as predicates
				referencedNodes.add(cleanName(sourceClass));
				referencedNodes.add(cleanName(targetClass));

				relationshipsBuffer.append("class ").append(cleanName(relationName)).append(" extends RELATIONSHIP {\n");
				relationshipsBuffer.append("    ").append(cleanName(sourceClass)).append("[1] source\n");
				relationshipsBuffer.append("    ").append(cleanName(targetClass)).append("[1] target\n");
				appendProperties(relationshipsBuffer, rel.getProperties());
				relationshipsBuffer.append("}\n\n");
			}
		}

		// 3. Auto-declare missing parent classes (Refinery requires extends targets to be actual classes)
		for (String parentClass : referencedParentClasses) {
			if (!declaredNodes.contains(parentClass) && !parentClass.equals("Node")) {
				classesBuffer.append("class ").append(parentClass).append(" extends Node {}\n\n");
				declaredNodes.add(parentClass); // Prevent it from also being generated as a predicate below
			}
		}

		out.append(classesBuffer);

		// 4. Generate Predicates exclusively for undeclared (inferred) types (like relationship endpoints)
		for (String refNode : referencedNodes) {
			if (!declaredNodes.contains(refNode)) {
				out.append("pred ").append(refNode).append("(Node n).\n\n");
			}
		}

		// 5. Output Propagation Rules (for secondary supertypes only)
		for (Map.Entry<String, List<String>> entry : propagationRules.entrySet()) {
			String className = entry.getKey();
			List<String> superTypes = entry.getValue();

			String ruleName = Character.toLowerCase(className.charAt(0)) + className.substring(1) + "ImpliedTypes";
			out.append("propagation rule ").append(ruleName).append("(").append(className).append(" p) ==> ");

			for (int i = 0; i < superTypes.size(); i++) {
				out.append(cleanName(superTypes.get(i))).append("(p)");
				if (i < superTypes.size() - 1) out.append(", ");
			}
			out.append(".\n\n");
		}

		// 6. Output Relationship Classes
		if (hasRelationships) {
			out.append("abstract class RELATIONSHIP.\n\n");
			out.append(relationshipsBuffer);
		}

		// 7. Process Queries (MATCH clauses) and Constraints (CONSTRAINT ... IMPLIES ...)
		processQueries(model, out);
		processConstraints(model, out);

		return out.toString().trim() + "\n";
	}

	private void processQueries(Cypher model, StringBuilder out) {
		List<Match> matchClauses = EcoreUtil2.getAllContentsOfType(model, Match.class);
		for (Match match : matchClauses) {
			processMatchPattern(match.getPattern(), out);
		}
	}

	private void processConstraints(Cypher model, StringBuilder out) {
		List<ConstraintStatement> constraints = EcoreUtil2.getAllContentsOfType(model, ConstraintStatement.class);
		List<CheckInfo> checkInfos = new ArrayList<>();

		for (ConstraintStatement stmt : constraints) {
			PatternInfo cInfo = extractPatternInfo(stmt.getConstraintPattern());
			PatternInfo iInfo = extractPatternInfo(stmt.getImpliesPattern());

			// Ensure all constraint parameters are bound in the implies body
			for (String param : cInfo.parameters) {
				if (!iInfo.parameters.contains(param)) {
					iInfo.conditions.addFirst("Node(" + param + ")");
				}
			}

			int currentId = constraintCounter++;
			String checkName = "check" + currentId;
			checkInfos.add(new CheckInfo(checkName, cInfo.parameters));

			// 1. Constraint Predicate
			out.append("pred constraint").append(currentId).append("(");
			appendParameterDeclarations(out, cInfo.parameters);
			out.append(") <-> \n");
			appendConditions(out, cInfo.conditions);
			out.append(".\n\n");

			// 2. Implies Predicate
			out.append("pred implication").append(currentId).append("(");
			appendParameterDeclarations(out, cInfo.parameters);
			out.append(") <-> \n");
			appendConditions(out, iInfo.conditions);
			out.append(".\n\n");

			// 3. Check Predicate
			out.append("pred ").append(checkName).append("(");
			appendParameterDeclarations(out, cInfo.parameters);
			out.append(") <-> \n");
			out.append("    constraint").append(currentId).append("(");
			appendParameterArguments(out, cInfo.parameters);
			out.append("), \n");
			out.append("    !implication").append(currentId).append("(");
			appendParameterArguments(out, cInfo.parameters);
			out.append(").\n\n");
		}

		// 4. Global Force Counterexample Error Rule (Connects all checks with disjunction ';')
		if (!checkInfos.isEmpty()) {
			out.append("error forceCounterexample() <-> \n");
			for (int i = 0; i < checkInfos.size(); i++) {
				CheckInfo info = checkInfos.get(i);
				out.append("    !").append(info.name).append("(");
				for (int j = 0; j < info.parameters.size(); j++) {
					out.append("_");
					if (j < info.parameters.size() - 1) out.append(", ");
				}
				out.append(")");
				if (i < checkInfos.size() - 1) {
					out.append(" ; \n");
				}
			}
			out.append(".\n\n");
		}
	}

	private PatternInfo extractPatternInfo(Pattern pattern) {
		PatternInfo info = new PatternInfo();
		if (pattern == null || pattern.getPatterns().isEmpty()) return info;

		// Reset relationship counter independently for each pattern block
		anonVarCounter = 1;

		for (PatternPart part : pattern.getPatterns()) {
			PatternElement pe = null;
			if (part instanceof PatternElement patternElement) {
				pe = patternElement;
			} else if (part.getPart() instanceof PatternElement patternElement) {
				pe = patternElement;
			}

			if (pe != null) {
				String sourceVar = processNodePattern(pe.getNodepattern(), info.parameters, info.conditions);
				for (PatternElementChain chain : pe.getChain()) {
					String targetVar = processNodePattern(chain.getNodePattern(), info.parameters, info.conditions);
					processRelationshipPattern(chain.getRelationshipPattern(), sourceVar, targetVar, info.conditions);
					sourceVar = targetVar;
				}
			}
		}
		return info;
	}

	private void processMatchPattern(Pattern pattern, StringBuilder out) {
		PatternInfo info = extractPatternInfo(pattern);
		if (info.parameters.isEmpty() && info.conditions.isEmpty()) return;

		out.append("pred match").append(matchCounter++).append("(");
		appendParameterDeclarations(out, info.parameters);
		out.append(") <-> \n");
		appendConditions(out, info.conditions);
		out.append(".\n\n");
	}

	private String processNodePattern(NodePattern nodePattern, List<String> parameters, List<String> conditions) {
		String varName = nodePattern.getVariable() != null ? nodePattern.getVariable().getName() : "n_anon_" + (anonVarCounter++);

		if (!parameters.contains(varName)) {
			parameters.add(varName);
		}

		if (nodePattern.getNodeLabels() != null && !nodePattern.getNodeLabels().isEmpty()) {
			for (NodeLabel label : nodePattern.getNodeLabels()) {
				conditions.add(cleanName(label.getLabelName()) + "(" + varName + ")");
			}
		}
		return varName;
	}

	private void processRelationshipPattern(RelationshipPattern relPattern, String sourceVar, String targetVar, List<String> conditions) {
		if (relPattern.getDetail() != null) {
			RelationshipDetail detail = relPattern.getDetail();
			String relType = null;
			String relVar = null;

			if (detail.getRelTypeNames() != null && !detail.getRelTypeNames().isEmpty()) {
				relType = cleanName(detail.getRelTypeNames().getFirst());
				relVar = detail.getVariable() != null ?
						detail.getVariable().getName() : "relationship" + (anonVarCounter++);
			} else if (detail.getVariable() != null) {
				relType = cleanName(detail.getVariable().getName());
				relVar = "relationship" + (anonVarCounter++);
			}

			if (relType != null) {
				conditions.add(relType + "(" + relVar + ")");

				if (relPattern.isIncoming()) {
					conditions.add(relType + "::source(" + relVar + ", " + targetVar + ")");
					conditions.add(relType + "::target(" + relVar + ", " + sourceVar + ")");
				} else {
					conditions.add(relType + "::source(" + relVar + ", " + sourceVar + ")");
					conditions.add(relType + "::target(" + relVar + ", " + targetVar + ")");
				}
			}
		}
	}

	private void appendParameterDeclarations(StringBuilder out, List<String> params) {
		for (int i = 0; i < params.size(); i++) {
			out.append("Node ").append(params.get(i));
			if (i < params.size() - 1) out.append(", ");
		}
	}

	private void appendParameterArguments(StringBuilder out, List<String> params) {
		for (int i = 0; i < params.size(); i++) {
			out.append(params.get(i));
			if (i < params.size() - 1) out.append(", ");
		}
	}

	private void appendConditions(StringBuilder out, List<String> conditions) {
		for (int i = 0; i < conditions.size(); i++) {
			out.append("    ").append(conditions.get(i));
			if (i < conditions.size() - 1) {
				out.append(", \n");
			}
		}
	}

	private void appendProperties(StringBuilder buffer, TypePropertyMap propertyMap) {
		if (propertyMap != null && propertyMap.getProperties() != null) {
			for (TypeProperty prop : propertyMap.getProperties()) {
				buffer.append("    ").append(mapType(prop.getType())).append(" ").append(prop.getName()).append("\n");
			}
		}
	}

	private String cleanName(String name) {
		if (name == null) return "";
		return name.startsWith(":") ? name.substring(1) : name;
	}

	private String mapType(String cypherType) {
		if (cypherType == null) return "string";
		return switch (cypherType.toUpperCase()) {
			case "INTEGER", "INT", "DATE" -> "int";
			case "BOOLEAN", "BOOL" -> "bool";
			case "FLOAT", "DOUBLE", "REAL" -> "real";
			default -> "string";
		};
	}
}
