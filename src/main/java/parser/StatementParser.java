package parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParseResult;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The parser that parses statements.
 *
 * @author Zdenek Tronicek, tronicek@tarleton.edu
 */
public class StatementParser extends Parser {

    public StatementParser(Properties conf) {
        super(conf);
    }

    @Override
    public Tokens parse(String code, boolean normalize) {
        JavaParser parser = new JavaParser(parserConfiguration);
        Statement invoc = null;
        String thisOrSuper = constructorInvocation(code);
        if (!thisOrSuper.isEmpty()) {
            invoc = parseConstructorInvocation(parser, thisOrSuper);
            code = code.substring(thisOrSuper.length());
        }
        Statement stmt = parseBlock(parser, "{ " + code + " }");
        List<JavaToken> tokens = new ArrayList<>();
        if (invoc != null) {
            List<JavaToken> itt = tokenize(invoc);
            tokens.addAll(itt);
        }
        List<JavaToken> btt = tokenize(stmt);
        btt = btt.subList(1, btt.size() - 1);
        tokens.addAll(btt);
        boolean wasNormalized = false;
        if (normalize) {
            NormalizingVisitor norm = new NormalizingVisitor(conf, tokens);
            norm.visitBlockStmt(stmt);
            wasNormalized = norm.wasModified();
        }
        return new Tokens(tokens, wasNormalized);
    }

    private String constructorInvocation(String code) {
        String str = code.trim();
        Pattern pat = Pattern.compile("super\\s*\\(");
        Matcher m = pat.matcher(str);
        if (m.find() && m.start() == 0) {
            int i = findCallEnd(str, m.end());
            return str.substring(0, i);
        }
        Pattern pat2 = Pattern.compile("this\\s*\\(");
        Matcher m2 = pat2.matcher(str);
        if (m2.find() && m2.start() == 0) {
            int i = findCallEnd(str, m2.end());
            return str.substring(0, i);
        }
        return "";
    }

    private int findCallEnd(String code, int i) {
        int c = 1;
        for (; c > 0; i++) {
            char ch = code.charAt(i);
            switch (ch) {
                case '(':
                    c++;
                    break;
                case ')':
                    c--;
            }
        }
        while (code.charAt(i) != ';') {
            i++;
        }
        return i + 1;
    }

    private Statement parseConstructorInvocation(JavaParser parser, String code) {
        ParseResult<ExplicitConstructorInvocationStmt> result = parser.parseExplicitConstructorInvocationStmt(code);
        if (!result.isSuccessful()) {
            System.err.println("parser error " + result.getProblems());
            throw new AssertionError();
        }
        return result.getResult().get();
    }

    private Statement parseBlock(JavaParser parser, String code) {
        ParseResult<BlockStmt> result = parser.parseBlock(code);
        if (!result.isSuccessful()) {
            System.err.println("parser error " + result.getProblems());
            throw new AssertionError();
        }
        return result.getResult().get();
    }

    private List<JavaToken> tokenize(Statement stmt) {
        List<JavaToken> tokens = new ArrayList<>();
        Optional<TokenRange> opt = stmt.getTokenRange();
        if (opt.isPresent()) {
            TokenRange range = opt.get();
            JavaToken token = range.getBegin();
            JavaToken end = range.getEnd();
            while (token != end) {
                switch (token.getCategory()) {
                    case COMMENT:
                    case EOL:
                    case WHITESPACE_NO_EOL:
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
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
