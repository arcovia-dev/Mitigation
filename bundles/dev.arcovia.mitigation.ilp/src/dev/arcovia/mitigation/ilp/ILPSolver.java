package dev.arcovia.mitigation.ilp;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;

import dev.arcovia.mitigation.sat.BiMap;

import java.util.ArrayList;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ILPSolver {
	private BiMap<MPVariable, Mitigation> mitigationMap = new BiMap<>();

	public List<Mitigation> solve(List<List<Mitigation>> mitigations, Set<Mitigation> allMitigations,
			List<List<Mitigation>> contradictions) throws Exception {
		try {
			loadOrToolsNative();
		} catch (IOException e) {
			throw new Exception("Unable to load required libariers used by google orTools");

		}
		MPSolver solver = MPSolver.createSolver("CBC_MIXED_INTEGER_PROGRAMMING");

		for (Mitigation mitigation : allMitigations) {
			MPVariable var = solver.makeIntVar(0, 1, mitigation.toString());
			mitigationMap.put(var, mitigation);
		}
		Integer counter = 0;
		for (var constraint : mitigations) {
			// Coverage constraint for the single violation: at least one mitigation active
			MPConstraint cover = solver.makeConstraint(1.0, Double.POSITIVE_INFINITY, counter.toString());
			counter++;
			for (var mitigation : constraint) {
				cover.setCoefficient(mitigationMap.getKey(mitigation), 1.0);
			}
		}

		for (var contradiction : contradictions) {
			// if a contradicting pair is selected it implies that the other is not selected
			MPConstraint conflict = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1.0, counter.toString());
			counter++;
			for (var mitigation : contradiction) {
				conflict.setCoefficient(mitigationMap.getKey(mitigation), 1.0);
			}
		}
		// required implementation
		for (Mitigation mitigation : allMitigations) {

			var mitigationValue = mitigationMap.getKey(mitigation);

			List<MPVariable> requiredAlternatives = new ArrayList<>();
			int clauseIndex = 0;

			// each clause is a list of alternative literals
			for (List<Mitigation> clause : mitigation.required()) {

				MPVariable parentElement = solver.makeIntVar(0, 1,
						"required_" + safeName(mitigation.mitigation().toString()) + "_" + clauseIndex);
				requiredAlternatives.add(parentElement);

				// if the parent element is chosen all Children need to be chosen as well
				for (Mitigation m : clause) {
					MPVariable childrenVariable = mitigationMap.getKey(m);
					MPConstraint childEnforcement = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0.0,
							"required_child_" + counter);
					counter++;
					childEnforcement.setCoefficient(parentElement, 1.0);
					childEnforcement.setCoefficient(childrenVariable, -1.0);
				}

				// if all children are satisfied, parent is satisfied as well:
				// clauseSatisfied >= sum(literals) - (k-1)
				int k = clause.size();
				MPConstraint childrenConstraint = solver.makeConstraint(-(k - 1), Double.POSITIVE_INFINITY,
						"required_parent_" + counter);
				counter++;
				childrenConstraint.setCoefficient(parentElement, 1.0);

				for (Mitigation m : clause) {
					MPVariable v = mitigationMap.getKey(m);
					childrenConstraint.setCoefficient(v, -1.0);
				}

				clauseIndex++;

			}
			// if the mitigationValue is chosen, one of the alternatives needs to be chosen
			if (!requiredAlternatives.isEmpty()) {
				MPConstraint required = solver.makeConstraint(0.0, Double.POSITIVE_INFINITY,
						"required_cover_" + counter);
				counter++;

				for (MPVariable y : requiredAlternatives) {
					required.setCoefficient(y, 1.0);
				}

				required.setCoefficient(mitigationValue, -1.0);
			}

		}

		MPObjective objective = solver.objective();

		for (var mitigation : allMitigations) {
			objective.setCoefficient(mitigationMap.getKey(mitigation), mitigation.cost());
		}

		objective.setMinimization();

		String lpModel = solver.exportModelAsLpFormat(false);
		try (FileWriter writer = new FileWriter("model.lp")) {
			writer.write(lpModel);
		} catch (IOException e) {
			e.printStackTrace();
		}

		MPSolver.ResultStatus status = solver.solve();

		if (status == MPSolver.ResultStatus.OPTIMAL || status == MPSolver.ResultStatus.FEASIBLE) {
			List<Mitigation> chosen = new ArrayList<>();
			for (MPVariable variable : solver.variables()) {
				if (variable.solutionValue() > 0.5) {
					// skip auxiliary variables introduced by required semantic
					if (variable.name().startsWith("required_") || variable.name().startsWith("y_")) {
						continue;
					}

					Optional<Mitigation> mitigation = allMitigations.stream()
							.filter(m -> variable.name().equals(m.toString())).findFirst();
					if (mitigation.isPresent()) {
						chosen.add(mitigation.get());
					}
				}
			}
			return chosen;
		} else {
			System.out.println("No feasible solution: " + status);
			return null;
		}
	}

	/***
	 * Returns a string that is valid for LP/MPS export by: - replacing all
	 * whitespace and illegal characters with _ - if only a number or an empty
	 * string is used it is set to variable x_(number)
	 * 
	 * @param s any arbitrary string
	 * @return s
	 * 
	 */
	private static String safeName(String s) {
		s = s.trim().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", "_");
		if (s.isEmpty() || Character.isDigit(s.charAt(0))) {
			s = "x_" + s;
		}
		return s;
	}

	private static volatile boolean nativeLoaded = false;

	private static synchronized void loadOrToolsNative() throws IOException {
		if (nativeLoaded) {
			return;
		}

		// Strategy 1: Standard Loader — works in Eclipse IDE
		try {
			Loader.loadNativeLibraries();
			nativeLoaded = true;
			return;
		} catch (Exception | Error ignored) {
		}

		// Strategy 2: OSGi — read the native JAR via bundle.getEntry()
		String platformJar = getPlatformJarPath();
		InputStream jarStream = null;

		try {
			Bundle bundle = FrameworkUtil.getBundle(ILPSolver.class);
			if (bundle != null) {
				URL jarUrl = bundle.getEntry(platformJar);
				if (jarUrl != null) {
					jarStream = jarUrl.openStream();
				}
			}
		} catch (Exception ignored) {
		}

		if (jarStream == null) {
			jarStream = ILPSolver.class.getClassLoader().getResourceAsStream(platformJar);
		}

		if (jarStream == null) {
			throw new IOException("Native JAR not found: " + platformJar);
		}

		Path tempDir = Files.createTempDirectory("ortools-native");
		tempDir.toFile().deleteOnExit();

		String mainLibName = System.mapLibraryName("jniortools"); // e.g. libjniortools.so
		List<Path> depLibs = new ArrayList<>();
		Path mainLib = null;

		try (ZipInputStream zip = new ZipInputStream(jarStream)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				String fileName = Path.of(entry.getName()).getFileName().toString();

				// Match .dll, .dylib, .so AND versioned sonames like libortools.so.9
				boolean isNative = fileName.endsWith(".dll") || fileName.endsWith(".dylib") || fileName.endsWith(".so")
						|| fileName.matches(".*\\.so\\.\\d+.*");

				if (isNative) {
					Path target = tempDir.resolve(fileName);
					Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
					target.toFile().deleteOnExit();
					if (fileName.equals(mainLibName)) {
						mainLib = target;
					} else {
						depLibs.add(target);
					}
				}
			}
		}

		if (mainLib == null) {
			throw new IOException(mainLibName + " not found in " + platformJar);
		}

		// Load all dependencies first with retry to handle unknown ordering
		int passes = depLibs.size() + 1;
		while (!depLibs.isEmpty() && passes-- > 0) {
			depLibs.removeIf(p -> {
				try {
					System.load(p.toAbsolutePath().toString());
					return true;
				} catch (UnsatisfiedLinkError e) {
					return false;
				}
			});
		}

		System.load(mainLib.toAbsolutePath().toString());
		nativeLoaded = true;
	}

	private static String getPlatformJarPath() {
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			return "lib/win64/ortools-win32-x86-64-9.14.6206.jar";
		}
		else if (os.contains("mac") || os.contains("darwin")) {
			return arch.contains("aarch64") ? "lib/macos/ortools-linux-aarch64-9.14.6206.jar"
					: "lib/macos/ortools-darwin-x86-64-9.14.6206.jar";
		}
		return arch.contains("aarch64") ? "lib/linux64/ortools-linux-aarch64-9.14.6206.jar"
				: "lib/linux64/ortools-linux-x86-64-9.14.6206.jar";
	}

}
