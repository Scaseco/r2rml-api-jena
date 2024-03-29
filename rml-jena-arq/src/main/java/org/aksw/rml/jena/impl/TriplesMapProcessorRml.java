package org.aksw.rml.jena.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.aksw.commons.util.algebra.GenericDag;
import org.aksw.commons.util.function.FunctionUtils;
import org.aksw.fnml.model.FunctionMap;
import org.aksw.jenax.arq.util.expr.ExprUtils;
import org.aksw.jenax.arq.util.var.Vars;
import org.aksw.r2rml.jena.arq.impl.MappingCxt;
import org.aksw.r2rml.jena.arq.impl.TriplesMapProcessorR2rml;
import org.aksw.r2rml.jena.domain.api.TermMap;
import org.aksw.r2rml.jena.domain.api.TriplesMap;
import org.aksw.rml.jena.plugin.ReferenceFormulationRegistry;
import org.aksw.rml.model.LogicalSource;
import org.aksw.rml.model.RmlTermMap;
import org.aksw.rml.model.RmlTriplesMap;
import org.aksw.rmlx.model.RmlDefinitionBlock;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.syntax.Element;

/**
 * The RML processor adds a bit of functionality over the R2RML processor:
 * <ul>
 *   <li>The triples map contains the reference formulation which controls how turn rml:reference's into SPARQL Exprs</li>
 *   <li>TermMaps may be FunctionMaps
 * </ul>
 *
 */
public class TriplesMapProcessorRml
    extends TriplesMapProcessorR2rml
{
    protected Model fnmlModel;

    // Initialized on call()
    // protected ReferenceFormulation referenceFormulation;
    // protected Var itemVar; // Variable for an item of the source document
    protected ReferenceFormulationRegistry registry;
    // protected ReferenceFormulation referenceFormulation;


    public TriplesMapProcessorRml(TriplesMap triplesMap,  Model fnmlModel) {
        this(triplesMap, null, fnmlModel, null);
    }

    public TriplesMapProcessorRml(TriplesMap triplesMap, String baseIri, Model fnmlModel, ReferenceFormulationRegistry registry) {
        super(triplesMap, baseIri);
        this.fnmlModel = fnmlModel;

        if (registry == null) {
            registry = ReferenceFormulationRegistry.get();
        }
        this.registry = registry;
    }

    /**
     * Configures the context's referenceResolver and sourceIdentityResolver.
     */
    @Override
    public void initResolvers(MappingCxt cxt) {
        TriplesMap ctm = cxt.getTriplesMap();
        LogicalSource logicalSource = ctm.as(RmlTriplesMap.class).getLogicalSource();
        ReferenceFormulation referenceFormulation = null;
        if (logicalSource != null) {
            String rfi = logicalSource.getReferenceFormulationIri();
            referenceFormulation = registry.getOrThrow(rfi);
        }

        ReferenceFormulation rf = referenceFormulation;
        Var triplesMapVar = cxt.getTriplesMapVar();

        // The "raw" reference resolver does not deal with aliases
        Function<String, Expr> rawRefResolver = refStr -> {
            Expr r = rf.reference(triplesMapVar, refStr);
            return r;
        };

        // Remap of specified aliases to allocated variables (for better or worse we relabel variables)
        Map<Expr, Expr> remap = new HashMap<>();

        // Set up the reference resolver where aliases take precedence
        cxt.setReferenceResolver(refStr -> {
            Expr tmp = new ExprVar(refStr);
            Expr r = remap.get(tmp);
            if (r == null) {
                r = rawRefResolver.apply(refStr);
            }
            return r;
        });

        cxt.setSourceIdentityResolver(tm -> {
            RmlTriplesMap rtm = tm.as(RmlTriplesMap.class);
            LogicalSource ls = rtm.getLogicalSource();

            // Create a copy of the logical source that has aliases/binds cleared
            // LogicalSource copy = RmlDefinitionBlockUtils.closureWithoutDefinitions(ls).as(LogicalSource.class);
            Element r = rf.source(ls, Vars.x);
            return r;
        });

        // Expand aliases and definitions
        if (logicalSource != null) {
            RmlDefinitionBlock block = logicalSource.as(RmlDefinitionBlock.class);
            GenericDag<Expr, Var> dag = cxt.getExprDag();
            VarExprList vel = RmlDefinitionBlockUtils.processExprs(block, rawRefResolver);
            for (Entry<Var, Expr> e : vel.getExprs().entrySet()) {
                Var v = e.getKey();
                Expr rawExpr = e.getValue();
                Expr expr = ExprUtils.replace(rawExpr, FunctionUtils.nullToIdentity(remap::get));
                Expr newExpr = dag.addRoot(expr);
                if (newExpr.isVariable()) {
                    remap.put(new ExprVar(v), newExpr);
                }
            }
        }
    }

    @Override
    protected Expr resolveColumnLikeTermMap(MappingCxt cxt, TermMap tm, Resource fallbackTermType) {
        Expr result;

        // TODO We need access to (1) the item var and (2) the reference formulation
        // We either need a context object or some form of worker

        // Check for whether we dealing with an RML term reference
        RmlTermMap rmlTm = tm.as(RmlTermMap.class);
        String ref = rmlTm.getReference();
        if (ref != null) {
            result = referenceToExpr(cxt, ref);
        } else {
            // Check for function call
            FunctionMap fm = tm.as(FunctionMap.class);
            TriplesMap fntm = fm.getFunctionValue();
            if (fntm != null) {
                result = RmlLib.buildFunctionCall(fnmlModel, fntm);
                // FIXME The resulting expression needs to be post-processed by
                // the R2RML layer, because e.g. rr:termType rr:IRI may need to be applied
            } else {
                result = super.termMapToExpr(cxt, tm, fallbackTermType);
            }
        }
        return result;
    }

//    @Override
//    protected Object getSourceIdentity(TriplesMap tm) {
//        RmlTriplesMap rtm = tm.as(RmlTriplesMap.class);
//        LogicalSource logicalSource = rtm.getLogicalSource();
//        Element result = referenceFormulation.source(logicalSource, Vars.x);
//        return result;
//    }

//    @Override
//    protected Expr referenceToExpr(MappingCxt cxt, String colName) {
//        Expr result = referenceFormulation.reference(cxt.getTriplesMapVar(), colName);
//        return result;
//    }

//  @Override
//  public TriplesMapToSparqlMapping call() {
//      LogicalSource logicalSource = triplesMap.as(RmlTriplesMap.class).getLogicalSource();
//      if (logicalSource != null) {
//          String rfi = logicalSource.getReferenceFormulationIri();
//          referenceFormulation = registry.getOrThrow(rfi);
//      }
//
//      TriplesMapToSparqlMapping base = super.call();
//      return base;
//  }


}
