package nicad;

import com.github.javaparser.JavaToken;
import parser.ParseException;
import parser.Parser;
import edu.tarleton.drdup2.nicad.NiCadClone;
import edu.tarleton.drdup2.nicad.NiCadClones;
import edu.tarleton.drdup2.nicad.NiCadSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import parser.Line;
import parser.Lines;

/**
 * This class computes the similarity in a NiCad XML file.
 *
 * @author Zdenek Tronicek
 */
public class Similarity {

    private final Properties conf;
    private final boolean treatNullAsLiteral;
    private final boolean treatSuperThisAsIdentifier;

    public Similarity(Properties conf) {
        this.conf = conf;
        treatNullAsLiteral = Boolean.parseBoolean(conf.getProperty("treatNullAsLiteral", "false"));
        treatSuperThisAsIdentifier = Boolean.parseBoolean(conf.getProperty("treatSuperThisAsIdentifier", "false"));
    }

    public void process() throws Exception {
        Parser parser = Parser.instantiate(conf);
        String input = conf.getProperty("inputFile");
        NiCadClones cls = readFile(input);
        for (NiCadClone clone : cls.getClones()) {
            processClone(parser, clone);
        }
        int i = input.lastIndexOf(".xml");
        String output = input.substring(0, i) + "-similarity.xml";
        writeTextFile(cls, output);
    }

    private NiCadClones readFile(String fileName) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(NiCadClones.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        return (NiCadClones) unmarshaller.unmarshal(new File(fileName));
    }

    private void processClone(Parser parser, NiCadClone clone) {
        try {
            List<Lines> lines = new ArrayList<>();
            for (NiCadSource src : clone.getSources()) {
                Lines p = parser.parseToLines(src.getSourceCode(), true);
                lines.add(p);
            }
            if (lines.size() > 2) {
                throw new AssertionError("clone class found");
            }
            Lines lines1 = lines.get(0);
            Lines lines2 = lines.get(1);
            int s = similarity(lines1.getLines(), lines2.getLines());
            clone.setSimilarity(s);
            clone.setDistance(null);
        } catch (ParseException e) {
            clone.setSimilarity(0);
            clone.setDistance(null);
            System.err.println("parsing failed");
        }
    }

    private int similarity(List<Line> lines1, List<Line> lines2) {
        List<Line> m1 = removeBrace(lines1);
        List<Line> m2 = removeBrace(lines2);
        int lcs = LCS(m1, m2);
        //double d = 2.0 * lcs / (m1.size() + m2.size());
        double d = 1.0 * lcs / Math.max(m1.size(), m2.size());
        return (int) Math.round(d * 100);
    }

    private List<Line> removeBrace(List<Line> lines) {
        List<Line> p = new ArrayList<>();
        for (Line line : lines) {
            if (line.size() == 1) {
                JavaToken t = line.getToken(0);
                if (t.getKind() == JavaToken.Kind.RBRACE.getKind()) {
                    continue;
                }
            }
            p.add(line);
        }
        return p;
    }

    private int LCS(List<Line> lines1, List<Line> lines2) {
        int m = lines1.size();
        int[] d = new int[m + 1];
        for (int i = 0; i < d.length; i++) {
            d[i] = 0;
        }
        int[] nd = new int[d.length];
        for (int i = 0; i < lines2.size(); i++) {
            nd[0] = 0;
            for (int j = 0; j < m; j++) {
                Line line1 = lines1.get(j);
                Line line2 = lines2.get(i);
                boolean res = equal(line1.getTokens(), line2.getTokens());
                if (res) {
                    nd[j + 1] = d[j] + 1;
                } else {
                    nd[j + 1] = Math.max(d[j + 1], nd[j]);
                }
            }
            int[] p = d;
            d = nd;
            nd = p;
        }
        return d[m];
    }

    private boolean equal(List<JavaToken> tt1, List<JavaToken> tt2) {
        if (tt1.size() != tt2.size()) {
            return false;
        }
        for (int i = 0; i < tt1.size(); i++) {
            JavaToken t1 = tt1.get(i);
            JavaToken t2 = tt2.get(i);
            if (!equal(t1, t2)) {
                return false;
            }
        }
        return true;
    }

    private boolean equal(JavaToken tok1, JavaToken tok2) {
        String s1 = tok1.getText();
        String s2 = tok2.getText();
        if (tok1.getKind() == tok2.getKind() && s1.equals(s2)) {
            return true;
        }
        if (isIdentifier(tok1) && isIdentifier(tok2)) {
            return true;
        }
        if (isLiteral(tok1) && isLiteral(tok2)) {
            return true;
        }
        return false;
    }

    private boolean isIdentifier(JavaToken token) {
        switch (token.getCategory()) {
            case IDENTIFIER:
                return true;
            case KEYWORD:
                String s = token.getText();
                if (treatSuperThisAsIdentifier && isSuperThis(s)) {
                    return true;
                }
                return isPrimitiveType(s);
            default:
                return false;
        }
    }

    private boolean isSuperThis(String token) {
        return token.equals("super") || token.equals("this");
    }

    private boolean isPrimitiveType(String token) {
        switch (token) {
            case "boolean":
            case "byte":
            case "char":
            case "double":
            case "float":
            case "int":
            case "long":
            case "short":
                return true;
            default:
                return false;
        }
    }

    private boolean isLiteral(JavaToken token) {
        switch (token.getCategory()) {
            case LITERAL:
                return true;
            case KEYWORD:
                String s = token.getText();
                if (s.equals("null") && treatNullAsLiteral) {
                    return true;
                }
                return s.equals("false") || s.equals("true");
            default:
                return false;
        }
    }

    private void writeTextFile(NiCadClones clones, String fileName) throws Exception {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"))) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            out.println("<clones>");
            for (NiCadClone clone : clones.getClones()) {
                out.printf("    <clone nlines=\"%d\" similarity=\"%d\">%n", clone.getNlines(), clone.getSimilarity());
                for (NiCadSource src : clone.getSources()) {
                    out.printf("        <source file=\"%s\" startline=\"%d\" endline=\"%d\">", src.getFile(), src.getStartline(), src.getEndline());
                    String code = src.getSourceCode()
                            .replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;");
                    out.print(code);
                    out.println("</source>");
                }
                out.println("    </clone>");
            }
            out.println("</clones>");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("expected argument: properties file (e.g. JavaAPI.properties)");
            System.exit(0);
        }
        Properties conf = new Properties();
        try (FileReader in = new FileReader(args[0])) {
            conf.load(in);
        }
        Similarity sim = new Similarity(conf);
        sim.process();
    }
}
