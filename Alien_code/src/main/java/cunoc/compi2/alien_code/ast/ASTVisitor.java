package cunoc.compi2.alien_code.ast;

import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.ast.expr.BinaryOpNode;
import cunoc.compi2.alien_code.ast.expr.LiteralNode;
import cunoc.compi2.alien_code.ast.expr.NewObjectNode;
import cunoc.compi2.alien_code.ast.expr.StructLiteralNode;
import cunoc.compi2.alien_code.ast.expr.UnaryOpNode;
import cunoc.compi2.alien_code.ast.program.ImportNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.stmt.ArrayDeclNode;
import cunoc.compi2.alien_code.ast.stmt.AssignmentNode;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.ast.stmt.BreakNode;
import cunoc.compi2.alien_code.ast.stmt.ContinueNode;
import cunoc.compi2.alien_code.ast.stmt.DecrementNode;
import cunoc.compi2.alien_code.ast.stmt.DoWhileNode;
import cunoc.compi2.alien_code.ast.stmt.ElseIfNode;
import cunoc.compi2.alien_code.ast.stmt.ForNode;
import cunoc.compi2.alien_code.ast.stmt.IfNode;
import cunoc.compi2.alien_code.ast.stmt.IncrementNode;
import cunoc.compi2.alien_code.ast.stmt.PrintNode;
import cunoc.compi2.alien_code.ast.stmt.ReadNode;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.ast.stmt.WhileNode;

public interface ASTVisitor<T> {
    T visitProgram(ProgramNode node);

    default T visitImport(ImportNode node) { return null; }
    default T visitVariableDecl(VariableDeclNode node) { return null; }
    default T visitArrayDecl(ArrayDeclNode node) { return null; }
    default T visitStructLiteral(StructLiteralNode node) { return null; }
    default T visitBlock(BlockNode node) { return null; }
    default T visitAccess(AccessNode node) { return null; }
    default T visitNewObject(NewObjectNode node) { return null; }

    default T visitAssignment(AssignmentNode node) { return null; }
    default T visitIncrement(IncrementNode node) { return null; }
    default T visitDecrement(DecrementNode node) { return null; }
    default T visitRead(ReadNode node) { return null; }
    default T visitPrint(PrintNode node) { return null; }
    default T visitWhile(WhileNode node) { return null; }
    default T visitDoWhile(DoWhileNode node) { return null; }
    default T visitFor(ForNode node) { return null; }
    default T visitContinue(ContinueNode node) { return null; }
    default T visitBreak(BreakNode node) { return null; }
    default T visitIf(IfNode node) { return null; }
    default T visitElseIf(ElseIfNode node) { return null; }

    default T visitLiteral(LiteralNode node) { return null; }
    default T visitBinaryOp(BinaryOpNode node) { return null; }
    default T visitUnaryOp(UnaryOpNode node) { return null; }
}