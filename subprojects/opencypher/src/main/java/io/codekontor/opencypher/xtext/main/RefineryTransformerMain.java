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
import java.util.List;

/**
 * Main execution class for the standalone openCypher-to-Refinery compiler.
 * It coordinates initialization, loading, validation, and invocation of the transformer.
 */
public class RefineryTransformerMain {
    public static void main(String[] args) {
        String inputFilePath = "models/example.cypher";
        try {
            // Setup the Xtext context and retrieve the Guice Injector
            Injector injector = new OpenCypherStandaloneSetup().createInjectorAndDoEMFRegistration();
            // Load the AST model
            System.out.println("Parsing input file: " + inputFilePath);
            Resource resource = loadResource(inputFilePath, injector);
            Cypher model = (Cypher) resource.getContents().get(0);
            // Trigger full validation (including custom errors and warnings)
            System.out.println("Running semantic validation...");
            boolean hasErrors = validateModel(resource, injector);
            if (hasErrors) {
                System.err.println("Transformation aborted due to validation errors.");
                System.exit(1); // Stop execution with a failure code
            }
            // Invoke the transformer component if the model is clean
            System.out.println("Validation passed. Translating model...");
            RefineryModelTransformer transformer = injector.getInstance(RefineryModelTransformer.class);
            String refineryCode = transformer.convertToRefinery(model);
            System.out.println("\n--- Generated Refinery Code ---");
            System.out.println(refineryCode);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
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
