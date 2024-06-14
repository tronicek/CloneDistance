package parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.printer.PrettyPrinter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * The parser that parses methods, constructors and initializers.
 *
 * @author Zdenek Tronicek, tronicek@tarleton.edu
 */
public class MethodParser extends Parser {

    public MethodParser(Properties conf) {
        super(conf);
    }

    @Override
    public Tokens parse(String code, boolean normalize) {
        JavaParser parser = new JavaParser(parserConfiguration);
        BodyDeclaration body = parseBody(parser, code);
        List<JavaToken> tokens = tokenize(body, false);
        boolean wasNormalized = false;
        if (normalize) {
            NormalizingVisitor norm = new NormalizingVisitor(conf, tokens);
            norm.visitBody(body);
            wasNormalized = norm.wasModified();
        }
        return new Tokens(tokens, wasNormalized);
    }

    private BodyDeclaration parseBody(JavaParser parser, String code) {
        ParseResult<BodyDeclaration> result = parser.parseBodyDeclaration(code);
        if (!result.isSuccessful()) {
            //System.err.println("parser error " + result.getProblems());
            //System.err.println(code);
            throw new ParseException();
        }
        return result.getResult().get();
    }

    private List<JavaToken> tokenize(BodyDeclaration<?> body, boolean eol) {
        List<JavaToken> tokens = new ArrayList<>();
        Optional<TokenRange> opt = body.getTokenRange();
        if (opt.isPresent()) {
            TokenRange range = opt.get();
            JavaToken token = range.getBegin();
            JavaToken end = range.getEnd();
            while (token != end) {
                switch (token.getCategory()) {
                    case COMMENT:
                    case WHITESPACE_NO_EOL:
                        break;
                    case EOL:
                        if (eol) {
                            tokens.add(token);
                        }
                        break;
                    default:
                        tokens.add(token);
                }
                token = token.getNextToken().orElse(null);
            }
            tokens.add(token);
        }
        return tokens;
    }

    @Override
    public Lines parseToLines(String code, boolean normalize) {
        JavaParser parser = new JavaParser(parserConfiguration);
        BodyDeclaration body = parseBody(parser, code);
        PrettyPrinter pp = new PrettyPrinter();
        String pcode = pp.print(body);
        BodyDeclaration pbody = parseBody(parser, pcode);
        List<JavaToken> tokens = tokenize(pbody, true);
        boolean wasNormalized = false;
        if (normalize) {
            NormalizingVisitor norm = new NormalizingVisitor(conf, tokens);
            norm.visitBody(body);
            wasNormalized = norm.wasModified();
        }
        List<Line> lines = convertToLines(tokens);
        return new Lines(lines, wasNormalized);
    }

    private List<Line> convertToLines(List<JavaToken> tokens) {
        List<Line> lines = new ArrayList<>();
        List<JavaToken> line = new ArrayList<>();
        for (JavaToken tok : tokens) {
            switch (tok.getCategory()) {
                case EOL:
                    if (!line.isEmpty()) {
                        lines.add(new Line(line));
                        line = new ArrayList<>();
                    }
                    break;
                default:
                    line.add(tok);
            }
        }
        if (!line.isEmpty()) {
            lines.add(new Line(line));
        }
        return lines;
    }
}
