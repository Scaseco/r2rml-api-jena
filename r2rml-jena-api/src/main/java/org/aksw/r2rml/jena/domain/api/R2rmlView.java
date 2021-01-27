package org.aksw.r2rml.jena.domain.api;

import java.util.Set;

import org.aksw.jena_sparql_api.mapper.annotation.Iri;
import org.aksw.jena_sparql_api.mapper.annotation.IriType;
import org.aksw.jena_sparql_api.mapper.annotation.ResourceView;
import org.aksw.r2rml.common.vocab.R2rmlTerms;
import org.apache.jena.rdf.model.Resource;

@ResourceView
public interface R2rmlView
	extends LogicalTable
{
	@Iri(R2rmlTerms.sqlQuery)
	String getSqlQuery();
	R2rmlView setSqlQuery(String queryString);	

	@Iri(R2rmlTerms.sqlVersion)
	Set<Resource> getSqlVersions();

	/**
	 * Convenience view of the resource IRIs as strings
	 * 
	 * @return
	 */
	@Iri(R2rmlTerms.sqlVersion)
	@IriType
	Set<String> getSqlVersionIris();
}
