package org.aksw.rml.v2.jena.domain.api;

import org.aksw.jenax.annotation.reprogen.ResourceView;
import org.aksw.rml2.vocab.jena.RML2;
import org.aksw.rmltk.model.backbone.r2rml.ILogicalTableR2rml;

/**
 * Interface for RDF-based logical tables.
 *
 * As this denotes the same information as in the more basic
 * {@link org.aksw.PlainLogicalTable.domain.api.LogicalTable}, deriving from it should be
 * safe.
 *
 *
 *
 * @author raven Apr 1, 2018
 *
 */
@ResourceView
public interface LogicalTable
    extends ILogicalTableR2rml, MappingComponentRml2
{
    /**
     * R2RML specifies that the minimum condition for an entity to qualify as an "base table or view" is
     * "Having an rr:tableName property"
     *
     * @return
     */
    @Override
    default boolean qualifiesAsBaseTableOrView() {
        return hasProperty(RML2.tableName);
    }

    /**
     * Obtain a RefObjectMap view of this resource.
     * Calling this method does NOT require {@link #qualifiesAsRefObjectMap()} to yield true.
     *
     * @return
     */
    @Override
    default BaseTableOrView asBaseTableOrView() {
        return as(BaseTableOrView.class);
    }

    /**
     * R2RML specifies that the minimum condition for an entity to qualify as an "R2RML view" is
     * "Having an rr:sqlQuery property"
     *
     * @return
     */
    @Override
    default boolean qualifiesAsR2rmlView() {
        return hasProperty(RML2.sqlQuery);
    }

    /**
     * Obtain an ObjectMap view of this resource.
     * Calling this method does NOT require {@link #qualifiesAsObjectMap()} to yield true.
     *
     * @return
     */
    @Override
    default R2rmlView asR2rmlView() {
        return as(R2rmlView.class);
    }
}