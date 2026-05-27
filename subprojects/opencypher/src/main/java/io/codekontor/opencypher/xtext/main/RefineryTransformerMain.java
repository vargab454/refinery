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
import java.util.ArrayList;
import java.util.List;

/**
 * Main execution class for the standalone openCypher-to-Refinery compiler.
 * It coordinates initialization, loading, validation, and invocation of the transformer.
 */
public class RefineryTransformerMain {

	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_RED = "\u001B[31m";
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_YELLOW = "\u001B[33m";

	private enum TransformationResult {
		SUCCESS,
		WARNING,
		ERROR
	}

	private static class BuildReport {
		final String fileName;
		final TransformationResult result;
		int errorCount = 0;
		int warningCount = 0;

		public BuildReport(String fileName, TransformationResult result) {
			this.fileName = fileName;
			this.result = result;
		}
	}

	public static void main(String[] args) {
		Injector injector = new OpenCypherStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(ResourceSet.class);
		IResourceValidator validator = injector.getInstance(IResourceValidator.class);
		RefineryModelTransformer transformer = new RefineryModelTransformer();
		File inputDir = new File("models/input");
		File outputDir = new File("models/output");
		if (!inputDir.exists()) inputDir.mkdirs();
		if (!outputDir.exists()) outputDir.mkdirs();
		File[] inputFiles = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".cypher"));
		if (inputFiles == null || inputFiles.length == 0) {
			System.out.println(ANSI_YELLOW + "[WARN] No '.cypher' files found in: " + inputDir.getAbsolutePath() + ANSI_RESET);
			return;
		}
		System.out.println("\n[START] Batch transformation initiated...");
		List<BuildReport> reports = new ArrayList<>();
		for (File inputFile : inputFiles) {
			String fileName = inputFile.getName();
			String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
			File outputFile = new File(outputDir, baseName + ".problem");
			try {
				URI fileURI = URI.createFileURI(inputFile.getAbsolutePath());
				Resource resource = resourceSet.getResource(fileURI, true);
				List<Issue> issues = validator.validate(resource, CheckMode.ALL, null);
				long errorCount = issues.stream().filter(i -> i.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR).count();
				long warningCount = issues.stream().filter(i -> i.getSeverity() == org.eclipse.xtext.diagnostics.Severity.WARNING).count();
				BuildReport report;
				String finalOutputContent;
				if (errorCount > 0) {
					report = new BuildReport(fileName, TransformationResult.ERROR);
					report.errorCount = (int) errorCount;
					report.warningCount = (int) warningCount;
					StringBuilder errorLog = new StringBuilder();
					errorLog.append("// Transformation aborted due to validation errors inside '").append(fileName).append("':\n\n");
					for (Issue issue : issues) {
						if (issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR) {
							errorLog.append("[ERROR] Line ").append(issue.getLineNumber()).append(": ").append(issue.getMessage()).append("\n");
						}
					}
					finalOutputContent = errorLog.toString();
				} else if (warningCount > 0) {
					report = new BuildReport(fileName, TransformationResult.WARNING);
					report.warningCount = (int) warningCount;
					Cypher model = (Cypher) resource.getContents().get(0);
					finalOutputContent = transformer.convertToRefinery(model);
				} else {
					report = new BuildReport(fileName, TransformationResult.SUCCESS);
					Cypher model = (Cypher) resource.getContents().get(0);
					finalOutputContent = transformer.convertToRefinery(model);
				}
				reports.add(report);
				try (FileWriter writer = new FileWriter(outputFile)) {
					writer.write(finalOutputContent);
				}
			} catch (Exception e) {
				BuildReport criticalReport = new BuildReport(fileName, TransformationResult.ERROR);
				criticalReport.errorCount = 1;
				reports.add(criticalReport);
				System.err.println("[CRITICAL ERROR] " + fileName + ": " + e.getMessage());
			}
		}
		printSummary(reports, outputDir.getAbsolutePath());
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

	private static void printSummary(List<BuildReport> reports, String outputPath) {
		System.out.println("\n==================================================");
		System.out.println("               TRANSFORMATION SUMMARY             ");
		System.out.println("==================================================");
		int totalSuccess = 0;
		int totalWarnings = 0;
		int totalErrors = 0;
		for (BuildReport r : reports) {
			if (r.result == TransformationResult.SUCCESS) {
				System.out.println(ANSI_GREEN + "[SUCCESS] " + r.fileName + ANSI_RESET);
				totalSuccess++;
			}
		}
		for (BuildReport r : reports) {
			if (r.result == TransformationResult.WARNING) {
				System.out.println(ANSI_YELLOW + "[WARNING] " + r.fileName + " (" + r.warningCount + " warning shadowed)" + ANSI_RESET);
				totalWarnings++;
			}
		}
		for (BuildReport r : reports) {
			if (r.result == TransformationResult.ERROR) {
				System.out.println(ANSI_RED + "[ERROR]   " + r.fileName + " (" + r.errorCount + " critical error found)" + ANSI_RESET);
				totalErrors++;
			}
		}
		System.out.println("==================================================");
		System.out.print("Final Metrics: ");
		System.out.print(ANSI_GREEN + totalSuccess + " Success" + ANSI_RESET + " | ");
		System.out.print(ANSI_YELLOW + totalWarnings + " Warning" + ANSI_RESET + " | ");
		System.out.println(ANSI_RED + totalErrors + " Failed" + ANSI_RESET);
		System.out.println("Output artifacts directory: " + outputPath);
		System.out.println("==================================================\n");
	}
}
