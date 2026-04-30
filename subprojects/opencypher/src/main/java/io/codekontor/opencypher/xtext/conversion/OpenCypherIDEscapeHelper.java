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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Set;
import java.util.regex.Pattern;

@Singleton
public class OpenCypherIDEscapeHelper {

	private static final Pattern UNESCAPED_ID_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
	private static final char ESCAPE_CHAR = '`';
	private static final Set<Character> FORBIDDEN_CHARACTERS = ImmutableSet.of((char) 0, ESCAPE_CHAR);

	public String toValue(String string) {
		if (string == null) {
			return null;
		} else if (string.length() > 0 && string.charAt(0) == ESCAPE_CHAR) {
			int length = string.length();
			return string.substring(1, length - 1);
		} else {
			return string;
		}
	}

	public boolean mustEscape(String value) {
		return !UNESCAPED_ID_PATTERN.matcher(value).matches();
	}

	public String toEscapedString(String value) {
		if (mustEscape(value)) {
			return ESCAPE_CHAR + value + ESCAPE_CHAR;
		} else {
			return value;
		}
	}

	public Set<Character> collectInvalidCharacters(String value) {
		ImmutableSet.Builder<Character> invalidCharactersFound = ImmutableSet.builder();
		int length = value.length();
		for (int i = 0; i < length; i++) {
			char character = value.charAt(i);
			if (FORBIDDEN_CHARACTERS.contains(character)) {
				invalidCharactersFound.add(character);
			}
		}
		Set<Character> invalidChars = invalidCharactersFound.build();
		if (invalidChars.isEmpty()) {
			return null;
		} else {
			return invalidChars;
		}
	}
}
