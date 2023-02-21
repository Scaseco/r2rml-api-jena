package org.aksw.rml.jena.impl;

import java.util.Arrays;

import org.aksw.r2rml.jena.arq.impl.JoinDeclaration;
import org.aksw.r2rml.jena.arq.impl.TriplesMapToSparqlMapping;
import org.aksw.rml.jena.plugin.ReferenceFormulationRegistry;
import org.aksw.rml.model.LogicalSource;
import org.aksw.rml.model.RmlTriplesMap;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.Template;

public class RmlQueryGenerator {
    /** Produces a query from all non-joining elements */
    public static Query createQuery(TriplesMapToSparqlMapping mapping, ReferenceFormulationRegistry registry) {
        if (registry == null) {
            registry = ReferenceFormulationRegistry.get();
        }

        LogicalSource childSource = mapping.getTriplesMap().as(RmlTriplesMap.class).getLogicalSource();
        String childRfIri = childSource.getReferenceFormulationIri();
        ReferenceFormulation childRf = registry.getOrThrow(childRfIri);
        Element childElt = childRf.source(childSource, mapping.getMappingCxt().getTriplesMapVar());
        // ElementBind childSubjectElt = join.getChildSubjectDefinition();

        ElementGroup elt = new ElementGroup();
        elt.addElement(childElt);

        VarExprList varToExpr = mapping.getVarToExpr();
        varToExpr.forEachVarExpr((v, e) ->  {
//            Expr ee = !safeVars
//                    ? e
//                    : ExprTransformer.transform(new NodeTransformExpr(n -> n.isVariable() ? VarUtils.safeVar(n.getName()) : n), e);
            elt.addElement(new ElementBind(v, e));
        });


        Query result = new Query();
        result.setQueryConstructType();
        result.setConstructTemplate(mapping.getTemplate());
        result.setQueryPattern(elt);

        return result;
    }

    public static Query createQuery(JoinDeclaration join, ReferenceFormulationRegistry registry) {
        if (registry == null) {
            registry = ReferenceFormulationRegistry.get();
        }

        LogicalSource childSource = join.getChildTriplesMap().as(RmlTriplesMap.class).getLogicalSource();
        LogicalSource parentSource = join.getParentTriplesMap().as(RmlTriplesMap.class).getLogicalSource();

        String childRfIri = childSource.getReferenceFormulationIri();
        ReferenceFormulation childRf = registry.getOrThrow(childRfIri);

        String parentRfIri = parentSource.getReferenceFormulationIri();
        ReferenceFormulation parentRf = registry.getOrThrow(parentRfIri);

        Element childElt = childRf.source(childSource, join.getChildVar());
        Element parentElt = parentRf.source(parentSource, join.getParentVar());

        ElementBind childSubjectElt = join.getChildSubjectDefinition();
        ElementBind parentSubjectElt = join.getParentSubjectDefinition();

        ElementGroup elt = new ElementGroup();
        elt.addElement(childElt);
        elt.addElement(parentElt);

        for (Expr expr : join.getConditionExprs()) {
            elt.addElementFilter(new ElementFilter(expr));
        }

        elt.addElement(childSubjectElt);
        elt.addElement(parentSubjectElt);

        Query result = new Query();
        result.setQueryConstructType();
        result.setConstructTemplate(new Template(new QuadAcc(Arrays.asList(join.getQuad()))));
        result.setQueryResultStar(true);
        result.setQueryPattern(elt);
        return result;
    }
}