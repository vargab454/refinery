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

import org.eclipse.xtext.ide.editor.contentassist.IdeContentProposalProvider;

/**
 * The OpenCypherIdeModule is used to register and configure components,
 * specifically for the IDE part of the Xtext framework.
 */
public class OpenCypherIdeModule extends AbstractOpenCypherIdeModule {

	/**
	 * Binds the custom Content Proposal Provider to the Xtext IDE framework.
	 *
	 * @return The class type of the custom proposal provider implementation.
	 */
	public Class<? extends IdeContentProposalProvider> bindIdeContentProposalProvider() {
		return OpenCypherIdeContentProposalProvider.class;
	}
}
