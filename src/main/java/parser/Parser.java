package parser;

import com.github.javaparser.ParserConfiguration;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * The common parent of parsers.
 *
 * @author Zdenek Tronicek, tronicek@tarleton.edu
 */
public abstract class Parser {

    protected final Properties conf;
    protected final ParserConfiguration parserConfiguration = new ParserConfiguration();

    public static Parser instantiate(Properties conf) {
        String level = conf.getProperty("level");
        switch (level) {
            case "method":
                return new MethodParser(conf);
            case "statement":
            case "statements":
                return new StatementParser(conf);
            default:
                throw new AssertionError("invalid level: " + level);
        }
    }

    protected Parser(Properties conf) {
        this.conf = conf;
        prepareParserConfiguration();
    }

    private void prepareParserConfiguration() {
        boolean preprocessUnicodeEscapes = Boolean.parseBoolean(conf.getProperty("preprocessUnicodeEscapes"));
        String languageLevel = conf.getProperty("languageLevel", "JAVA_8");
        String sourceEncoding = conf.getProperty("sourceEncoding", "UTF-8");
        ParserConfiguration.LanguageLevel lang = ParserConfiguration.LanguageLevel.valueOf(languageLevel);
        parserConfiguration.setPreprocessUnicodeEscapes(preprocessUnicodeEscapes);
        parserConfiguration.setLanguageLevel(lang);
        Charset cs = Charset.forName(sourceEncoding);
        parserConfiguration.setCharacterEncoding(cs);
    }

    public abstract Tokens parse(String code, boolean normalize);

    public abstract Lines parseToLines(String code, boolean normalize);
}
