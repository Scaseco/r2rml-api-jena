package org.aksw.r2rml.jena.domain.api;

import java.util.Set;

import org.aksw.jenax.annotation.reprogen.Iri;
import org.aksw.jenax.annotation.reprogen.ResourceView;
import org.aksw.r2rml.common.vocab.R2rmlTerms;
import org.apache.jena.rdf.model.Resource;

@ResourceView
public interface SubjectMap
	extends TermMap, HasGraphMap
{	

	/**
	 * Return a set view (never null) of resources specified via rr:class
	 * 
	 * @return
	 */
	@Iri(R2rmlTerms.xclass)
	Set<Resource> getClasses();
}
