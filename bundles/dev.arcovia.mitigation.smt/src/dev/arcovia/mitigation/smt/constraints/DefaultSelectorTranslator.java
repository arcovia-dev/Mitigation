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
     * @param cls Class of the DSL selector
     * @param h Handler that should translate selectors of given class.
     */
    private <T extends AbstractSelector> void register(Class<T> cls, AbstractSelectorHandler<T> h) {
        handlers.put(cls, h);
    }

    @Override
    public BoolExpr toBool(AbstractSelector selector, DFDVertex vertex) {
        var handler = findHandler(selector.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No selector handler registered for " + selector.getClass()
                    .getName());
        }
        @SuppressWarnings("unchecked")
        AbstractSelectorHandler<AbstractSelector> h = (AbstractSelectorHandler<AbstractSelector>) handler;
        return h.encode(selector, vertex, smt);
    }

    /**
     * Finds correct handler for selector of given class
     * @param cls Selector class
     * @return Handler for selectors of this class
     */
    private AbstractSelectorHandler<?> findHandler(Class<?> cls) {
        var h = handlers.get(cls);
        if (h != null)
            return h;

        for (var e : handlers.entrySet()) {
            if (e.getKey()
                    .isAssignableFrom(cls))
                return e.getValue();
        }
        return null;
    }
}
