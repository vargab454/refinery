package tools.refinery.opencypher.cli;

import tools.refinery.generator.standalone.StandaloneRefinery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MeasureTransformation {

	private static final String PROJECT_PATH = "C:\\Users\\varga\\Egyetem\\Szakdoga\\refinery\\models\\output\\";
	private static final String PROBLEM_TO_USE = "multiple_implication";
	private static final String REFINERY_PROBLEM_PATH = PROJECT_PATH + PROBLEM_TO_USE + ".problem";
	private static final int NODE_NUMBER = 10;
	private static final int RELATION_NUMBER = 5;
	private static final int WARMUP_ITERATIONS = 1;
	private static final int MEASURE_ITERATIONS = 10;

	static void main(String[] args) throws IOException {

		long sumRunTime = 0;

		String content = Files.readString(Path.of(REFINERY_PROBLEM_PATH));

		content += "\nscope Node=" + NODE_NUMBER + ", RELATIONSHIP=" + RELATION_NUMBER + ".";

		System.out.print(content);

		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			runRefinery(content);
		}

		for (int i = 0; i < MEASURE_ITERATIONS; i++) {
			long startTime = System.nanoTime();
			runRefinery(content);
			long stopTime = System.nanoTime();
			sumRunTime += stopTime - startTime;
		}

		long avgRunTime = sumRunTime / MEASURE_ITERATIONS;
		System.out.printf("Building example for problem %s with %d nodes and %d relations after %d warmup iterations " +
						"on average across %d iterations took\n%f",
				REFINERY_PROBLEM_PATH,
				NODE_NUMBER,
				RELATION_NUMBER,
				WARMUP_ITERATIONS,
				MEASURE_ITERATIONS,
				(double) avgRunTime);
	}

	private static void runRefinery(String refineryProblem) throws IOException {
		var problem = StandaloneRefinery.getProblemLoader().loadString(refineryProblem);
		try (var generator = StandaloneRefinery.getGeneratorFactory().createGenerator(problem)) {
			generator.generate();
		}
	}
}
