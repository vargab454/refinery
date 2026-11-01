package tools.refinery.opencypher.cli;

import tools.refinery.generator.standalone.StandaloneRefinery;
import io.codekontor.opencypher.xtext.OpenCypherStandaloneSetup;
import io.codekontor.opencypher.xtext.openCypher.Cypher;
import io.codekontor.opencypher.xtext.transform.RefineryModelTransformer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import com.google.inject.Injector;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MeasureTransformation {

	private static final String INPUT_DIR = "models/input/";
	private static final String BASE_OUTPUT_DIR = "models/output/";
	private static final String LOG_FILE_PATH = BASE_OUTPUT_DIR + "measurement_results.txt";

	private static final String[] PROBLEM_NAMES = {
			"healthcare",
			"logistics"
	};

	private static final String[] VARIANTS = { "OWA", "CWA" };

	private enum DatasetSize {
		SMALL(10, 15),
		MEDIUM(30, 45),
		LARGE(80, 120);

		final int nodes;
		final int relations;

		DatasetSize(int nodes, int relations) {
			this.nodes = nodes;
			this.relations = relations;
		}
	}

	private static final int WARMUP_ITERATIONS = 1;
	private static final int[] ITERATION_COUNTS = { 10, 20, 30 };
	private static final long TIMEOUT_SECONDS = 300;

	private static final long BASE_SEED = 42L;

	private record TransformRecord(String problemName, double owaTimeMs, double cwaTimeMs) {}
	private record SummaryRecord(String fileName, String sizeName, int targetIterations, double avgRunTimeMs, int timeouts) {}

	static void main(String[] args) {
		ExecutorService executor = Executors.newCachedThreadPool(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("Refinery-Solver-Thread");
			return t;
		});

		Injector injector = new OpenCypherStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(ResourceSet.class);
		RefineryModelTransformer transformer = new RefineryModelTransformer();

		List<TransformRecord> transformRecords = new ArrayList<>();
		List<SummaryRecord> summaryRecords = new ArrayList<>();

		try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH, false))) {

			logAndPrint(writer, "==================================================");
			logAndPrint(writer, "MEASUREMENT STARTED: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			logAndPrint(writer, "==================================================\n");

			long currentGlobalSeed = BASE_SEED;

			for (String problemName : PROBLEM_NAMES) {
				logAndPrint(writer, ">>> Processing problem set: " + problemName);

				Path cypherPath = Paths.get(INPUT_DIR, problemName + ".cypher");
				if (!Files.exists(cypherPath)) {
					logAndPrint(writer, "[ERROR] Input Cypher file not found: " + cypherPath.toAbsolutePath());
					continue;
				}

				String owaContent = "";
				String cwaContent = "";
				double owaTimeMs = 0.0;
				double cwaTimeMs = 0.0;

				try {
					URI fileURI = URI.createFileURI(cypherPath.toFile().getAbsolutePath());
					Resource resource = resourceSet.getResource(fileURI, true);
					Cypher model = (Cypher) resource.getContents().getFirst();

					long startOwa = System.nanoTime();
					owaContent = transformer.convertToRefinery(model, false);
					owaTimeMs = (System.nanoTime() - startOwa) / 1_000_000.0;

					long startCwa = System.nanoTime();
					cwaContent = transformer.convertToRefinery(model, true);
					cwaTimeMs = (System.nanoTime() - startCwa) / 1_000_000.0;

					transformRecords.add(new TransformRecord(problemName, owaTimeMs, cwaTimeMs));
					logAndPrint(writer, String.format("  [TRANSFORM] %s.cypher -> OWA: %.2f ms | CWA: %.2f ms", problemName, owaTimeMs, cwaTimeMs));

					Files.createDirectories(Paths.get(BASE_OUTPUT_DIR, "OWA"));
					Files.createDirectories(Paths.get(BASE_OUTPUT_DIR, "CWA"));
					Files.writeString(Paths.get(BASE_OUTPUT_DIR, "OWA", problemName + ".problem"), owaContent);
					Files.writeString(Paths.get(BASE_OUTPUT_DIR, "CWA", problemName + ".problem"), cwaContent);

				} catch (Exception e) {
					logAndPrint(writer, "[CRITICAL ERROR] Failed to transform " + problemName + ".cypher: " + e.getMessage());
					continue;
				}

				for (String variant : VARIANTS) {
					String displayFileName = variant + "/" + problemName + ".problem";
					String baseContent = variant.equals("OWA") ? owaContent : cwaContent;

					for (DatasetSize size : DatasetSize.values()) {
						for (int measureIterations : ITERATION_COUNTS) {

							String contentWithScope = baseContent + "\nscope Node=" + size.nodes + ", RELATIONSHIP=" + size.relations + ".";

							logAndPrint(writer, String.format("\n--- RUNNING: File: %s | Size: %s (Nodes: %d, Rels: %d) | Target Iterations: %d ---",
									displayFileName, size.name(), size.nodes, size.relations, measureIterations));

							logAndPrint(writer, "  [INFO] Starting " + WARMUP_ITERATIONS + " warmup iterations...");
							for (int i = 1; i <= WARMUP_ITERATIONS; i++) {
								currentGlobalSeed++;
								runWithTimeout(executor, contentWithScope, currentGlobalSeed, writer, "Warmup " + i + " (Seed: " + currentGlobalSeed + ")");
							}

							logAndPrint(writer, "  [INFO] Starting " + measureIterations + " measurement iterations...");
							long sumRunTimeNano = 0;
							int successfulIterations = 0;

							for (int i = 1; i <= measureIterations; i++) {
								currentGlobalSeed++;
								String label = String.format("Iteration %d (Seed: %d)", i, currentGlobalSeed);
								long runTime = runWithTimeout(executor, contentWithScope, currentGlobalSeed, writer, label);

								if (runTime >= 0) {
									sumRunTimeNano += runTime;
									successfulIterations++;
								}
							}

							int timeouts = measureIterations - successfulIterations;
							double avgRunTimeMs = 0.0;

							if (successfulIterations > 0) {
								double sumRunTimeMs = sumRunTimeNano / 1_000_000.0;
								avgRunTimeMs = sumRunTimeMs / successfulIterations;
								logAndPrint(writer, String.format("  [SUMMARY] Success: %d/%d | Total time: %.2f ms | Average time: %.2f ms",
										successfulIterations, measureIterations, sumRunTimeMs, avgRunTimeMs));
							} else {
								logAndPrint(writer, "  [SUMMARY] All measurement iterations failed or timed out.");
							}

							summaryRecords.add(new SummaryRecord(displayFileName, size.name(), measureIterations, avgRunTimeMs, timeouts));
						}
					}
				}
				logAndPrint(writer, "--------------------------------------------------\n");
			}

			logAndPrint(writer, "=========================================================================================================");
			logAndPrint(writer, "                             1. TRANSFORMATION TIME SUMMARY (Cypher -> AST)                              ");
			logAndPrint(writer, "=========================================================================================================");
			logAndPrint(writer, String.format("%-40s | %-20s | %-20s", "Problem Name", "OWA Transform (ms)", "CWA Transform (ms)"));
			logAndPrint(writer, "-----------------------------------------|----------------------|----------------------");
			for (TransformRecord tr : transformRecords) {
				logAndPrint(writer, String.format("%-40s | %-20.2f | %-20.2f", tr.problemName, tr.owaTimeMs, tr.cwaTimeMs));
			}
			logAndPrint(writer, "\n=========================================================================================================");

			logAndPrint(writer, "                              2. SOLVER GENERATION SUMMARY (AST -> Graph)                                ");
			logAndPrint(writer, "=========================================================================================================");
			logAndPrint(writer, String.format("%-40s | %-10s | %-18s | %-15s | %-10s", "File Variant", "Dataset", "Target Iterations", "Avg Time (ms)", "Timeouts"));
			logAndPrint(writer, "-----------------------------------------|------------|--------------------|-----------------|-----------");

			for (SummaryRecord record : summaryRecords) {
				String avgTimeStr = (record.timeouts == record.targetIterations) ? "N/A" : String.format("%.2f", record.avgRunTimeMs);
				logAndPrint(writer, String.format("%-40s | %-10s | %-18d | %-15s | %-10d",
						record.fileName, record.sizeName, record.targetIterations, avgTimeStr, record.timeouts));
			}
			logAndPrint(writer, "=========================================================================================================\n");

			logAndPrint(writer, "MEASUREMENT FINISHED.");

		} catch (IOException e) {
			System.err.println("[CRITICAL ERROR] Failed to write log file: " + e.getMessage());
		} finally {
			executor.shutdownNow();
		}
	}

	private static long runWithTimeout(ExecutorService executor, String refineryProblem, long seed, PrintWriter writer, String iterationLabel) {
		long startTime = System.nanoTime();

		Future<Void> future = executor.submit(() -> {
			runRefinery(refineryProblem, seed);
			return null;
		});

		try {
			future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			long stopTime = System.nanoTime();
			long elapsedNano = stopTime - startTime;

			logAndPrint(writer, String.format("    -> %s: SUCCESS (%.2f ms)", iterationLabel, elapsedNano / 1_000_000.0));
			return elapsedNano;

		} catch (TimeoutException e) {
			future.cancel(true);
			logAndPrint(writer, String.format("    -> %s: TIMEOUT (Exceeded %d seconds)", iterationLabel, TIMEOUT_SECONDS));
			return -1;
		} catch (InterruptedException e) {
			future.cancel(true);
			logAndPrint(writer, String.format("    -> %s: INTERRUPTED", iterationLabel));
			Thread.currentThread().interrupt();
			return -1;
		} catch (ExecutionException e) {
			logAndPrint(writer, String.format("    -> %s: ERROR (%s)", iterationLabel, e.getCause().getMessage()));
			return -1;
		}
	}

	private static void runRefinery(String refineryProblem, long seed) throws Exception {
		var problem = StandaloneRefinery.getProblemLoader().loadString(refineryProblem);
		try (var generator = StandaloneRefinery.getGeneratorFactory().createGenerator(problem)) {

			try {
				Class<?> currentClass = generator.getClass();
				while (currentClass != null) {
					for (Field field : currentClass.getDeclaredFields()) {
						if ((field.getName().toLowerCase().contains("seed")) && (field.getType() == long.class || field.getType() == Long.class)) {
							field.setAccessible(true);
							field.set(generator, seed);
						}
					}
					currentClass = currentClass.getSuperclass();
				}
			} catch (Exception _) {}

			generator.generate();
		}
	}

	private static void logAndPrint(PrintWriter writer, String message) {
		System.out.println(message);
		writer.println(message);
		writer.flush();
	}
}
