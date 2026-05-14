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

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;

/**
 * Custom value converter for the SYMBOLIC_NAME_X rule.
 * It manages the conversion between the model representation and the textual representation in the editor, including
 * escaping logic for Cypher identifiers.
 */
public class SYMBOLIC_NAME_XValueConverter extends AbstractValueConverter<String> {

	/**
	 * Helper component used to handle the specific escaping rules of openCypher.
	 */
	@Inject
	private OpenCypherIDEscapeHelper helper;

	/**
	 * Converts the model value to its textual representation for the editor.
	 * This is called during serialization.
	 */
	@Override
	public String toString(String value) throws ValueConverterException {
		Set<Character> invalidChars = helper.collectInvalidCharacters(value);
		if (invalidChars == null) {
			return helper.toEscapedString(value);
		} else {
			throw new ValueConverterException(getInvalidCharactersMessage(value, invalidChars), null, null);
		}
	}

	/**
	 * Generates a detailed error message listing the illegal characters found in the identifier.
	 *
	 * @param value The problematic identifier string
	 * @param invalidChars The set of characters that caused the validation to fail
	 * @return A formatted error string including character codes in hexadecimal
	 */
	protected String getInvalidCharactersMessage(String value, Set<Character> invalidChars) {
		String chars = invalidChars.stream()
				.map(c -> "'" + c + "' (0x" + Integer.toHexString(c) + ")")
				.collect(Collectors.joining(", "));
		return "ID '" + value + "' contains invalid characters: " + chars;
	}

	/**
	 * Converts the textual representation from the editor into a clean String for the model.
	 */
	@Override
	public String toValue(String string, INode node) throws ValueConverterException {
		return helper.toValue(string);
	}
}
