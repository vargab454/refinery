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

/**
 * Custom ID value converter that handles identifier escaping logic for openCypher.
 * Extends the standard IgnoreCaseIDValueConverter to provide Cypher-specific
 * escaping rules and compatibility with Neo4j's naming conventions.
 */
public class OpenCypherIDValueConverter extends IgnoreCaseIDValueConverter {

	/**
	 * Delegate helper that contains the actual string manipulation logic.
	 */
	@Inject
	private OpenCypherIDEscapeHelper helper;

	/**
	 * Converts the textual representation from the editor into a String value for the model.
	 */
	@Override
	public String toValue(String string, INode node) {
		return helper.toValue(string);
	}

	/**
	 * Overridden to bypass standard Xtext ID validation.
	 * In Neo4j Cypher (as of 3.3.1), an empty string (``) is considered a valid identifier.
	 * By leaving this empty, we prevent the superclass from throwing a ValueConverterException when it encounters empty IDs.
	 */
	@Override
	protected void assertValidValue(String value) {}

	/**
	 * Determines if the given value needs to be escaped based on its content.
	 */
	@Override
	protected boolean mustEscape(String value) {
		return helper.mustEscape(value);
	}

	/**
	 * Performs the actual escaping of a String value.
	 */
	@Override
	protected String toEscapedString(String value) {
		return helper.toEscapedString(value);
	}

	/**
	 * Identifies characters within the string that are invalid and cannot be part of an identifier even if escaped.
	 */
	@Override
	protected Set<Character> collectInvalidCharacters(String value) {
		return helper.collectInvalidCharacters(value);
	}
}
