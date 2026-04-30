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

public class OpenCypherIdeContentProposalProvider extends IdeContentProposalProvider {

	@Inject
	private OpenCypherGrammarAccess grammarAccess;

	@Inject
	private OpenCypherTemplateProposalProvider templateProvider;

	@Override
	public void createProposals(RuleCall ruleCall, ContentAssistContext context,
			IIdeContentProposalAcceptor acceptor) {

		AbstractRule rule = ruleCall.getRule();

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
