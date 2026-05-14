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
 * Code-Kontor GmbH - initial API and implementation
 */
package io.codekontor.opencypher.xtext.conversion;

import com.google.inject.Inject;
import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;

/**
 * The ValueConverterService is the registry for all value converters in the language.
 * By extending DefaultTerminalConverters, we inherit standard converters for
 * rules like ID, INT, and STRING, while allowing us to register our own.
 */
public class OpenCypherValueConverterService extends DefaultTerminalConverters {

	/**
	 * Injects the converter implementation for symbolic names.
	 */
	@Inject
	private SYMBOLIC_NAME_XValueConverter symbolNameXValueConverter;

	/**
	 * Registers the custom converter for the SYMBOLIC_NAME_X grammar rule.
	 *
	 * The @ValueConverter annotation: Whenever the SYMBOLIC_NAME_X rule is encountered, the converter returned by
	 * this method is used.
	 *
	 * @return The converter instance responsible for escaping/unescaping Cypher identifiers.
	 */
	@ValueConverter(rule = "SYMBOLIC_NAME_X")
	public IValueConverter<String> SYMBOLIC_NAME_X() { return symbolNameXValueConverter; }
}
