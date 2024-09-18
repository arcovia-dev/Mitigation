package dev.abunai.confidentiality.mitigation.tests.ranking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class DebuggingHelperTests {

	protected final String pathToMeassurements = "meassurements.txt";

	@Test
	public float seeAverageRuntime() {
		Path filePath = Paths.get(pathToMeassurements);
		if (!Files.isRegularFile(filePath)) {
			System.out.println("run mitigation first !!!");
			
		}
		try {
			var contentLines = Files.readAllLines(filePath);
			int sum = 0;
			for (int i = contentLines.size() - 20; i < contentLines.size() && i >= 0; i++) {
				sum += Integer.parseInt(contentLines.get(i));
			}
			return (float)sum / (float)20;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0.0f;
	}
}
