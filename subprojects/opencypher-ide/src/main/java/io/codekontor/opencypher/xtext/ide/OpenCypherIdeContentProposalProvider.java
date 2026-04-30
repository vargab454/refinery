/*
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

import io.codekontor.opencypher.xtext.services.OpenCypherGrammarAccess;
import javax.inject.Inject;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.IIdeContentProposalAcceptor;
import org.eclipse.xtext.ide.editor.contentassist.IdeContentProposalProvider;

/**
 * Main Content Proposal Provider for the OpenCypher IDE.
 * It intercepts the standard Xtext proposal mechanism to provide context-aware
 * suggestions based on the specific parser rule currently being processed.
 */
public class OpenCypherIdeContentProposalProvider extends IdeContentProposalProvider {

	/**
	 * Access to the generated grammar rules. Used to identify which rule
	 * triggered the content assist request.
	 */
	@Inject
	private OpenCypherGrammarAccess grammarAccess;

	/**
	 * Delegate provider that contains the actual template/snippet logic.
	 */
	@Inject
	private OpenCypherTemplateProposalProvider templateProvider;

	/**
	 * Overrides the default proposal creation.
	 *
	 * In Xtend, this used 'dispatch' to separate logic by type. In Java, we use
	 * an if-else structure to check if the current rule matches a specific
	 * grammar rule (e.g., MatchRule, LabelNameRule).
	 *
	 * @param ruleCall Represents the current rule being called in the grammar.
	 * @param context Provides editor state (cursor position, text around it).
	 * @param acceptor Collects and displays the generated proposals.
	 */
	@Override
	public void createProposals(RuleCall ruleCall, ContentAssistContext context,
			IIdeContentProposalAcceptor acceptor) {

		AbstractRule rule = ruleCall.getRule();

		// Check the triggered rule against specific grammar rules to provide relevant snippets or metadata-based suggestions.
		if (rule == grammarAccess.getMatchRule()) {
			templateProvider.createMatchRuleProposal(context, acceptor);
		} else if (rule == grammarAccess.getLabelNameRule()) {
			templateProvider.createLabelNameRuleProposal(context, acceptor);
		} else if (rule == grammarAccess.getPatternPartRule()) {
			templateProvider.createPatternPartRuleProposal(context, acceptor);
		} else if (rule == grammarAccess.getNodePatternRule()) {
			templateProvider.createNodePatternRuleProposal(context, acceptor);
		} else if (rule == grammarAccess.getFunctionInvocationRule()) {
			templateProvider.createFunctionInvocationRuleProposal(context, acceptor);
		} else if (rule == grammarAccess.getPatternElementChainRule()) {
			templateProvider.createPatternElementChainRuleProposal(context, acceptor);
		} else if (rule == grammarAccess.getPropertyKeyNameRule()) {
			templateProvider.createPropertyKeyNameRuleProposal(context, acceptor);
		} else if (rule == grammarAccess.getRelTypeNameRule()) {
			templateProvider.createRelTypeNameRuleProposal(context, acceptor);
		} else {
			super.createProposals(ruleCall, context, acceptor);
		}
	}
}
