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
package io.codekontor.opencypher.xtext.conversion;

import com.google.inject.Inject;
import java.util.Set;
import org.eclipse.xtext.conversion.impl.IgnoreCaseIDValueConverter;
import org.eclipse.xtext.nodemodel.INode;

public class OpenCypherIDValueConverter extends IgnoreCaseIDValueConverter {

	@Inject
	private OpenCypherIDEscapeHelper helper;

	@Override
	public String toValue(String string, INode node) {
		return helper.toValue(string);
	}

	@Override
	protected void assertValidValue(String value) {
		// In Neo4j Cypher (as of 3.3.1) `` (empty string) is a valid ID.
		// Omit call to super.assertValidValue, which would throw a ValueConverterException on empty values.
	}

	@Override
	protected boolean mustEscape(String value) {
		return helper.mustEscape(value);
	}

	@Override
	protected String toEscapedString(String value) {
		return helper.toEscapedString(value);
	}

	@Override
	protected Set<Character> collectInvalidCharacters(String value) {
		return helper.collectInvalidCharacters(value);
	}
}
