/*
 * openCypher Xtext - Slizaa Static Software Analysis Tools
 * Copyright © 2024 Code-Kontor GmbH and others (slizaa@codekontor.io)
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
import org.eclipse.emf.ecore.EObject;
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
		Map<String, String> varTypes = new LinkedHashMap<>();
	}

	private static class CheckInfo {
		String name;
		List<String> parameters;
		CheckInfo(String name, List<String> parameters) {
			this.name = name;
			this.parameters = parameters;
		}
	}

	public String convertToRefinery(Cypher model, boolean isCwa) {
		if (model == null) return "// Error: Model root container is empty.";

		this.matchCounter = 1;
		this.constraintCounter = 1;

		StringBuilder out = new StringBuilder();
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

		if (isCwa) {
			out.append("abstract class Node.\n\n");
		} else {
			out.append("class Node.\n\n");
		}

		Set<String> explicitClasses = new HashSet<>();
		Set<String> allReferencedTypes = new HashSet<>();
		Map<String, List<String>> propagationRules = new LinkedHashMap<>();

		for (Statement stmt : model.getStatements()) {
			if (stmt instanceof NodeTypeDefinition nodeType && !(nodeType instanceof RelationshipTypeDefinition)) {
				if (nodeType.getName() != null) {
					explicitClasses.add(cleanName(nodeType.getName()));
				}
				if (nodeType.getSuperTypes() != null) {
					for (String st : nodeType.getSuperTypes()) {
						allReferencedTypes.add(cleanName(st));
					}
				}
			} else if (stmt instanceof RelationshipTypeDefinition rel) {
				if (rel.getLeft() != null) explicitClasses.add(cleanName(rel.getLeft().getName()));
				if (rel.getRight() != null) explicitClasses.add(cleanName(rel.getRight()));
			}
		}

		List<NodeLabel> allLabels = EcoreUtil2.getAllContentsOfType(model, NodeLabel.class);
		for (NodeLabel label : allLabels) {
			allReferencedTypes.add(cleanName(label.getLabelName()));
		}
		allReferencedTypes.addAll(explicitClasses);

		StringBuilder classesBuffer = new StringBuilder();
		StringBuilder relationshipsBuffer = new StringBuilder();
		boolean hasRelationships = false;

		Set<String> generatedClasses = new HashSet<>();

		for (Statement statement : model.getStatements()) {
			if (statement instanceof NodeTypeDefinition nodeType && !(nodeType instanceof RelationshipTypeDefinition)) {
				String className = cleanName(nodeType.getName());
				if (className.isEmpty()) continue;

				generatedClasses.add(className);
				List<String> classSupertypes = new ArrayList<>();
				List<String> predSupertypes = new ArrayList<>();

				if (nodeType.getSuperTypes() != null) {
					for (String st : nodeType.getSuperTypes()) {
						String cleanSt = cleanName(st);
						if (isCwa || explicitClasses.contains(cleanSt)) {
							classSupertypes.add(cleanSt);
						} else {
							predSupertypes.add(cleanSt);
						}
					}
				}

				String extendsClause = classSupertypes.isEmpty() ? "Node" : String.join(", ", classSupertypes);
				boolean hasProps = nodeType.getProperties() != null
						&& nodeType.getProperties().getProperties() != null
						&& !nodeType.getProperties().getProperties().isEmpty();

				if (hasProps) {
					classesBuffer.append("class ").append(className).append(" extends ").append(extendsClause).append(" {\n");
					appendProperties(classesBuffer, nodeType.getProperties());
					classesBuffer.append("}\n\n");
				} else {
					classesBuffer.append("class ").append(className).append(" extends ").append(extendsClause).append(".\n\n");
				}

				if (!predSupertypes.isEmpty()) {
					propagationRules.put(className, predSupertypes);
				}
			}
			else if (statement instanceof RelationshipTypeDefinition rel) {
				hasRelationships = true;
				String sourceClass = rel.getLeft() != null ? cleanName(rel.getLeft().getName()) : "Node";
				String targetClass = rel.getRight() != null ? cleanName(rel.getRight()) : "Node";
				String relationName = cleanName(rel.getRel());

				relationshipsBuffer.append("class ").append(relationName).append(" extends RELATIONSHIP {\n");
				relationshipsBuffer.append("    ").append(sourceClass).append("[1] source\n");
				relationshipsBuffer.append("    ").append(targetClass).append("[1] target\n");
				appendProperties(relationshipsBuffer, rel.getProperties());
				relationshipsBuffer.append("}\n\n");
			}
		}

		for (String explicit : explicitClasses) {
			if (!generatedClasses.contains(explicit)) {
				classesBuffer.append("class ").append(explicit).append(" extends Node.\n\n");
				generatedClasses.add(explicit);
			}
		}

		for (String type : allReferencedTypes) {
			if (!explicitClasses.contains(type)) {
				if (isCwa) {
					classesBuffer.append("class ").append(type).append(" extends Node.\n\n");
				} else {
					classesBuffer.append("pred ").append(type).append("(Node n).\n\n");
				}
			}
		}

		out.append(classesBuffer);

		for (Map.Entry<String, List<String>> entry : propagationRules.entrySet()) {
			String className = entry.getKey();
			List<String> superTypes = entry.getValue();

			String ruleName = Character.toLowerCase(className.charAt(0)) + className.substring(1) + "ImpliedTypes";
			out.append("propagation rule ").append(ruleName).append("(").append(className).append(" p) ==> ");

			for (int i = 0; i < superTypes.size(); i++) {
				out.append(superTypes.get(i)).append("(p)");
				if (i < superTypes.size() - 1) out.append(", ");
			}
			out.append(".\n\n");
		}

		if (hasRelationships) {
			out.append("abstract class RELATIONSHIP.\n\n");
			out.append(relationshipsBuffer);
		}

		processQueries(model, out);
		processConstraints(model, out);

		return out.toString().trim() + "\n";
	}

	private void processQueries(Cypher model, StringBuilder out) {
		List<Match> matchClauses = EcoreUtil2.getAllContentsOfType(model, Match.class);
		for (Match match : matchClauses) {
			processMatchPattern(match.getPattern(), match.getWhere(), out);
		}
	}

	private void processConstraints(Cypher model, StringBuilder out) {
		List<ConstraintStatement> constraints = EcoreUtil2.getAllContentsOfType(model, ConstraintStatement.class);
		List<CheckInfo> checkInfos = new ArrayList<>();

		for (ConstraintStatement stmt : constraints) {
			PatternInfo cInfo = extractPatternInfo(stmt.getConstraintPattern(), stmt.getConstraintWhere());
			PatternInfo iInfo = extractPatternInfo(stmt.getImpliesPattern(), stmt.getImpliesWhere());

			for (String param : cInfo.parameters) {
				if (!iInfo.parameters.contains(param)) {
					iInfo.conditions.addFirst("Node({" + param + "})");
				}
			}

			int currentId = constraintCounter++;
			String checkName = "check" + currentId;
			checkInfos.add(new CheckInfo(checkName, cInfo.parameters));

			out.append("pred constraint").append(currentId).append("(");
			appendParameterDeclarations(out, cInfo.parameters);
			out.append(") <-> \n");
			appendConditions(out, cInfo.conditions, cInfo.parameters);
			out.append(".\n\n");

			out.append("pred implication").append(currentId).append("(");
			appendParameterDeclarations(out, cInfo.parameters);
			out.append(") <-> \n");
			appendConditions(out, iInfo.conditions, cInfo.parameters);
			out.append(".\n\n");

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

	private PatternInfo extractPatternInfo(Pattern pattern, Where where) {
		PatternInfo info = new PatternInfo();
		if (pattern == null || pattern.getPatterns().isEmpty()) return info;

		anonVarCounter = 1;

		for (PatternPart part : pattern.getPatterns()) {
			PatternElement pe = null;
			if (part instanceof PatternElement patternElement) {
				pe = patternElement;
			} else if (part.getPart() instanceof PatternElement patternElement) {
				pe = patternElement;
			}

			if (pe != null) {
				String sourceVar = processNodePattern(pe.getNodepattern(), info.parameters, info.conditions, info.varTypes);
				for (PatternElementChain chain : pe.getChain()) {
					String targetVar = processNodePattern(chain.getNodePattern(), info.parameters, info.conditions, info.varTypes);
					processRelationshipPattern(chain.getRelationshipPattern(), sourceVar, targetVar, info.conditions, info.varTypes);
					sourceVar = targetVar;
				}
			}
		}

		if (where != null && where.getExpression() != null) {
			flattenAnds(where.getExpression(), info.conditions, info.varTypes);
		}

		return info;
	}

	private void processMatchPattern(Pattern pattern, Where where, StringBuilder out) {
		PatternInfo info = extractPatternInfo(pattern, where);
		if (info.parameters.isEmpty() && info.conditions.isEmpty()) return;

		out.append("pred match").append(matchCounter++).append("(");
		appendParameterDeclarations(out, info.parameters);
		out.append(") <-> \n");
		appendConditions(out, info.conditions, info.parameters);
		out.append(".\n\n");
	}

	private String processNodePattern(NodePattern nodePattern, List<String> parameters, List<String> conditions, Map<String, String> varTypes) {
		String varName = nodePattern.getVariable() != null ? nodePattern.getVariable().getName() : "n_anon_" + (anonVarCounter++);

		if (!parameters.contains(varName)) {
			parameters.add(varName);
		}

		if (nodePattern.getNodeLabels() != null && !nodePattern.getNodeLabels().isEmpty()) {
			varTypes.put(varName, cleanName(nodePattern.getNodeLabels().get(0).getLabelName()));
			for (NodeLabel label : nodePattern.getNodeLabels()) {
				conditions.add(cleanName(label.getLabelName()) + "({" + varName + "})");
			}
		} else {
			varTypes.putIfAbsent(varName, "Node");
		}
		return varName;
	}

	private void processRelationshipPattern(RelationshipPattern relPattern, String sourceVar, String targetVar, List<String> conditions, Map<String, String> varTypes) {
		if (relPattern.getDetail() != null) {
			RelationshipDetail detail = relPattern.getDetail();
			String relType = null;
			String relVar = null;

			if (detail.getRelTypeNames() != null && !detail.getRelTypeNames().isEmpty()) {
				relType = cleanName(detail.getRelTypeNames().getFirst());
				relVar = detail.getVariable() != null ? detail.getVariable().getName() : "relationship" + (anonVarCounter++);
			} else if (detail.getVariable() != null) {
				relType = cleanName(detail.getVariable().getName());
				relVar = "relationship" + (anonVarCounter++);
			}

			if (relType != null) {
				varTypes.put(relVar, relType);
				conditions.add(relType + "({" + relVar + "})");

				if (relPattern.isIncoming()) {
					conditions.add(relType + "::source({" + relVar + "}, {" + targetVar + "})");
					conditions.add(relType + "::target({" + relVar + "}, {" + sourceVar + "})");
				} else {
					conditions.add(relType + "::source({" + relVar + "}, {" + sourceVar + "})");
					conditions.add(relType + "::target({" + relVar + "}, {" + targetVar + "})");
				}
			}
		}
	}

	private void flattenAnds(Expression expr, List<String> conditions, Map<String, String> varTypes) {
		if (expr instanceof AndExpression andExpr) {
			flattenAnds(andExpr.getLeft(), conditions, varTypes);
			flattenAnds(andExpr.getRight(), conditions, varTypes);
		} else {
			String exprStr = processExpression(expr, varTypes);
			if (!exprStr.isEmpty()) {
				conditions.add(exprStr);
			}
		}
	}

	private String getVariableName(Expression expr) {
		if (expr instanceof VariableRef varRef && varRef.getVariableName() != null) {
			return varRef.getVariableName();
		}
		for (EObject child : expr.eContents()) {
			if (child instanceof Expression childExpr) {
				String res = getVariableName(childExpr);
				if (!res.isEmpty()) return res;
			}
		}
		return "";
	}

	private String processExpression(EObject expr, Map<String, String> varTypes) {
		if (expr == null) return "";

		if (expr instanceof ComparisonExpression compExpr) {
			String op = compExpr.getOperator();
			String leftStr = processExpression(compExpr.getLeft(), varTypes);
			String rightStr = processExpression(compExpr.getRight(), varTypes);

			if ("=".equals(op)) op = "==";
			else if ("<>".equals(op)) op = "!=";

			return leftStr + " " + op + " " + rightStr;
		}

		if (expr instanceof PropertyLookupExpression propExpr) {
			String varName = getVariableName(propExpr.getLeft());
			String propName = propExpr.getPropertyLookups().get(0).getPropertyKeyName();
			String type = varTypes.getOrDefault(varName, "Node");

			return type + "::" + propName + "({" + varName + "})";
		}

		if (expr instanceof OrExpression orExpr) {
			return "(" + processExpression(orExpr.getLeft(), varTypes) + " ; " + processExpression(orExpr.getRight(), varTypes) + ")";
		}

		if (expr instanceof NotExpression notExpr) {
			return "!(" + processExpression(notExpr.getLeft(), varTypes) + ")";
		}

		if (expr instanceof VariableRef varRef) {
			if (varRef.getVariableName() != null) {
				return "{" + varRef.getVariableName() + "}";
			}
		}

		if (expr instanceof StringLiteral strLit) {
			return strLit.getValue();
		}

		if (expr instanceof NumberLiteral numLit) {
			return numLit.getValue();
		}

		if (expr instanceof BooleanLiteral boolLit) {
			return boolLit.getValue().toLowerCase();
		}

		if (expr instanceof ParenthesizedExpression parenExpr) {
			return "(" + processExpression(parenExpr.getExpression(), varTypes) + ")";
		}

		for (EObject child : expr.eContents()) {
			String res = processExpression(child, varTypes);
			if (res != null && !res.isEmpty()) {
				return res;
			}
		}

		return "";
	}

	private void appendConditions(StringBuilder out, List<String> conditions, List<String> exportedParams) {
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{([^}]+)}");
		for (int i = 0; i < conditions.size(); i++) {
			String cond = conditions.get(i);
			java.util.regex.Matcher m = p.matcher(cond);
			StringBuilder sb = new StringBuilder();

			while (m.find()) {
				String varName = m.group(1);
				m.appendReplacement(sb, varName);
			}
			m.appendTail(sb);

			out.append("    ").append(sb);
			if (i < conditions.size() - 1) {
				out.append(", \n");
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
			case "BOOLEAN", "BOOL" -> "boolean";
			case "FLOAT", "DOUBLE", "REAL" -> "real";
			default -> "string";
		};
	}
}
