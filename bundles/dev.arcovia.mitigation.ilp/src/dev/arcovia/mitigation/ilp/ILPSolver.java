package dev.arcovia.mitigation.ilp;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.arcovia.mitigation.sat.BiMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ILPSolver {
    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9_]");
    private int counter = 0;
    private BiMap<Integer, Mitigation> mitigationMap = new BiMap<>();
    
    public ILPSolver() {
        
    }
    
    public void writeLP(List<List<Mitigation>> mitigations,Set<Mitigation> allMitigations) throws IOException {
        
        for (Mitigation m : allMitigations) {
            mitigationMap.put(counter, m);
            counter++;
        }
        
        String out = "problem.lp" ;
        
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8))) {
            // Objective
            pw.println("\\ Auto-generated set-cover MILP");
            pw.println("Minimize");
            pw.print("  obj: ");
            boolean first = true;
            for (Mitigation m : allMitigations) {
              if (!first) pw.print(" + ");
              pw.print(formatCoeff(m.cost()) + " " + mitigationMap.getKey(m));
              first = false;
            }
            pw.println();

            // Constraints: each violation covered (sum >= 1)
            pw.println("Subject To");
            for (int i = 0; i < mitigations.size(); i++) {
              List<Mitigation> cover = mitigations.get(i);
              pw.print("  cover_" + i + ": ");
              
              for (int j = 0; j < cover.size(); j++) {
                if (j > 0) pw.print(" + ");
                pw.print(mitigationMap.getKey(cover.get(j)));
              }
              pw.println(" >= 1");
            }

            // Integrality
            pw.println("Binary");
            for (Mitigation m : allMitigations) {
              pw.println("  " + mitigationMap.getKey(m));
            }
            pw.println("End");
        }
    }
    // Helpers
    
    private static String toVarName(String id) {
      String s = id.trim().replace(' ', '_');
      s = NON_ALNUM.matcher(s).replaceAll("_");
      if (!s.isEmpty() && Character.isDigit(s.charAt(0))) s = "x_" + s; // LP vars cannot start with digit
      return s;
    }
    private static String formatCoeff(double c) {
      // avoid scientific notation; trim .0
      String s = String.format(Locale.ROOT, "%.12f", c);
      s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
      return s.isEmpty() ? "0" : s;
    }
        

}
