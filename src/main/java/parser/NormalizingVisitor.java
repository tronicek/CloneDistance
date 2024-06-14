package parser;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.List;
import java.util.Properties;

/**
 * The visitor that implements normalization, such as adding curly braces.
 *
 * @author Zdenek Tronicek, tronicek@tarleton.edu
 */
public class NormalizingVisitor extends VoidVisitorAdapter<Void> {

    private final boolean addBlocks;
    private final boolean ignoreAnnotations;
    private final boolean ignoreParentheses;
    private final boolean ignoreUnaryAtLiterals;
    private final List<JavaToken> tokens;
    private boolean modified;

    public NormalizingVisitor(Properties conf, List<JavaToken> tokens) {
        addBlocks = Boolean.parseBoolean(conf.getProperty("addBlocks", "false"));
        ignoreAnnotations = Boolean.parseBoolean(conf.getProperty("ignoreAnnotations", "false"));
        ignoreParentheses = Boolean.parseBoolean(conf.getProperty("ignoreParentheses", "false"));
        ignoreUnaryAtLiterals = Boolean.parseBoolean(conf.getProperty("ignoreUnaryAtLiterals", "false"));
        this.tokens = tokens;
    }

    public boolean wasModified() {
        return modified;
    }

    public void visitBody(BodyDeclaration<?> body) {
        if (body.isConstructorDeclaration()) {
            visit(body.asConstructorDeclaration(), null);
            return;
        }
        if (body.isMethodDeclaration()) {
            visit(body.asMethodDeclaration(), null);
        }
        if (body.isInitializerDeclaration()) {
            visit(body.asInitializerDeclaration(), null);
        }
    }

    public void visitBlockStmt(Statement stmt) {
        if (stmt.isBlockStmt()) {
            visit(stmt.asBlockStmt(), null);
            return;
        }
        throw new AssertionError("invalid statement: " + stmt);
    }

    private void deleteTokens(Node n) {
        TokenRange range = n.getTokenRange().get();
        JavaToken tok = range.getBegin();
        JavaToken end = range.getEnd();
        while (tok != end) {
            deleteToken(tok);
            tok = tok.getNextToken().get();
        }
        deleteToken(tok);
    }

    private void deleteToken(JavaToken token) {
        for (int i = 0; i < tokens.size(); i++) {
            JavaToken tok = tokens.get(i);
            if (tok == token) {
                tokens.remove(i);
                break;
            }
        }
    }

    @Override
    public void visit(EnclosedExpr n, Void arg) {
        if (ignoreParentheses) {
            Node parent = n.getParentNode().get();
            parent.replace(n, n.getInner());
            TokenRange range = n.getTokenRange().get();
            JavaToken begin = range.getBegin();
            deleteToken(begin);
            JavaToken end = range.getEnd();
            deleteToken(end);
            modified = true;
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(ForEachStmt n, Void arg) {
        Statement body = n.getBody();
        if (addBlocks && !body.isBlockStmt()) {
            BlockStmt block = new BlockStmt();
            block.addStatement(body);
            n.setBody(block);            
            addBlock(body);
            modified = true;
        }
        super.visit(n, arg);
    }

    private void addBlock(Statement stmt) {
        TokenRange range = stmt.getTokenRange().get();
        int begin = find(range.getBegin());
        JavaToken lbrace = new JavaToken(JavaToken.Kind.LBRACE.getKind(), "{");
        tokens.add(begin, lbrace);
        int end = find(range.getEnd());
        JavaToken rbrace = new JavaToken(JavaToken.Kind.RBRACE.getKind(), "}");
        tokens.add(end + 1, rbrace);
    }

    private int find(JavaToken token) {
        for (int i = 0; i < tokens.size(); i++) {
            JavaToken tok = tokens.get(i);
            if (tok == token) {
                return i;
            }
        }
        throw new AssertionError();
    }

    @Override
    public void visit(ForStmt n, Void arg) {
        Statement body = n.getBody();
        if (addBlocks && !body.isBlockStmt()) {
            BlockStmt block = new BlockStmt();
            block.addStatement(body);
            n.setBody(block);            
            addBlock(body);
            modified = true;
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(IfStmt n, Void arg) {
        Statement thenStmt = n.getThenStmt();
        if (addBlocks && !thenStmt.isBlockStmt()) {
            BlockStmt block = new BlockStmt();
            block.addStatement(thenStmt);
            n.setThenStmt(block);            
            addBlock(thenStmt);
            modified = true;
        }
        Statement elseStmt = n.getElseStmt().orElse(null);
        if (addBlocks && elseStmt != null && !elseStmt.isBlockStmt()) {
            BlockStmt block = new BlockStmt();
            block.addStatement(elseStmt);
            n.setElseStmt(block);            
            addBlock(elseStmt);
            modified = true;
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(NormalAnnotationExpr n, Void arg) {
        if (ignoreAnnotations) {
            deleteTokens(n);
            return;
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(MarkerAnnotationExpr n, Void arg) {
        if (ignoreAnnotations) {
            deleteTokens(n);
            return;
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(SingleMemberAnnotationExpr n, Void arg) {
        if (ignoreAnnotations) {
            deleteTokens(n);
            return;
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(UnaryExpr n, Void arg) {
        Expression expr = n.getExpression();
        if (ignoreUnaryAtLiterals && expr.isLiteralExpr()) {
            TokenRange range = n.getTokenRange().get();
            JavaToken tok = range.getBegin();
            JavaToken lit = range.getEnd();
            lit.setText(tok.getText() + lit.getText());
            deleteToken(tok);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(WhileStmt n, Void arg) {
        Statement body = n.getBody();
        if (addBlocks && !body.isBlockStmt()) {
            BlockStmt block = new BlockStmt();
            block.addStatement(body);
            n.setBody(block);            
            addBlock(body);
            modified = true;
        }
        super.visit(n, arg);
    }
}
