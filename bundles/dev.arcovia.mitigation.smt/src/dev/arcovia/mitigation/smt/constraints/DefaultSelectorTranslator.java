package dev.arcovia.mitigation.smt.constraints;

import java.util.HashMap;
import java.util.Map;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VariableNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexTypeSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;

/**
 * Encodes selector matching logic by choosing the correct handler.
 */
public final class DefaultSelectorTranslator implements SelectorTranslator {
    private final Map<Class<?>, AbstractSelectorHandler<?>> handlers = new HashMap<>();
    private final SMT smt;

    /**
     * Register implemented selectors
     * @param smt SMT object that requests translation
     */
    public DefaultSelectorTranslator(SMT smt) {
        this.smt = smt;

        register(DataCharacteristicsSelector.class, new DataCharacteristicsHandler());
        register(DataCharacteristicListSelector.class, new DataCharacteristicListHandler());
        register(VariableNameSelector.class, new DataNameHandler());
        register(VertexCharacteristicsListSelector.class, new VertexCharacteristicListHandler());
        register(VertexTypeSelector.class, new VertexTypeHandler());
        register(VertexNameSelector.class, new VertexNameHandler());
        register(VertexCharacteristicsSelector.class, new VertexCharacteristicsHandler());
    }

    /**
     * Registers a handler for a given selector class
     * @param <T> Type of the DSL selector
     * @param selectorClass Class of the DSL selector
     * @param selectorHandler Handler that should translate selectors of given class.
     */
    private <T extends AbstractSelector> void register(Class<T> selectorClass, AbstractSelectorHandler<T> selectorHandler) {
        handlers.put(selectorClass, selectorHandler);
    }

    @Override
    public BoolExpr toBool(AbstractSelector selector, DFDVertex vertex) {
        var handler = findHandler(selector.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No selector handler registered for " + selector.getClass()
                    .getName());
        }
        if (handler instanceof DataCharacteristicsHandler castHandler && selector instanceof DataCharacteristicsSelector castSelector) {
            return castHandler.encode(castSelector, vertex, smt);
        }

        if (handler instanceof DataCharacteristicListHandler castHandler && selector instanceof DataCharacteristicListSelector castSelector) {
            return castHandler.encode(castSelector, vertex, smt);
        }

        if (handler instanceof DataNameHandler castHandler && selector instanceof VariableNameSelector castSelector) {
            return castHandler.encode(castSelector, vertex, smt);
        }

        if (handler instanceof VertexCharacteristicListHandler castHandler && selector instanceof VertexCharacteristicsListSelector castSelector) {
            return castHandler.encode(castSelector, vertex, smt);
        }

        if (handler instanceof VertexTypeHandler castHandler && selector instanceof VertexTypeSelector castSelector) {
            return castHandler.encode(castSelector, vertex, smt);
        }

        if (handler instanceof VertexNameHandler castHandler && selector instanceof VertexNameSelector castSelector) {
            return castHandler.encode(castSelector, vertex, smt);
        }

        if (handler instanceof VertexCharacteristicsHandler castHandler && selector instanceof VertexCharacteristicsSelector castSelector) {
            return castHandler.encode(castSelector, vertex, smt);
        }

        throw new IllegalArgumentException("Handler " + handler.getClass()
                .getName() + " cannot encode selector "
                + selector.getClass()
                        .getName());
    }

    /**
     * Finds correct handler for selector of given class
     * @param selectorClass Selector class
     * @return Handler for selectors of this class
     */
    private <T extends AbstractSelector> AbstractSelectorHandler<?> findHandler(Class<T> selectorClass) {
        var handler = handlers.get(selectorClass);
        if (handler != null)
            return handler;

        for (var entry : handlers.entrySet()) {
            if (entry.getKey()
                    .isAssignableFrom(selectorClass))
                return entry.getValue();
        }
        return null;
    }
}
