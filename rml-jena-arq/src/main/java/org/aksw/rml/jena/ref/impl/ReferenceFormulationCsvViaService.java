package org.aksw.rml.jena.ref.impl;

import java.util.Arrays;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;

public class ReferenceFormulationCsvViaService
    extends ReferenceFormulationJsonViaService {
    @Override
    public Expr reference(Var itemVar, String expr) {
        String jsonGet = "http://jsa.aksw.org/fn/json/get";
        return new E_Function(jsonGet, ExprList.create(Arrays.asList(new ExprVar(itemVar), NodeValue.makeString(expr))));
    }
}

//public class ReferenceFormulationCsvViaService
//    implements ReferenceFormulation
//{
//    @Override
//    public Element source(LogicalSource logicalSource, Var sourceVar) {
//        BasicPattern bgp = new BasicPattern();
//
//        // Replace the logical source with a constant in order to make
//        // equality checks easier
//        Node s = NodeFactory.createURI(SparqlX_Rml_Terms.RML_SOURCE_SERVICE_IRI);
//
//        // Only add the immediate triples
//        logicalSource.listProperties()
//            .mapWith(stmt -> stmt.asTriple())
//            .mapWith(t -> Triple.create(s, t.getPredicate(), t.getObject()))
//            .forEach(bgp::add);
//        // GraphUtil.findAll(logicalSource.getModel().getGraph()).forEach(bgp::add);
//        bgp.add(Triple.create(s, Fno.returns.asNode(), sourceVar));
//        ElementService result = new ElementService(SparqlX_Rml_Terms.RML_SOURCE_SERVICE_IRI, new ElementTriplesBlock(bgp));
//        return result;
//    }
//
//    @Override
//    public Expr reference(Var itemVar, String expr) {
//        String jsonPath = "http://jsa.aksw.org/fn/json/path";
//        return new E_Function(jsonPath, ExprList.create(Arrays.asList(new ExprVar(itemVar), NodeValue.makeString("$['" + expr.replaceAll("'", "\\'") + "']"))));
//    }
//}
