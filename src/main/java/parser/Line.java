package parser;

import com.github.javaparser.JavaToken;
import java.util.List;

/**
 *
 * @author Zdenek Tronicek
 */
public class Line {

    private final List<JavaToken> tokens;

    public Line(List<JavaToken> tokens) {
        this.tokens = tokens;
    }

    public List<JavaToken> getTokens() {
        return tokens;
    }

    public JavaToken getToken(int index) {
        return tokens.get(index);
    }

    public int size() {
        return tokens.size();
    }
}
