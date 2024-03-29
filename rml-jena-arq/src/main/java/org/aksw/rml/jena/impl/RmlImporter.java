package org.aksw.rml.jena.impl;

import java.util.Collection;
import java.util.stream.Collectors;

import org.aksw.r2rml.jena.arq.impl.TriplesMapToSparqlMapping;
import org.aksw.rml.jena.plugin.ReferenceFormulationRegistry;
import org.aksw.rml.model.RmlTriplesMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class RmlImporter {
    /* The configured rml model - won't be changed */
    protected Model rmlModel;
    protected String baseIri;
    protected Model fnmlModel;
    protected Collection<String> triplesMapIds;
    protected ReferenceFormulationRegistry registry;

    /** The effective rml model which is derived from the {@link #rmlModel} */
//    protected Model effectiveRmlModel;
//    protected Collection<RmlTriplesMap> effectiveWorkloads;

    /** Create instances using {@link #from(Model)} */
    protected RmlImporter(Model rmlModel) {
        super();
        this.rmlModel = rmlModel;
    }

    public static RmlImporter from(Model rmlModel) {
        return new RmlImporter(rmlModel);
    }

    public Model getRmlModel() {
        return rmlModel;
    }

    public RmlImporter setRmlModel(Model rmlModel) {
        this.rmlModel = rmlModel;
        return this;
    }

    public String getBaseIri() {
        return baseIri;
    }

    public RmlImporter setBaseIri(String baseIri) {
        this.baseIri = baseIri;
        return this;
    }

    public Model getFnmlModel() {
        return fnmlModel;
    }

    public RmlImporter setFnmlModel(Model fnmlModel) {
        this.fnmlModel = fnmlModel;
        return this;
    }

    public Collection<String> getTriplesMapIds() {
        return triplesMapIds;
    }

    public RmlImporter setTriplesMapIds(Collection<String> triplesMapIds) {
        this.triplesMapIds = triplesMapIds;
        return this;
    }

    public ReferenceFormulationRegistry getReferenceFormulationRegistry() {
        return registry;
    }

    public RmlImporter setReferenceFormulationRegistry(ReferenceFormulationRegistry registry) {
        this.registry = registry;
        return this;
    }

    protected Collection<RmlTriplesMap> buildWorkloads(Model effectiveRmlModel) {
        Collection<RmlTriplesMap> result;
        if (triplesMapIds != null && !triplesMapIds.isEmpty()) {
            result = triplesMapIds.stream()
                .map(id -> effectiveRmlModel.createResource(id).as(RmlTriplesMap.class))
                .collect(Collectors.toList());
        } else {
            result = RmlImporterLib.listAllTriplesMaps(rmlModel);
        }
        return result;
    }

    protected Collection<TriplesMapToSparqlMapping> processWorkloads(Collection<RmlTriplesMap> workloads) {
        Collection<TriplesMapToSparqlMapping> result = workloads.stream()
            .map(workload -> new TriplesMapProcessorRml(workload, baseIri, fnmlModel, registry).call())
            .collect(Collectors.toList());
        return result;
    }

    protected Model buildEffectiveRmlModel() {
        // Create a copy of the RML model because we will expand shortcuts
        Model result = ModelFactory.createDefaultModel();
        result.add(rmlModel);
        return result;
    }

    public Collection<TriplesMapToSparqlMapping> process() {
        Model effectiveRmlModel = buildEffectiveRmlModel();
        Collection<RmlTriplesMap> workloads = buildWorkloads(effectiveRmlModel);
        Collection<TriplesMapToSparqlMapping> result = processWorkloads(workloads);
        return result;
    }
}
