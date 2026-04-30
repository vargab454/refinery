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

public class SYMBOLIC_NAME_XValueConverter extends AbstractValueConverter<String> {

	@Inject
	private OpenCypherIDEscapeHelper helper;

	@Override
	public String toString(String value) throws ValueConverterException {
		Set<Character> invalidChars = helper.collectInvalidCharacters(value);
		if (invalidChars == null) {
			return helper.toEscapedString(value);
		} else {
			throw new ValueConverterException(getInvalidCharactersMessage(value, invalidChars), null, null);
		}
	}

	protected String getInvalidCharactersMessage(String value, Set<Character> invalidChars) {
		String chars = invalidChars.stream()
				.map(c -> "'" + c + "' (0x" + Integer.toHexString(c) + ")")
				.collect(Collectors.joining(", "));
		return "ID '" + value + "' contains invalid characters: " + chars;
	}

	@Override
	public String toValue(String string, INode node) throws ValueConverterException {
		return helper.toValue(string);
	}
}
