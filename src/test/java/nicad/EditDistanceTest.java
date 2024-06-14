package nicad;

import edu.tarleton.drdup2.nicad.NiCadClone;
import edu.tarleton.drdup2.nicad.NiCadClones;
import java.io.File;
import java.util.List;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit tests.
 *
 * @author Zdenek Tronicek, tronicek@tarleton.edu
 */
public class EditDistanceTest {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private void test(Properties conf, String input) throws Exception {
        conf.setProperty("inputFile", "src/test/methods/" + input);
        conf.setProperty("rename", "blind");
        conf.setProperty("level", "method");
        EditDistance ed = new EditDistance(conf);
        ed.process();
        String inFile = conf.getProperty("inputFile");
        int d = inFile.lastIndexOf(".xml");
        String outFile = inFile.substring(0, d) + "-distance.xml";
        NiCadClones cls = readFile(inFile);
        NiCadClones cls2 = readFile(outFile);
        List<NiCadClone> clones = cls.getClones();
        List<NiCadClone> clones2 = cls2.getClones();
        for (int i = 0; i < clones.size(); i++) {
            NiCadClone cl = clones.get(i);
            NiCadClone cl2 = clones2.get(i);
            int sim = cl.getSimilarity();
            int dist = cl2.getDistance();
            assertEquals(100 - sim, dist);
        }
    }

    private void test(String input) throws Exception {
        Properties conf = new Properties();
        test(conf, input);
    }

    private NiCadClones readFile(String fileName) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(NiCadClones.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        return (NiCadClones) unmarshaller.unmarshal(new File(fileName));
    }

    @Test
    public void test1() throws Exception {
        test("test1.xml");
    }

    @Test
    public void test2a() throws Exception {
        test("test2a.xml");
    }

    @Test
    public void test2b() throws Exception {
        test("test2b.xml");
    }

    @Test
    public void test3() throws Exception {
        Properties conf = new Properties();
        conf.setProperty("ignoreUnaryAtLiterals", "true");
        test(conf, "test3.xml");
    }

    @Test
    public void test4() throws Exception {
        test("test4.xml");
    }

    @Test
    public void test5() throws Exception {
        Properties conf = new Properties();
        conf.setProperty("ignoreParentheses", "true");
        test(conf, "test5.xml");
    }

    @Test
    public void test6() throws Exception {
        test("test6.xml");
    }

    @Test
    public void test7() throws Exception {
        test("test7.xml");
    }

    @Test
    public void test8() throws Exception {
        test("test8.xml");
    }

    @Test
    public void test9() throws Exception {
        test("test9.xml");
    }

    @Test
    public void test10() throws Exception {
        test("test10.xml");
    }
}
