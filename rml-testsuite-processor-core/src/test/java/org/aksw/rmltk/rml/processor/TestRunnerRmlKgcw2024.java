package org.aksw.rmltk.rml.processor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.commons.util.lifecycle.ResourceMgr;
import org.aksw.jenax.arq.util.quad.DatasetCmp;
import org.aksw.jenax.arq.util.quad.DatasetCmp.Report;
import org.aksw.rml.jena.impl.RmlImporterLib;
import org.aksw.rml.jena.plugin.ReferenceFormulationRegistry;
import org.aksw.rml.jena.ref.impl.ReferenceFormulationJsonStrViaService;
import org.aksw.rml.v2.common.vocab.RmlIoTerms;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sys.JenaSystem;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

@RunWith(Parameterized.class)
public class TestRunnerRmlKgcw2024 {
    private static final Logger logger = LoggerFactory.getLogger(TestRunnerRmlKgcw2024.class);

    protected static ResourceMgr resourceMgr = new ResourceMgr();

//    @BeforeClass
//    public static void beforeClass() {
//        resourceMgr = new ResourceMgr();
//    }

    @AfterClass
    public static void afterClass() {
        resourceMgr.close();
    }

    @Parameters(name = "RML TestCase {index}: {0}")
    public static Collection<Object[]> data()
            throws Exception
    {
        JenaSystem.init();
//        InitRmlService.registerServiceRmlSource(ServiceExecutorRegistry.get());

        Path basePath = RmlTestCaseLister.toPath(resourceMgr, TestRunnerRmlKgcw2024.class.getResource("/kgcw/2024/track1").toURI());

        ReferenceFormulationRegistry rfRegistry = new ReferenceFormulationRegistry();
        ReferenceFormulationRegistry.registryDefaults(rfRegistry);


        // Override registration for JSON to *not* use natural mappings
        rfRegistry.put(RmlIoTerms.JSONPath, new ReferenceFormulationJsonStrViaService());


        List<RmlTestCase> testCases = RmlTestCaseLister.list(basePath, resourceMgr, rfRegistry);

        List<Object[]> params = testCases.stream()
                .map(testCase -> new Object[] {testCase.getSuiteName() + "::" + testCase.getName(), testCase})
                .toList();

        return params;
    }

    protected RmlTestCase testCase;

    public TestRunnerRmlKgcw2024(String name, RmlTestCase testCase) {
        this.testCase = testCase;
    }

    @Test
    public void run() {
        // System.out.println("GOT: " + XSDDouble.XSDdouble.unparse(Double.valueOf(123)));

        Set<String> ignoreSet = new HashSet<>(Arrays.asList("RMLTC0009a-MySQL", "RMLTC0009b-MySQL"));

        // The following two tests are ignored
        if (ignoreSet.contains(testCase.getName())) {
            Assume.assumeTrue(false);
        }

        Map<String, Dataset> expectedResultDses = testCase.getExpectedResultDses();
        // TODO: support multiple output files
        Map.Entry<String, Dataset> expectedResult = expectedResultDses.entrySet()
                .stream().min((a, b) -> Ints.saturatedCast(
                        b.getValue().asDatasetGraph().stream().count() - a.getValue().asDatasetGraph().stream().count()))
                .orElse(null);
        try {
            Model rmlModel = testCase.loadModel();
            RmlImporterLib.validateRml2(rmlModel);

            Dataset actualDs = testCase.call();

//            if (testCase.isExpectedFailure()) {
//                return;
//            }
            // Were we expected to fail?
            Assert.assertFalse(testCase.isExpectedFailure());

            boolean isIsomorphic;
            if (false) {
                Report report = DatasetCmp.assessIsIsomorphicByGraph(expectedResult.getValue(), actualDs);

                isIsomorphic = report.isIsomorphic();
                if (!isIsomorphic) {
                    System.out.println("Expected result: ");
                    RDFDataMgr.write(System.out, expectedResult.getValue(), RDFFormat.TRIG_BLOCKS);
                }
            } else {
                isIsomorphic = DatasetCmp.isIsomorphic(expectedResult.getValue(), actualDs, true, System.err, ResultSetLang.RS_TSV);
            }

            Assert.assertTrue(isIsomorphic);

        } catch (Exception e) {
            if (testCase.isExpectedFailure()) {
                logger.error("Expected failure: " + ExceptionUtils.getRootCauseMessage(e));
                // Ignore
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
