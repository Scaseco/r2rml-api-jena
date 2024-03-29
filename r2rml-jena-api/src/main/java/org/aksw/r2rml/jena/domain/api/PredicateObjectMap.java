package org.aksw.r2rml.jena.domain.api;

import java.util.Set;

import org.aksw.commons.collections.IterableUtils;
import org.aksw.jenax.annotation.reprogen.Iri;
import org.aksw.jenax.annotation.reprogen.IriType;
import org.aksw.jenax.annotation.reprogen.ResourceView;
import org.aksw.r2rml.common.vocab.R2rmlTerms;
import com.google.common.base.Preconditions;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

@ResourceView
public interface PredicateObjectMap
    extends MappingComponent, HasGraphMap
{
    /**
     * @return A mutable set view of the predicat maps. Never null.
     */
    @Iri(R2rmlTerms.predicateMap)
    Set<PredicateMap> getPredicateMaps();

    default PredicateMap getPredicateMap() {
        return IterableUtils.expectZeroOrOneItems(getPredicateMaps());
    }

    /**
     * @return A mutable set view of the object map types. Never null.
     */
    @Iri(R2rmlTerms.objectMap)
    Set<ObjectMapType> getObjectMaps();


    default ObjectMapType getObjectMap() {
        Set<ObjectMapType> oms = getObjectMaps();
        Preconditions.checkState(oms.size() <= 1);
        ObjectMapType result = oms.isEmpty() ? null : oms.iterator().next();
        return result;
    }

    /** Shorthands for constant objects */
    @Iri(R2rmlTerms.object)
    Set<RDFNode> getObjects();

    /** Shorthands for constant predicates */
    @Iri(R2rmlTerms.predicate)
    Set<Resource> getPredicates();

    /** Shorthands for constant objects as strings*/
    @Iri(R2rmlTerms.object)
    @IriType
    Set<String> getObjectIris();

    /** Shorthands for constant predicates as strings */
    @Iri(R2rmlTerms.predicate)
    @IriType
    Set<String> getPredicateIris();

    default String getPredicateIri() {
        return IterableUtils.expectZeroOrOneItems(getPredicateIris());
    }

    /** Shorthands for constant graphs as strings */
    @Iri(R2rmlTerms.graph)
    @IriType
    Set<Resource> getGraphIris();

    /**
     * Allocate a fresh blank node, add it to the set of predicate maps and return a view of it as a PredicateMap.
     *
     * @return
     */
    default PredicateMap addNewPredicateMap() {
        PredicateMap result = getModel().createResource().as(PredicateMap.class);
        getPredicateMaps().add(result);
        return result;
    }


    /**
     * Allocate a fresh blank node, add it to the set of object maps and return a view of it as a ObjectMap.
     *
     * @return
     */
    default ObjectMap addNewObjectMap() {
        ObjectMap result = getModel().createResource().as(ObjectMap.class);
        getObjectMaps().add(result);
        return result;
    }

    /**
     * Allocate a fresh blank node, add it to the set of object maps and return a view of it as a RefObjectMap.
     *
     * @return
     */
    default RefObjectMap addNewRefObjectMap() {
        RefObjectMap result = getModel().createResource().as(RefObjectMap.class);
        getObjectMaps().add(result);
        return result;
    }

    // Live filtered collection views would be easily possible if we added a dependency on jena-sparql-api-collections

//	default Set<ObjectMap> getTermObjectMaps() {
//		return Sets.filter(viewSetAs(getObjectMaps(), ObjectMap.class), ObjectMapType::qualifiesAsTermMap);
//	}
//
//	default Set<RefObjectMap> getRefObjectMaps() {
//		return Sets.filter(viewSetAs(getObjectMaps(), RefObjectMap.class), ObjectMapType::qualifiesAsTermMap);
//	}


    default PredicateObjectMap addPredicate(String iri) {
        return addPredicate(NodeFactory.createURI(iri));
    }

    default PredicateObjectMap addPredicate(Node node) {
        return addPredicate(getModel().wrapAsResource(node));
    }

    default PredicateObjectMap addPredicate(Resource resource) {
        getPredicates().add(resource);
        return this;
    }


    default PredicateObjectMap addObject(String iri) {
        return addObject(NodeFactory.createURI(iri));
    }

    default PredicateObjectMap addObject(Node node) {
        return addObject(getModel().wrapAsResource(node));
    }

    default PredicateObjectMap addObject(Resource resource) {
        getObjects().add(resource);
        return this;
    }


    default PredicateObjectMap addGraph(String iri) {
        return addGraph(NodeFactory.createURI(iri));
    }

    default PredicateObjectMap addGraph(Node node) {
        return addGraph(getModel().wrapAsResource(node));
    }

    default PredicateObjectMap addGraph(Resource resource) {
        getGraphs().add(resource);
        return this;
    }


//	Resource getPredicate();
//	PredicateObjectMap setPredicate(Resource predicate);
//
//	TermMap getPredicateMap();
//	PredicateObjectMap setPredicateMap(TermMap termMap);
//
//	TermMap getObjectMap();
//	PredicateObjectMap setObjectMap(TermMap termMap);
}
