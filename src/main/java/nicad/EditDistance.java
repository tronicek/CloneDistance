package nicad;

import com.github.javaparser.JavaToken;
import parser.ParseException;
import parser.Parser;
import parser.Tokens;
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

/**
 * This class computes the edit distance in a NiCad XML file.
 *
 * @author Zdenek Tronicek
 */
public class EditDistance {

    private final Properties conf;
    private final boolean treatNullAsLiteral;
    private final boolean treatSuperThisAsIdentifier;

    public EditDistance(Properties conf) {
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
        String output = input.substring(0, i) + "-distance.xml";
        writeTextFile(cls, output);
    }

    private NiCadClones readFile(String fileName) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(NiCadClones.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        return (NiCadClones) unmarshaller.unmarshal(new File(fileName));
    }

    private void processClone(Parser parser, NiCadClone clone) {
        try {
            List<Tokens> tokens = new ArrayList<>();
            for (NiCadSource src : clone.getSources()) {
                Tokens tt = parser.parse(src.getSourceCode(), true);
                tokens.add(tt);
            }
            if (tokens.size() > 2) {
                throw new AssertionError("clone class found");
            }
            Tokens tt1 = tokens.get(0);
            Tokens tt2 = tokens.get(1);
            int d = levenshteinDistance(tt1.getTokens(), tt2.getTokens());
            clone.setDistance(d);
            clone.setSimilarity(null);
        } catch (ParseException e) {
            clone.setDistance(Integer.MAX_VALUE);
            clone.setSimilarity(null);
            System.err.println("parsing failed");
        }
    }

    private int levenshteinDistance(List<JavaToken> tt1, List<JavaToken> tt2) {
        int m = tt1.size();
        int[] d = new int[m + 1];
        for (int i = 0; i < d.length; i++) {
            d[i] = i;
        }
        int[] nd = new int[d.length];
        for (int i = 0; i < tt2.size(); i++) {
            nd[0] = i + 1;
            for (int j = 0; j < m; j++) {
                JavaToken t1 = tt1.get(j);
                JavaToken t2 = tt2.get(i);
                boolean res = equal(t1, t2);
                if (res) {
                    nd[j + 1] = d[j];
                } else {
                    nd[j + 1] = 1 + min(d[j + 1], nd[j], d[j]);
                }
            }
            int[] p = d;
            d = nd;
            nd = p;
        }
        return d[m];
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

    private int min(int a, int b, int c) {
        int m = a < b ? a : b;
        if (c < m) {
            m = c;
        }
        return m;
    }

    private void writeTextFile(NiCadClones clones, String fileName) throws Exception {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"))) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            out.println("<clones>");
            for (NiCadClone clone : clones.getClones()) {
                out.printf("    <clone nlines=\"%d\" distance=\"%d\">%n", clone.getNlines(), clone.getDistance());
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
        EditDistance ed = new EditDistance(conf);
        ed.process();
    }
}
