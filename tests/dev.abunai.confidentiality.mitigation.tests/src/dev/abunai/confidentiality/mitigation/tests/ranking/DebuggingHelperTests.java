package dev.abunai.confidentiality.mitigation.tests.ranking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class DebuggingHelperTests {

	protected final String pathToMeassurements = "meassurements.txt";

	@Test
	public void seeAverageRuntime() {
		Path filePath = Paths.get(pathToMeassurements);
		if (!Files.isRegularFile(filePath)) {
			System.out.println("run mitigation first !!!");
			
		}
		try {
			var contentLines = Files.readAllLines(filePath);
			int sum = 0;
			for (int i = contentLines.size() - 6; i < contentLines.size() && i >= 0; i++) {
				sum += Integer.parseInt(contentLines.get(i));
			}
			System.out.println((float)sum / (float)6);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
