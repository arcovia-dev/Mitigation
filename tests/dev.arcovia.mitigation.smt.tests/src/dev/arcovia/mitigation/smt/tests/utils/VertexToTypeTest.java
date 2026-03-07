package dev.arcovia.mitigation.smt.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class VertexToTypeTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("vertexCases")
    void testVertexToType(Supplier<Node> vertexSupplier, DFDVertexType expectedType) {

        Node node = vertexSupplier.get();
        DFDVertex vertex = new DFDVertex(node, null, null);

        assertEquals(expectedType, ParsingUtils.vertexToType(vertex));
    }

    static Stream<Arguments> vertexCases() {
        return Stream.of(Arguments.of((Supplier<Node>) () -> dfdFactory.createProcess(), DFDVertexType.PROCESS),
                Arguments.of((Supplier<Node>) () -> dfdFactory.createStore(), DFDVertexType.STORE),
                Arguments.of((Supplier<Node>) () -> dfdFactory.createExternal(), DFDVertexType.EXTERNAL));
    }
}
