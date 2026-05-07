/**
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
package io.codekontor.opencypher.xtext.scoping;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IGlobalScopeProvider;
import org.eclipse.xtext.scoping.IScope;

import com.google.common.base.Predicate;

/**
 * NullGlobalScopeProvider is a specialized implementation of the Xtext IGlobalScopeProvider.
 *
 * In Xtext, a Global Scope is used to find elements outside the current file.
 * This Null implementation is used when you want to explicitly disable global
 * lookups, ensuring that the editor only sees symbols defined within the local file.
 */
public class NullGlobalScopeProvider implements IGlobalScopeProvider {

	/**
	 * Returns the scope for the given context and reference.
	 *
	 * @param context The resource currently being processed.
	 * @param reference The specific reference type being resolved.
	 * @param filter A predicate used to filter out unwanted descriptions.
	 *
	 * @return IScope.NULLSCOPE - An Xtext constant representing an empty scope.
	 */
	@Override
	public IScope getScope(Resource context, EReference reference,
			Predicate<IEObjectDescription> filter) {
		return IScope.NULLSCOPE;
	}
}
