/**
 * openCypher Xtext IDE - Slizaa Static Software Analysis Tools
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
package io.codekontor.opencypher.xtext.ide;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.ide.editor.contentassist.AbstractIdeTemplateProposalProvider;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.ide.editor.contentassist.IIdeContentProposalAcceptor;
import org.eclipse.xtext.ide.editor.contentassist.IdeContentProposalPriorities;

import com.google.inject.Inject;

import io.codekontor.opencypher.xtext.api.IGraphDatabaseMetaDataProvider;

/**
 * Custom provider for template-based content assist proposals in the openCypher editor.
 * This class extends the standard Xtext IDE template provider to offer domain-specific
 * suggestions like node labels, relationship types, and common Cypher syntax snippets.
 */
public class OpenCypherTemplateProposalProvider extends AbstractIdeTemplateProposalProvider {

	/**
	 * Handles the prioritization of suggestions in the content assist popup.
	 */
	@Inject
	private IdeContentProposalPriorities proposalPriorities;

	/**
	 * Provides access to the schema of the target graph database (labels, keys, types).
	 * This allows the editor to suggest actual values existing in the database.
	 */
	@Inject
	private IGraphDatabaseMetaDataProvider graphMetaDataProvider;

	/**
	 * Creates proposals for Node Labels (e.g., :Person, :City).
	 * It fetches available labels from the meta-data provider and suggests them as VALUE types.
	 */
	public void createLabelNameRuleProposal(ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		nullSafe(() -> graphMetaDataProvider.getNodeLabels(), label -> acceptProposal(label, label, template(label),
				context, acceptor, true, ContentAssistEntry.KIND_VALUE));
	}

	/**
	 * Creates proposals for Property Keys (e.g., name, age).
	 * Uses the metadata provider to list keys used in the current graph database.
	 */
	public void createPropertyKeyNameRuleProposal(ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		nullSafe(() -> graphMetaDataProvider.getPropertyKeys(),
				key -> acceptProposal(key, key, template(key), context, acceptor, true, ContentAssistEntry.KIND_VALUE));
	}

	/**
	 * Creates proposals for Relationship Types (e.g., :FOLLOWS, :WORKS_AT).
	 */
	public void createRelTypeNameRuleProposal(ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		nullSafe(() -> graphMetaDataProvider.getRelationshipTypes(),
				relationshipType -> acceptProposal(relationshipType, relationshipType, template(relationshipType),
						context, acceptor, true, ContentAssistEntry.KIND_VALUE));
	}

	/**
	 * Generates a snippet for the MATCH clause.
	 * The template "MATCH (${1}) ${0}" includes tab-stops:
	 * ${1} is the first cursor position for the pattern, ${0} is where the cursor ends up.
	 */
	public void createMatchRuleProposal(ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		acceptProposal("match", "Match()", template("MATCH (${1}) ${0}"), context, acceptor, true,
				ContentAssistEntry.KIND_SNIPPET);
	}

	/**
	 * Proposal for a standalone Node Pattern, e.g., (n).
	 */
	public void createNodePatternRuleProposal(ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		acceptProposal("(NodePatternRuleProposal)", "(node1)", template("(${1}) ${0}"), context, acceptor, true,
				ContentAssistEntry.KIND_SNIPPET);
	}

	/**
	 * Proposal for a Pattern Part snippet.
	 */
	public void createPatternPartRuleProposal(ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		acceptProposal("(PatternPartRuleProposal)", "(node1)", template("(${1}) ${0}"), context, acceptor, true,
				ContentAssistEntry.KIND_SNIPPET);
	}

	/**
	 * Creates snippets for directional relationships.
	 * Provides both outgoing (-[]->) and incoming (<-[]-) relation templates.
	 */
	public void createPatternElementChainRuleProposal(ContentAssistContext context,
													  IIdeContentProposalAcceptor acceptor) {

		acceptProposal("-[]->", "Outgoing Relation", template("-[${1}]-> ${0}"), context, acceptor, true,
				ContentAssistEntry.KIND_SNIPPET);

		acceptProposal("<-[]-", "Incoming Relation", template("<-[${1}]- ${0}"), context, acceptor, true,
				ContentAssistEntry.KIND_SNIPPET);
	}

	/**
	 * Dynamically generates proposals for available Cypher functions (e.g., count(), avg()).
	 * It strips the parameters from the key to create a clean template for invocation.
	 */
	public void createFunctionInvocationRuleProposal(ContentAssistContext context,
													 IIdeContentProposalAcceptor acceptor) {

		graphMetaDataProvider.getFunctions().forEach((key, value) -> {
			// Extract function name before the parenthesis for the completion template
			acceptProposal(key, value, template(key.substring(0, key.indexOf('('))), context, acceptor, true,
					ContentAssistEntry.KIND_FUNCTION);
		});
	}

	/**
	 * Helper method to configure and accept a proposal entry.
	 *
	 * @param name The text displayed in the suggestion list.
	 * @param description A short explanation of the proposal.
	 * @param template The actual code structure to be inserted.
	 * @param context The current state of the editor.
	 * @param acceptor The component that collects and displays the valid proposals.
	 * @param adaptIndentation Whether the inserted text should match the current line's indentation.
	 * @param kind The category of the proposal (Snippet, Value, Function, etc.).
	 */
	protected void acceptProposal(final String name, final String description, final StringConcatenationClient template,
								  final ContentAssistContext context, final IIdeContentProposalAcceptor acceptor,
								  final boolean adaptIndentation, final String kind) {

		// Create the entry from the template
		final ContentAssistEntry entry = this.createProposal(template, context, adaptIndentation);

		// Check if the proposal is valid at the current cursor position
		boolean _canAcceptProposal = this.canAcceptProposal(entry, context);
		if (_canAcceptProposal) {
			entry.setLabel(name);
			entry.setDescription(description);
			entry.setKind(kind);
			// Submit to acceptor with the default priority for its kind
			acceptor.accept(entry, this.proposalPriorities.getDefaultPriority(entry));
		}
	}

	/**
	 * Wraps a raw String into a StringConcatenationClient.
	 * This is required by the Xtext template engine to handle complex multi-line
	 * concatenations and indentation properly.
	 */
	private StringConcatenationClient template(final String string) {
		return new StringConcatenationClient() {
			@Override
			protected void appendTo(StringConcatenationClient.TargetStringConcatenation target) {
				target.append(string);
			}
		};
	};

	/**
	 * Utility method to perform actions safely on metadata lists.
	 * It ensures that the metadata provider is initialized and the supplier
	 * actually returns a list before attempting to iterate through it.
	 *
	 * @param supplier A lambda or method reference providing the list of strings (e.g., labels).
	 * @param consumer A logic to be applied to each non-null item in the list.
	 */
	private void nullSafe(Supplier<List<String>> supplier, Consumer<String> consumer) {
		if (graphMetaDataProvider != null) {
			List<String> list = supplier.get();
			if (list != null) {
				list.forEach(item -> {
					consumer.accept(item);
				});
			}
		}
	}
}
