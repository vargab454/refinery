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
package io.codekontor.opencypher.xtext.main;

import io.codekontor.opencypher.xtext.OpenCypherStandaloneSetup;
import io.codekontor.opencypher.xtext.openCypher.Cypher;
import io.codekontor.opencypher.xtext.transform.RefineryModelTransformer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import com.google.inject.Injector;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Main execution class for the standalone openCypher-to-Refinery compiler.
 * It coordinates initialization, loading, validation, and invocation of the transformer.
 */
public class RefineryTransformerMain {

	public static void main(String[] args) {
		// Initialize Xtext injector and fetch core validation/resource components
		Injector injector = new OpenCypherStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(ResourceSet.class);
		IResourceValidator validator = injector.getInstance(IResourceValidator.class);
		RefineryModelTransformer transformer = new RefineryModelTransformer();
		File inputDir = new File("models/input");
		File outputDir = new File("models/output");
		if (!inputDir.exists()) {
			inputDir.mkdirs();
			System.out.println("[INFO] Created missing input directory at: " + inputDir.getAbsolutePath());
		}
		if (!outputDir.exists()) {
			outputDir.mkdirs();
			System.out.println("[INFO] Created missing output directory at: " + outputDir.getAbsolutePath());
		}
		File[] inputFiles = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".cypher"));
		if (inputFiles == null || inputFiles.length == 0) {
			System.out.println("[WARN] No '.cypher' files found in: " + inputDir.getAbsolutePath());
			return;
		}
		System.out.println("[START] Batch transformation and validation initiated.");
		for (File inputFile : inputFiles) {
			String fileNameWithExtension = inputFile.getName();
			String baseName = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf('.'));
			File outputFile = new File(outputDir, baseName + ".problem");
			try {
				// Load the resource dynamically
				URI fileURI = URI.createFileURI(inputFile.getAbsolutePath());
				Resource resource = resourceSet.getResource(fileURI, true);

				// Run Xtext validation
				List<Issue> issues = validator.validate(resource, CheckMode.ALL, null);
				// Filter specifically for ERROR level diagnostics
				boolean hasErrors = issues.stream().anyMatch(issue -> org.eclipse.xtext.diagnostics.Severity.ERROR == issue.getSeverity());
				String finalOutputContent;
				if (hasErrors) {
					// If errors are found, build an error report instead of transforming
					System.out.println("Xtext Validation failed for: " + fileNameWithExtension + " -> Writing errors to output.");
					StringBuilder errorLog = new StringBuilder();
					errorLog.append("// Transformation aborted due to validation errors inside '").append(fileNameWithExtension).append("':\n\n");
					for (Issue issue : issues) {
						if (issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR) {
							errorLog.append("[ERROR] Line ").append(issue.getLineNumber()).append(": ").append(issue.getMessage()).append("\n");
						}
					}
					finalOutputContent = errorLog.toString();
				} else {
					// No errors found, safe to trigger the model transformer
					System.out.println("✔ Transformation successful for: " + fileNameWithExtension);
					Cypher model = (Cypher) resource.getContents().get(0);
					finalOutputContent = transformer.convertToRefinery(model);
				}
				// Write the resulting content to the output file
				try (FileWriter writer = new FileWriter(outputFile)) {
					writer.write(finalOutputContent);
				}
			} catch (Exception e) {
				System.err.println("[CRITICAL ERROR] Processing failed for '" + fileNameWithExtension + "': " + e.getMessage());
			}
		}
		System.out.println("[SUCCESS] Batch execution finished. Checked files are in: " + outputDir.getAbsolutePath());
	}

    private static Resource loadResource(String filePath, Injector injector) {
        ResourceSet resourceSet = injector.getInstance(ResourceSet.class);
        URI fileURI = URI.createFileURI(new File(filePath).getAbsolutePath());
        return resourceSet.getResource(fileURI, true);
    }

    /**
     * Executes all Xtext validators linked to the language grammar.
     * Maps both standard structural checks and custom @Check methods.
     *
     * @return true if at least one ERROR issue was discovered, false otherwise.
     */
    private static boolean validateModel(Resource resource, Injector injector) {
        // Fetch the validator registry from the active injector context
        IResourceValidator validator = injector.getInstance(IResourceValidator.class);
        // Execute validation on the entire resource using the full verification profile
        List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
        boolean hasErrors = false;
        for (Issue issue : issues) {
            switch (issue.getSeverity()) {
                case ERROR -> {
                    System.err.printf("[ERROR] Line %d: %s (Code: %s)%n", issue.getLineNumber(), issue.getMessage(), issue.getCode());
                    hasErrors = true;
                }
                case WARNING -> {
                    System.out.printf("[WARNING] Line %d: %s (Code: %s)%n", issue.getLineNumber(), issue.getMessage(), issue.getCode());
                }
                case INFO -> {
                    System.out.printf("[INFO] Line %d: %s%n", issue.getLineNumber(), issue.getMessage());
                }
            }
        }
        return hasErrors;
    }
}
