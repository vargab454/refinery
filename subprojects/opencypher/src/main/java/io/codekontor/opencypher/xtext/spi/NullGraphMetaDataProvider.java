///**
// * openCypher Xtext - Slizaa Static Software Analysis Tools
// * Copyright © ${year} Code-Kontor GmbH and others (slizaa@codekontor.io)
// *
// * This program and the accompanying materials are made available under the
// * terms of the Eclipse Public License 2.0 which is available at
// * http://www.eclipse.org/legal/epl-2.0.
// *
// * SPDX-License-Identifier: EPL-2.0
// *
// * Contributors:
// *  Code-Kontor GmbH - initial API and implementation
// */
//package io.codekontor.opencypher.xtext.spi;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//import io.codekontor.opencypher.xtext.api.IGraphDatabaseMetaDataProvider;
//
///**
// * The NullGraphMetaDataProvider class is a classic implementation of the Null Object Pattern.
// *
// * It serves as a default, do-nothing provider. Prevents NullPointerExceptions
// * by providing valid but empty data structures instead of null.
// */
//public class NullGraphMetaDataProvider implements IGraphDatabaseMetaDataProvider {
//
//	/**
//	 * Retrieves the labels used for nodes in the graph.
//	 *
//	 * @return An immutable empty List. This tells the caller that no node labels are
//	 * currently known or available in the context.
//	 */
//	@Override
//	public List<String> getNodeLabels() {
//		return Collections.emptyList();
//	}
//
//	/**
//	 * Retrieves all property keys existing in the graph.
//	 *
//	 * @return An immutable empty List, indicating no properties are available.
//	 */
//	@Override
//	public List<String> getPropertyKeys() {
//		return Collections.emptyList();
//	}
//
//	/**
//	 * Retrieves the types of relationships defined in the graph.
//	 *
//	 * @return An immutable empty List.
//	 */
//	@Override
//	public List<String> getRelationshipTypes() {
//		return Collections.emptyList();
//	}
//
//	/**
//	 * Retrieves the available Cypher functions and their descriptions/signatures.
//	 *
//	 * @return An immutable empty Map, signifying that no custom or built-in
//	 * functions are registered in this provider.
//	 */
//	@Override
//	public Map<String, String> getFunctions() {
//		return Collections.emptyMap();
//	}
//}
//
