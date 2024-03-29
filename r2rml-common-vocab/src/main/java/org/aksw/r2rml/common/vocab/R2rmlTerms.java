package org.aksw.r2rml.common.vocab;

/**
 * The terms of the R2RML vocabulary as strings.
 * The Jena adaption is the class {@link org.aksw.r2rml.jena.vocab.RR} in the r2rm-jena-vocab module
 *
 *
 * TODO Rename to R2RMLTerms
 *
 * @author sherif
 * @author Claus Stadler
 *
 */
public class R2rmlTerms {
    public static final String uri = "http://www.w3.org/ns/r2rml#";

    public static final String TriplesMap = uri + "TriplesMap";

    public static final String LogicalTable = uri + "LogicalTable";
    public static final String R2RMLView = uri + "R2RMLView";
    public static final String BaseTableOrView = uri + "BaseTableOrView";

    public static final String TermMap = uri + "TermMap";
    public static final String GraphMap = uri + "GraphMap";
    public static final String SubjectMap = uri + "SubjectMap";
    public static final String PredicateMap = uri + "PredicateMap";
    public static final String ObjectMap = uri + "ObjectMap";

    public static final String PredicateObjectMap = uri + "PredicateObjectMap";

    public static final String RefObjectMap = uri + "RefObjectMap";

    public static final String Join = uri + "Join";

    public static final String child 				= uri + "child";
    public static final String xclass 				= uri + "class";
    public static final String column 				= uri + "column";
    public static final String datatype 			= uri + "datatype";
    public static final String constant 			= uri + "constant";
    public static final String graph 				= uri + "graph";
    public static final String graphMap 			= uri + "graphMap";
    public static final String inverseExpression 	= uri + "inverseExpression";
    public static final String joinCondition 		= uri + "joinCondition";
    public static final String language 			= uri + "language";
    public static final String logicalTable 		= uri + "logicalTable";
    public static final String object 				= uri + "object";
    public static final String objectMap 			= uri + "objectMap";
    public static final String parent 				= uri + "parent";
    public static final String parentTriplesMap 	= uri + "parentTriplesMap";
    public static final String predicate 			= uri + "predicate";
    public static final String predicateMap 		= uri + "predicateMap";
    public static final String predicateObjectMap	= uri + "predicateObjectMap";
    public static final String sqlQuery 			= uri + "sqlQuery";
    public static final String sqlVersion 			= uri + "sqlVersion";
    public static final String subject 				= uri + "subject";
    public static final String subjectMap 			= uri + "subjectMap";
    public static final String tableName			= uri + "tableName";
    public static final String template 			= uri + "template";
    public static final String termType 			= uri + "termType";
    public static final String BlankNode			= uri + "BlankNode";

    // Other Terms
    public static final String defaultGraph 		= uri + "defaultGraph";
    public static final String SQL2008 				= uri + "SQL2008";
    public static final String IRI 					= uri + "IRI";

    public static final String Literal 				= uri + "Literal";
}
