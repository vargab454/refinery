/*
 * Copyright (c) 2019-2021 Code-Kontor GmbH and others (slizaa@codekontor.io)
 * Copyright (c) 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.xtext-generated")
}

mavenArtifact {
	name = "OpenCypher IDE"
	description = "IDE support for the OpenCypher language"
}

dependencies {
	api(project(":refinery-opencypher"))
	api(libs.xtext.ide)
	api(libs.xtext.xbase.ide)
	xtextGenerated(project(":refinery-opencypher", "generatedIdeSources"))
}
