package parser;

import java.util.List;

/**
 * The class that represents lines created from the source code.
 * 
 * @author Zdenek Tronicek, tronicek@tarleton.edu
 */
public class Lines {

    private final List<Line> lines;
    private final boolean wasNormalized;

    public Lines(List<Line> lines, boolean wasNormalized) {
        this.lines = lines;
        this.wasNormalized = wasNormalized;
    }

    public List<Line> getLines() {
        return lines;
    }
    
    public Line getLine(int index) {
        return lines.get(index);
    }

    public boolean wasNormalized() {
        return wasNormalized;
    }
    
    public int size() {
        return lines.size();
    }
}
