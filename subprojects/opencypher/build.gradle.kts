/*
 * Copyright (c) 2019-2021 Code-Kontor GmbH and others (slizaa@codekontor.io)
 * Copyright (c) 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.java-test-fixtures")
	id("tools.refinery.gradle.mwe2")
	id("tools.refinery.gradle.sonarqube")
	id("tools.refinery.gradle.xtext-generated")
}

mavenArtifact {
	description = "OpenCypher language"
}

val generatedIdeSources: Configuration by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
}

dependencies {
	api(libs.ecore)
	api(libs.xtext.core)
	api(libs.xtext.xbase)
	testFixturesApi(libs.junit.api)
	testFixturesApi(libs.xtext.testing)
	mwe2(libs.xtext.generator)
	mwe2(libs.xtext.generator.antlr)
	implementation(files(layout.buildDirectory.dir("generated/sources/xtext/main")))
	runtimeOnly("org.slf4j:log4j-over-slf4j:2.0.13")
	runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
}

sourceSets {
	main {
		java {
			srcDir("src/main/java")
			srcDirs(layout.buildDirectory.dir("generated/sources/xtext/main"))
		}
	}
	testFixtures {
		java.srcDir("src/testFixtures/xtext-gen")
		resources.srcDir("src/testFixtures/xtext-gen")
	}
}

val generateXtextLanguage by tasks.registering(JavaExec::class) {
	mainClass.set("org.eclipse.emf.mwe2.launch.runtime.Mwe2Launcher")
	classpath(configurations.mwe2)
	inputs.file("src/main/java/io/codekontor/opencypher/xtext/GenerateOpenCypher.mwe2")
	inputs.file("src/main/java/io/codekontor/opencypher/xtext/OpenCypher.xtext")
	outputs.dir(layout.buildDirectory.dir("generated/sources/xtext/main"))
	outputs.dir("src/testFixtures/xtext-gen")
	outputs.dir(layout.buildDirectory.dir("generated/sources/xtext/ide"))
	outputs.dir(layout.buildDirectory.dir("generated/sources/xtext/web"))
	val rootDirFile = projectDir.parentFile
	args("src/main/java/io/codekontor/opencypher/xtext/GenerateOpenCypher.mwe2", "-p", "rootPath=${rootDirFile.absolutePath}")
}

tasks {
	jar {
		from(sourceSets.main.map { it.allSource }) {
			include("**/*.xtext")
		}
	}

	syncXtextGeneratedSources {
		// We generate Xtext runtime sources directly to {@code src/main/xtext-gen}, so there is no need to copy them
		// from an artifact. We expose the {@code generatedIdeSources} artifacts to
		// sibling IDE and web projects which can use this task to consume them and copy the appropriate sources to
		// their own {@code src/main/xtext-gen} directory.
		enabled = false
	}

	for (taskName in listOf("compileJava", "processResources", "compileTestFixturesJava",
			"processTestFixturesResources", "generateEclipseSourceFolders", "sourcesJar")) {
		named(taskName) {
			dependsOn(generateXtextLanguage)
		}
	}

	clean {
		delete(layout.buildDirectory.dir("generated/sources/xtext/main"))
		delete("src/testFixtures/xtext-gen")
	}

	register<JavaExec>("runRefineryTransformer") {
		group = "application"
		description = "Runs the standalone openCypher to Refinery model transformer."
		mainClass.set("io.codekontor.opencypher.xtext.main.RefineryTransformerMain")
		classpath = sourceSets.main.get().runtimeClasspath
		workingDir = rootProject.projectDir
	}
}

artifacts {
	add(generatedIdeSources.name, layout.buildDirectory.dir("generated/sources/xtext/ide")) {
		builtBy(generateXtextLanguage)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.exclusions", "src/textFixtures/xtext-gen/**")
}

eclipse.project.natures.plusAssign("org.eclipse.xtext.ui.shared.xtextNature")
