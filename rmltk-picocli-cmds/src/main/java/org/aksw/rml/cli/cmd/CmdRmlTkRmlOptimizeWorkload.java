package org.aksw.rml.cli.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.aksw.commons.algebra.allen.AllenRelations;
import org.aksw.commons.util.range.Cmp;
import org.aksw.commons.util.range.RangeTreeNode;
import org.aksw.jena_sparql_api.algebra.transform.TransformPullExtend;
import org.aksw.jena_sparql_api.rx.script.SparqlScriptProcessor;
import org.aksw.jenax.arq.util.node.ComparableNodeValue;
import org.aksw.jenax.arq.util.quad.QuadUtils;
import org.aksw.jenax.arq.util.syntax.QueryGenerationUtils;
import org.aksw.jenax.arq.util.syntax.QueryUtils;
import org.aksw.jenax.constraint.api.VSpace;
import org.aksw.jenax.constraint.impl.VSpaceImpl;
import org.aksw.jenax.constraint.util.NodeRanges;
import org.aksw.jenax.stmt.core.SparqlStmt;
import org.aksw.rml.jena.impl.Clusters;
import org.aksw.rml.jena.impl.Clusters.Cluster;
import org.aksw.rml.jena.impl.RmlLib;
import org.aksw.rml.jena.impl.UnsortedUtils;
import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.atlas.lib.tuple.TupleFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryType;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.Template;

import com.google.common.collect.Range;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "workload", description = "Optimize a workload of sparql queries by merging queries having the same RML source")
public class CmdRmlTkRmlOptimizeWorkload
    implements Callable<Integer>
{
    @Parameters(arity = "1..n", description = "Input RML file(s)")
    public List<String> inputFiles = new ArrayList<>();

//    @Option(names = { "--no-distinct" }, description = "Apply intra-query distinct", defaultValue = "false")
//    public boolean distinct = false;

    @Option(names = { "--cluster-by-predicate" }, description = "Cluster only by predicates. This is inferior to the default clustering by subject's value spaces.", defaultValue = "false")
    public boolean clusterByPredicate = false;

    @Option(names = { "--no-order" }, description = "Do not sort the result", defaultValue = "false")
    public boolean noOrder = false;

    @Option(names = { "--no-group" }, description = "Do not group queries that have the same source", defaultValue = "false")
    public boolean noGroup = false;


    @Option(names = { "--verbose" }, description = "Display additional information such as the partition tree", defaultValue = "false")
    public boolean verbose = false;


    /** TODO This method fits better in the to-sparql command */
    @Option(names = { "--pre-distinct" }, description = "Whenever DISTINCT is applied to the outcome of a UNION then also apply distinct to each member first", defaultValue = "false")
    public boolean preDistinct = false;


    /** Returns 0 when ranges overlap */
    public static <T extends Comparable<T>> int compareRanges(Range<T> x, Range<T> y) {
        int result = AllenRelations.isBefore(x, y)
                ? -1
                : AllenRelations.isAfter(x, y)
                    ? 1
                    : 0;
        return result;
    }




    public static <K extends Comparable<K>, V> List<List<Entry<Tuple<Range<K>>, V>>> clusterRangeTuplesByComponent(List<Entry<Tuple<Range<K>>, V>> items, int[] order) {
        List<List<Entry<Tuple<Range<K>>, V>>> clusters = List.of(items);
        for (int i = 0; i < order.length; ++i) {
            int componentIdx = order[i];
            List<List<Entry<Tuple<Range<K>>, V>>> nextClusters = new ArrayList<>();
            for (Collection<Entry<Tuple<Range<K>>, V>> cluster : clusters) {
                List<List<Entry<Tuple<Range<K>>, V>>> contrib = clusterRangeTuplesByComponent(cluster, componentIdx);
                nextClusters.addAll(contrib);
            }
            clusters = nextClusters;
        }
        return clusters;
    }



    public static <K extends Comparable<K>, V> List<List<Entry<Tuple<Range<K>>, V>>> clusterRangeTuplesByComponent(Collection<Entry<Tuple<Range<K>>, V>> items, int componentIdx) {
        RangeTreeNode<K, Entry<Tuple<Range<K>>, V>> tree = RangeTreeNode.newRoot();
        for (Entry<Tuple<Range<K>>, V> item : items) {
            Tuple<Range<K>> tuple = item.getKey();
            Range<K> range = tuple.get(componentIdx);
            tree.put(range, item);
        }

        // Each child of the root forms a cluster
        List<List<Entry<Tuple<Range<K>>, V>>> clusters = new ArrayList<>(tree.getChildNodes().size());
        for (RangeTreeNode<K, Entry<Tuple<Range<K>>, V>> node : tree.getChildNodes()) {
            List<Entry<Tuple<Range<K>>, V>> cluster = node.streamAllValuesPreOrder().collect(Collectors.toList());
            clusters.add(cluster);
        }
        return clusters;
    }


    @Override
    public Integer call() throws Exception {
        SparqlScriptProcessor processor = SparqlScriptProcessor.createPlain(null, null);
        processor.process(inputFiles);
        List<SparqlStmt> stmts = new ArrayList<>(processor.getPlainSparqlStmts());

        // TODO Only queries are supported - add a meaningful error message on violation
        List<Query> queries = stmts.stream().map(SparqlStmt::getQuery).collect(Collectors.toList());

        Quad quadVars = Quad.create(Var.alloc("__g__"), Var.alloc("__s__"), Var.alloc("__p__"), Var.alloc("__o__"));
        Quad sortVars = noOrder ? null : Quad.create(quadVars.getSubject(), quadVars.getPredicate(), quadVars.getObject(), quadVars.getGraph());

        if (!clusterByPredicate) {
            // TODO Clustering has yet to be handled
            // Convert to construct to lateral union

            if (noOrder) {
                int order[] = {1, 2, 3, 0};

//                // Note: the multimap actually violates the collection contract
                List<Entry<Tuple<Range<Cmp<Entry<?, Cmp<ComparableNodeValue>>>>>, Integer>> init = new ArrayList<>();
                int i = 0;
                for (Query query : queries) {
                    indexQuery(init, i, query);
                    ++i;
                }
                List<List<Entry<Tuple<Range<Cmp<Entry<?, Cmp<ComparableNodeValue>>>>>, Integer>>> clusters = clusterRangeTuplesByComponent(init, order);

                List<Query> newQueries = new ArrayList<>(clusters.size());
                for (List<Entry<Tuple<Range<Cmp<Entry<?, Cmp<ComparableNodeValue>>>>>, Integer>> cluster : clusters) {
                    Collection<Integer> queryIdxs = cluster.stream().map(Entry::getValue).collect(Collectors.toList());
                    List<Query> qs = queryIdxs.stream().map(queries::get).collect(Collectors.toList());

                    if (!noGroup) {
                        RmlLib.optimizeRmlWorkloadInPlace(qs);
                    }

                    Query clusterQuery = mergeConstructQueriesIntoUnion(quadVars, qs, sortVars, preDistinct);
                    newQueries.add(clusterQuery);
                }

                queries = newQueries.stream().map(q -> finalizeQuery(quadVars, List.of(q), null)).collect(Collectors.toList());


                // pull OpExtent up as far as possible - especially over distinct
                boolean pullExtent = true;
                if (pullExtent) {
                    queries = queries.stream().map(q -> {
                        Op rawOp = Algebra.compile(q);
                        Op op = TransformPullExtend.transform(rawOp);
                        Query tmp = OpAsQuery.asQuery(op);
                        Query r = QueryUtils.restoreQueryForm(tmp, q);
                        return r;
                    }).collect(Collectors.toList());
                }


            } else {

                RangeTreeNode<Cmp<Entry<?, Cmp<ComparableNodeValue>>>, Integer> tree = RangeTreeNode.newRoot();
                int i = 0;
                for (Query query : queries) {
                    Map<Quad, Tuple<VSpace>> cquads = UnsortedUtils.analyzeQuads(query);
                    for(Tuple<VSpace> t : cquads.values()) {
                        Range<Cmp<Entry<?, Cmp<ComparableNodeValue>>>> range = VSpaceImpl.span(t.get(1));
                        tree.put(range, i);
                    }
                    ++i;
                }

                if (verbose) {
                    System.err.println(tree);
                }

                // Each child of the root forms a cluster
                List<Query> newQueries = new ArrayList<>(tree.getChildNodes().size());
                for (RangeTreeNode<Cmp<Entry<?, Cmp<ComparableNodeValue>>>, Integer> node : tree.getChildNodes()) {
                    List<Integer> queryIdxs = node.streamAllValuesPreOrder().collect(Collectors.toList());
                    List<Query> qs = queryIdxs.stream().map(queries::get).collect(Collectors.toList());

                    if (!noGroup) {
                        RmlLib.optimizeRmlWorkloadInPlace(qs);
                    }

                    Query clusterQuery = mergeConstructQueriesIntoUnion(quadVars, qs, sortVars, preDistinct);
                    newQueries.add(clusterQuery);
                }


                // Query finalQuery = finalizeQuery(quadVars, newQueries, null);
                // queries = List.of(finalQuery);
                queries = newQueries.stream().map(q -> finalizeQuery(quadVars, List.of(q), null)).collect(Collectors.toList());
            }
//            System.out.println(tree);
//
//            System.out.println(finalQuery);
//            return 0;
        } else {

            RmlLib.optimizeRmlWorkloadInPlace(queries);



            // boolean intraDistinct;
            boolean distinct = true;
            // Adding ?s ?p ?o should collapse all clusters into a single one
            // queries.add(QueryFactory.create("CONSTRUCT WHERE { ?s ?p ?o }"));
            if (distinct) {
                // TODO Clustering has yet to be handled
                // Convert to construct to lateral union
                // Quad quadVars = Quad.create(Var.alloc("__g__"), Var.alloc("__s__"), Var.alloc("__p__"), Var.alloc("__o__"));

                Clusters<Quad, Query> clusters = RmlLib.groupConstructQueriesByTemplate(queries);
                List<Query> newQueries = combine(quadVars, clusters, preDistinct);
                Query finalQuery = finalizeQuery(quadVars, newQueries, sortVars);

                queries = Collections.singletonList(finalQuery);
            }
        }

        for (Query query : queries) {
            System.out.println(query);
        }
        return 0;
    }



    /** This method exists to ease debugging by allowing for "drop to frame" */
    private void indexQuery(List<Entry<Tuple<Range<Cmp<Entry<?, Cmp<ComparableNodeValue>>>>>, Integer>> init, int queryId,
            Query query) {
        Map<Quad, Tuple<VSpace>> cquads = UnsortedUtils.analyzeQuads(query);
        Collection<Tuple<VSpace>> vstuples;

        if (false) {
             vstuples = cquads.values();
        } else {
            // Create a component-wise union of the vspaces
            List<VSpace> vspaces = new ArrayList<>(4);
            for (int i = 0; i < 4; ++i) {
                VSpace vspace = VSpaceImpl.create(NodeRanges.createClosed());
                for(Tuple<VSpace> t : cquads.values()) {
                    VSpace contrib = t.get(i);
                    vspace.stateUnion(contrib);
                }
                vspaces.add(vspace);
            }
            vstuples = List.of(TupleFactory.create(vspaces));
        }

        for(Tuple<VSpace> t : vstuples) {
            Tuple<Range<Cmp<Entry<?, Cmp<ComparableNodeValue>>>>> rt = t.map(VSpaceImpl::span);
            // clusters.put(rt, i);
            init.add(Map.entry(rt, queryId));
        }
    }

    public static Query finalizeQuery(Quad quadVars, List<Query> newQueries, Quad sortVars) {
        Query innerQuery;
        if (newQueries.size() == 1) {
            innerQuery = newQueries.iterator().next();
        } else {
            List<Element> ms = newQueries.stream().map(ElementSubQuery::new).collect(Collectors.toList());
            innerQuery = createUnionQuery(ms, quadVars, false);
        }

        Query finalQuery = new Query();
        finalQuery.setQueryConstructType();
        finalQuery.setConstructTemplate(new Template(new QuadAcc(Collections.singletonList(quadVars))));
        finalQuery.setQueryPattern(new ElementSubQuery(innerQuery));

        // if (!noOrder) {
        if (sortVars != null) {
            QuadUtils.streamNodes(sortVars).forEach(n -> finalQuery.addOrderBy(n, Query.ORDER_ASCENDING));
        }
        return finalQuery;
    }

    public static Query mergeConstructQueriesIntoUnion(Quad quadVars, Collection<Query> queries, Quad sortVars, boolean preDistinct) {
        Query result;
        if (queries.size() == 1) {
            Query member = queries.iterator().next();
            result = QueryGenerationUtils.constructToLateral(member, quadVars, QueryType.SELECT, true, true);
        } else {
            List<Element> tmp = queries.stream()
                    .map(member -> QueryGenerationUtils.constructToLateral(member, quadVars, QueryType.SELECT, preDistinct, true))
                    .map(ElementSubQuery::new)
                    .collect(Collectors.toList());

            result = createUnionQuery(tmp, quadVars, true);
        }

        if (sortVars != null) {
            QuadUtils.streamNodes(sortVars).forEach(n -> result.addOrderBy(n, Query.ORDER_ASCENDING));
        }

        return result;
    }

    private List<Query> combine(Quad quadVars, Clusters<Quad, Query> clusters, boolean preDistinct) {
        List<Query> newQueries = new ArrayList<>();
        for (Entry<Integer, Cluster<Quad, Query>> e : clusters.entrySet()) {
            System.err.println("Cluster " + e.getKey() + ": " + e.getValue().getValues().size() + " entries");

            Query newQuery = mergeConstructQueriesIntoUnion(quadVars, e.getValue().getValues(), null, preDistinct);
            newQueries.add(newQuery);
        }
        return newQueries;
    }

    public static Query createUnionQuery(List<Element> members, Quad quadVars, boolean distinct) {
        Query result = new Query();
        result.setQuerySelectType();
        result.setDistinct(distinct);
        QuadUtils.streamNodes(quadVars).forEach(v -> result.getProject().add((Var)v));
        ElementUnion unionElt = new ElementUnion();
        for (Element member : members) {
            unionElt.addElement(member);
        }
        result.setQueryPattern(unionElt);
        return result;
    }

}
