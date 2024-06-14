package parser;

import com.github.javaparser.JavaToken;
import java.util.List;

/**
 * The class that represents tokens created from the source code.
 * 
 * @author Zdenek Tronicek, tronicek@tarleton.edu
 */
public class Tokens {

    private final List<JavaToken> tokens;
    private final boolean wasNormalized;

    public Tokens(List<JavaToken> tokens, boolean wasNormalized) {
        this.tokens = tokens;
        this.wasNormalized = wasNormalized;
    }

    public List<JavaToken> getTokens() {
        return tokens;
    }
    
    public JavaToken getToken(int index) {
        return tokens.get(index);
    }

    public boolean wasNormalized() {
        return wasNormalized;
    }
    
    public int size() {
        return tokens.size();
    }
}
