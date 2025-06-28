package dev.arcovia.mitigation.ranking;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class DataFlowDiagramUncertaintyUtils {

    public static void createNewModelWithRemovedUncertainties(DataFlowDiagramAndDictionary dfd, List<String> uncertaintyIdsToRemove,
            URI oldUncertaintyURI, URI newUcertaintyURI, String newModelName, Class<? extends Plugin> pluginActivator) {

        ResourceSet resSet = new ResourceSetImpl();
        Resource oldUncertaintyRes = resSet.getResource(oldUncertaintyURI, true);
        Resource newUncertaintyRes = resSet.createResource(newUcertaintyURI);

        var sourceCollection = oldUncertaintyRes.getContents()
                .get(0);
        newUncertaintyRes.getContents()
                .add(sourceCollection);

        Stack<EObject> objectsToRemove = new Stack<>();
        for (EObject eObject : sourceCollection.eContents()) {
            var id = EcoreUtil.getID(eObject);
            if (uncertaintyIdsToRemove.contains(id)) {
                objectsToRemove.push(eObject);
            }
        }

        while (!objectsToRemove.isEmpty()) {
            var object = objectsToRemove.pop();
            EcoreUtil.delete(object);
        }

        try {
            newUncertaintyRes.save(Collections.EMPTY_MAP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String outputPath = getOutputPathFromURI(newUcertaintyURI);

        dfd.save("", Paths.get(outputPath, newModelName)
                .toString());
    }

    private static String getOutputPathFromURI(URI mitigationUncertaintyURI) {
        var segments = mitigationUncertaintyURI.segmentsList();
        return Paths.get(segments.get(2))
                .toString();
    }
}
