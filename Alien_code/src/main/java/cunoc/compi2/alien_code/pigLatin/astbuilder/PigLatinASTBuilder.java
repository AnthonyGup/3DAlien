package cunoc.compi2.alien_code.pigLatin.astbuilder;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import cunoc.compi2.alien_code.ast.program.ImportNode;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.expr.*;
import cunoc.compi2.alien_code.ast.stmt.*;
import cunoc.compi2.alien_code.pigLatin.grammar.PigLatinBaseVisitor;
import cunoc.compi2.alien_code.pigLatin.grammar.PigLatinLexer;
import cunoc.compi2.alien_code.pigLatin.grammar.PigLatinParser;

public class PigLatinASTBuilder extends PigLatinBaseVisitor<Node> {

    public ProgramNode construir(String codigo) {
        PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PigLatinParser parser = new PigLatinParser(tokens);
        return (ProgramNode) visitPrograma(parser.programa());
    }

    @Override
    public Node visitPrograma(PigLatinParser.ProgramaContext ctx) {
        ProgramNode program = new ProgramNode(line(ctx), column(ctx));
        program.sourceLanguage = "Pig Latin";
        List<Node> declarations = new ArrayList<>();
        for (PigLatinParser.ImportacionContext i : ctx.importacion()) {
            declarations.add(visitImportacion(i));
        }
        if (ctx.seccionVariables() != null) {
            for (PigLatinParser.DeclaracionContext d : ctx.seccionVariables().declaracion()) {
                declarations.add(visitDeclaracion(d));
            }
        }
        if (ctx.seccionPrincipal() != null) {
            for (PigLatinParser.SentenciaPrincipalContext s : ctx.seccionPrincipal().sentenciaPrincipal()) {
                declarations.add(visitSentenciaPrincipal(s));
            }
        }
        program.declarations = declarations;
        return program;
    }

    @Override
    public Node visitImportacion(PigLatinParser.ImportacionContext ctx) {
        StringBuilder path = new StringBuilder();
        List<org.antlr.v4.runtime.tree.TerminalNode> identifiers = ctx.rutaArchivo().IDENTIFICADOR();
        for (int i = 0; i < identifiers.size(); i++) {
            if (i > 0) path.append('.');
            path.append(identifiers.get(i).getText());
        }
        return new ImportNode(path.toString(), line(ctx), column(ctx));
    }

    @Override
    public Node visitSentenciaPrincipal(PigLatinParser.SentenciaPrincipalContext ctx) {
        if (ctx.sentencia() != null) return visitSentencia(ctx.sentencia());
        return visitCondicional(ctx.condicional());
    }

    @Override
    public Node visitDeclaracion(PigLatinParser.DeclaracionContext ctx) {
        if (ctx.declaracionVariable() != null) return visitDeclaracionVariable(ctx.declaracionVariable());
        return visitDeclaracionArreglo(ctx.declaracionArreglo());
    }

    @Override
    public Node visitDeclaracionVariable(PigLatinParser.DeclaracionVariableContext ctx) {
        String name = ctx.IDENTIFICADOR(0).getText();
        if (ctx.VERUM() != null || ctx.FALSUS() != null) {
            String value = ctx.VERUM() != null ? "true" : "false";
            LiteralNode initializer = new LiteralNode(LiteralNode.Clase.BOOLEANO, value, line(ctx), column(ctx));
            return new VariableDeclNode(name, Type.BOOL, null, initializer, line(ctx), column(ctx));
        }
        if (ctx.NOVUS() != null) {
            String className = ctx.IDENTIFICADOR(1).getText();
            NewObjectNode initializer = new NewObjectNode(className, argumentsFrom(ctx.argumentos()), line(ctx), column(ctx));
            return new VariableDeclNode(name, Type.STRUCT, className, initializer, line(ctx), column(ctx));
        }
        if (ctx.literalStructura() != null) {
            String className = ctx.IDENTIFICADOR(1).getText();
            Node initializer = visitLiteralStructura(ctx.literalStructura());
            return new VariableDeclNode(name, Type.STRUCT, className, initializer, line(ctx), column(ctx));
        }
        if (ctx.tipoPrimitivo() != null) {
            Type type = Type.fromPigLatin(ctx.tipoPrimitivo().getText());
            Node initializer = visit(ctx.expresion());
            return new VariableDeclNode(name, type, null, initializer, line(ctx), column(ctx));
        }
        String className = ctx.IDENTIFICADOR(1).getText();
        return new VariableDeclNode(name, Type.STRUCT, className, null, line(ctx), column(ctx));
    }

    @Override
    public Node visitDeclaracionArreglo(PigLatinParser.DeclaracionArregloContext ctx) {
        String name = ctx.IDENTIFICADOR().getText();
        int size = Integer.parseInt(ctx.NUMERO().getText());
        PigLatinParser.TipoContext context = ctx.tipo();
        Type type;
        String typeName = null;
        if (context.tipoPrimitivo() != null) {
            type = Type.fromPigLatin(context.tipoPrimitivo().getText());
        } else {
            type = Type.STRUCT;
            typeName = context.IDENTIFICADOR().getText();
        }
        List<Node> initializers = new ArrayList<>();
        if (ctx.inicializadorArreglo() != null) {
            for (PigLatinParser.ExpresionContext e : ctx.inicializadorArreglo().expresion()) {
                initializers.add(visit(e));
            }
        }
        return new ArrayDeclNode(name, size, type, typeName, initializers, line(ctx), column(ctx));
    }

    @Override
    public Node visitLiteralStructura(PigLatinParser.LiteralStructuraContext ctx) {
        List<Node> values = new ArrayList<>();
        for (PigLatinParser.ValorAtributoContext v : ctx.valorAtributo()) {
            if (v.literalStructura() != null) {
                values.add(visitLiteralStructura(v.literalStructura()));
            } else {
                values.add(visit(v.expresion()));
            }
        }
        return new StructLiteralNode(values, line(ctx), column(ctx));
    }

    @Override
    public Node visitSentencia(PigLatinParser.SentenciaContext ctx) {
        if (ctx.asignacion() != null) return visitAsignacion(ctx.asignacion());
        if (ctx.incremento() != null) return visitIncremento(ctx.incremento());
        if (ctx.decremento() != null) return visitDecremento(ctx.decremento());
        if (ctx.ciclo() != null) return visitCiclo(ctx.ciclo());
        if (ctx.perge() != null) return visitPerge(ctx.perge());
        if (ctx.interrumpe() != null) return visitInterrumpe(ctx.interrumpe());
        if (ctx.accesoVariable() != null) return visitAccesoVariable(ctx.accesoVariable());
        if (ctx.leer() != null) return visitLeer(ctx.leer());
        return visitImprimir(ctx.imprimir());
    }

    @Override
    public Node visitLeer(PigLatinParser.LeerContext ctx) {
        AccessNode target = null;
        if (ctx.accesoVariable() != null) {
            target = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        }
        return new ReadNode(target, line(ctx), column(ctx));
    }

    @Override
    public Node visitImprimir(PigLatinParser.ImprimirContext ctx) {
        List<Node> expressions = new ArrayList<>();
        for (PigLatinParser.ExpresionContext e : ctx.expresion()) {
            expressions.add(visit(e));
        }
        return new PrintNode(expressions, line(ctx), column(ctx));
    }

    @Override
    public Node visitCiclo(PigLatinParser.CicloContext ctx) {
        if (ctx.dum() != null) return visitDum(ctx.dum());
        if (ctx.facere() != null) return visitFacere(ctx.facere());
        return visitPer(ctx.per());
    }

    @Override
    public Node visitDum(PigLatinParser.DumContext ctx) {
        Node condition = visit(ctx.expresion());
        BlockNode body = (BlockNode) visitCuerpoBloque(ctx.cuerpoBloque());
        return new WhileNode(condition, body, line(ctx), column(ctx));
    }

    @Override
    public Node visitFacere(PigLatinParser.FacereContext ctx) {
        BlockNode body = (BlockNode) visitCuerpoBloque(ctx.cuerpoBloque());
        Node condition = visit(ctx.expresion());
        return new DoWhileNode(body, condition, line(ctx), column(ctx));
    }

    @Override
    public Node visitPer(PigLatinParser.PerContext ctx) {
        Node initializer = visitInicializadorCiclo(ctx.inicializadorCiclo());
        Node condition = visit(ctx.expresion());
        Node step = visitPasoCiclo(ctx.pasoCiclo());
        BlockNode body = (BlockNode) visitCuerpoBloque(ctx.cuerpoBloque());
        return new ForNode(initializer, condition, step, body, line(ctx), column(ctx));
    }

    @Override
    public Node visitInicializadorCiclo(PigLatinParser.InicializadorCicloContext ctx) {
        String name = ctx.IDENTIFICADOR().getText();
        Type type = Type.fromPigLatin(ctx.tipoPrimitivo().getText());
        Node initializer = visit(ctx.expresion());
        return new VariableDeclNode(name, type, null, initializer, line(ctx), column(ctx));
    }

    @Override
    public Node visitPasoCiclo(PigLatinParser.PasoCicloContext ctx) {
        AccessNode target = new AccessNode(ctx.IDENTIFICADOR().getText(), line(ctx), column(ctx));
        if (ctx.INCREMENTO() != null) return new IncrementNode(target, line(ctx), column(ctx));
        return new DecrementNode(target, line(ctx), column(ctx));
    }

    @Override
    public Node visitPerge(PigLatinParser.PergeContext ctx) {
        return new ContinueNode(line(ctx), column(ctx));
    }

    @Override
    public Node visitInterrumpe(PigLatinParser.InterrumpeContext ctx) {
        return new BreakNode(line(ctx), column(ctx));
    }

    @Override
    public Node visitCondicional(PigLatinParser.CondicionalContext ctx) {
        Node condition = visit(ctx.expresion());
        BlockNode body = (BlockNode) visitCuerpoBloque(ctx.cuerpoBloque());
        List<ElseIfNode> branches = new ArrayList<>();
        for (PigLatinParser.AliterContext a : ctx.aliter()) {
            branches.add((ElseIfNode) visitAliter(a));
        }
        return new IfNode(condition, body, branches, line(ctx), column(ctx));
    }

    @Override
    public Node visitAliter(PigLatinParser.AliterContext ctx) {
        Node condition = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        BlockNode body = (BlockNode) visitCuerpoBloque(ctx.cuerpoBloque());
        return new ElseIfNode(condition, body, line(ctx), column(ctx));
    }

    @Override
    public Node visitCuerpoBloque(PigLatinParser.CuerpoBloqueContext ctx) {
        BlockNode block = new BlockNode(line(ctx), column(ctx));
        for (PigLatinParser.SentenciaPrincipalContext s : ctx.sentenciaPrincipal()) {
            block.sentencias.add(visitSentenciaPrincipal(s));
        }
        return block;
    }

    @Override
    public Node visitAsignacion(PigLatinParser.AsignacionContext ctx) {
        AccessNode target = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        Node value = visit(ctx.expresion());
        return new AssignmentNode(target, value, line(ctx), column(ctx));
    }

    @Override
    public Node visitIncremento(PigLatinParser.IncrementoContext ctx) {
        AccessNode target = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        return new IncrementNode(target, line(ctx), column(ctx));
    }

    @Override
    public Node visitDecremento(PigLatinParser.DecrementoContext ctx) {
        AccessNode target = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        return new DecrementNode(target, line(ctx), column(ctx));
    }

    @Override
    public Node visitExpresion(PigLatinParser.ExpresionContext ctx) {
        return visit(ctx.expresionOr());
    }

    @Override
    public Node visitExpresionOr(PigLatinParser.ExpresionOrContext ctx) {
        Node result = visit(ctx.expresionAnd(0));
        for (int i = 0; i < ctx.OR().size(); i++) {
            result = new BinaryOpNode("||", result, visit(ctx.expresionAnd(i + 1)), line(ctx), column(ctx));
        }
        return result;
    }

    @Override
    public Node visitExpresionAnd(PigLatinParser.ExpresionAndContext ctx) {
        Node result = visit(ctx.expresionIgualdad(0));
        for (int i = 0; i < ctx.AND().size(); i++) {
            result = new BinaryOpNode("&&", result, visit(ctx.expresionIgualdad(i + 1)), line(ctx), column(ctx));
        }
        return result;
    }

    @Override
    public Node visitExpresionIgualdad(PigLatinParser.ExpresionIgualdadContext ctx) {
        Node result = visit(ctx.expresionRelacional(0));
        List<org.antlr.v4.runtime.tree.TerminalNode> operators = new ArrayList<>();
        operators.addAll(ctx.IGUAL());
        operators.addAll(ctx.DISTINTO());
        List<PigLatinParser.ExpresionRelacionalContext> sides = ctx.expresionRelacional();
        for (int i = 0; i < operators.size(); i++) {
            result = new BinaryOpNode(operators.get(i).getText(), result, visit(sides.get(i + 1)), line(ctx), column(ctx));
        }
        return result;
    }

    @Override
    public Node visitExpresionRelacional(PigLatinParser.ExpresionRelacionalContext ctx) {
        Node result = visit(ctx.expresionAditiva(0));
        List<org.antlr.v4.runtime.tree.TerminalNode> operators = new ArrayList<>();
        operators.addAll(ctx.MENOR());
        operators.addAll(ctx.MAYOR());
        operators.addAll(ctx.MENOR_IGUAL());
        operators.addAll(ctx.MAYOR_IGUAL());
        List<PigLatinParser.ExpresionAditivaContext> sides = ctx.expresionAditiva();
        for (int i = 0; i < operators.size(); i++) {
            result = new BinaryOpNode(operators.get(i).getText(), result, visit(sides.get(i + 1)), line(ctx), column(ctx));
        }
        return result;
    }

    @Override
    public Node visitExpresionAditiva(PigLatinParser.ExpresionAditivaContext ctx) {
        Node result = visit(ctx.expresionMultiplicativa(0));
        List<org.antlr.v4.runtime.tree.TerminalNode> operators = new ArrayList<>();
        operators.addAll(ctx.MAS());
        operators.addAll(ctx.MENOS());
        List<PigLatinParser.ExpresionMultiplicativaContext> sides = ctx.expresionMultiplicativa();
        for (int i = 0; i < operators.size(); i++) {
            result = new BinaryOpNode(operators.get(i).getText(), result, visit(sides.get(i + 1)), line(ctx), column(ctx));
        }
        return result;
    }

    @Override
    public Node visitExpresionMultiplicativa(PigLatinParser.ExpresionMultiplicativaContext ctx) {
        Node result = visit(ctx.expresionUnaria(0));
        List<org.antlr.v4.runtime.tree.TerminalNode> operators = new ArrayList<>();
        operators.addAll(ctx.POR());
        operators.addAll(ctx.DIV());
        List<PigLatinParser.ExpresionUnariaContext> sides = ctx.expresionUnaria();
        for (int i = 0; i < operators.size(); i++) {
            result = new BinaryOpNode(operators.get(i).getText(), result, visit(sides.get(i + 1)), line(ctx), column(ctx));
        }
        return result;
    }

    @Override
    public Node visitExpresionUnaria(PigLatinParser.ExpresionUnariaContext ctx) {
        if (ctx.NON() != null) {
            return new UnaryOpNode("non", visit(ctx.expresionUnaria()), line(ctx), column(ctx));
        }
        if (ctx.MENOS() != null) {
            return new UnaryOpNode("-", visit(ctx.expresionUnaria()), line(ctx), column(ctx));
        }
        return visit(ctx.factor());
    }

    @Override
    public Node visitAccesoVariable(PigLatinParser.AccesoVariableContext ctx) {
        AccessNode access = new AccessNode(ctx.IDENTIFICADOR().getText(), line(ctx), column(ctx));
        for (PigLatinParser.SufijoAccesoContext s : ctx.sufijoAcceso()) {
            if (s.PAREN_IZQ() != null) {
                access.sufijos.add(AccessNode.Sufijo.llamada(argumentsFrom(s.argumentos())));
            } else if (s.CORCHETE_IZQ() != null) {
                access.sufijos.add(AccessNode.Sufijo.indice(visit(s.expresion())));
            } else {
                access.sufijos.add(AccessNode.Sufijo.campo(s.IDENTIFICADOR().getText()));
            }
        }
        return access;
    }

    @Override
    public Node visitFactor(PigLatinParser.FactorContext ctx) {
        if (ctx.NUMERO() != null) {
            return new LiteralNode(LiteralNode.Clase.ENTERO, ctx.NUMERO().getText(), line(ctx), column(ctx));
        }
        if (ctx.DECIMAL() != null) {
            return new LiteralNode(LiteralNode.Clase.DECIMAL, ctx.DECIMAL().getText(), line(ctx), column(ctx));
        }
        if (ctx.CADENA() != null) {
            return new LiteralNode(LiteralNode.Clase.CADENA, ctx.CADENA().getText(), line(ctx), column(ctx));
        }
        if (ctx.CARACTER() != null) {
            return new LiteralNode(LiteralNode.Clase.CARACTER, ctx.CARACTER().getText(), line(ctx), column(ctx));
        }
        if (ctx.VERUM() != null) {
            return new LiteralNode(LiteralNode.Clase.BOOLEANO, "true", line(ctx), column(ctx));
        }
        if (ctx.FALSUS() != null) {
            return new LiteralNode(LiteralNode.Clase.BOOLEANO, "false", line(ctx), column(ctx));
        }
        if (ctx.NOVUS() != null) {
            String className = ctx.IDENTIFICADOR().getText();
            return new NewObjectNode(className, argumentsFrom(ctx.argumentos()), line(ctx), column(ctx));
        }
        if (ctx.accesoVariable() != null) {
            return visitAccesoVariable(ctx.accesoVariable());
        }
        if (ctx.literalStructura() != null) {
            return visitLiteralStructura(ctx.literalStructura());
        }
        return visit(ctx.expresion());
    }

    private List<Node> argumentsFrom(PigLatinParser.ArgumentosContext ctx) {
        List<Node> arguments = new ArrayList<>();
        if (ctx != null) {
            for (PigLatinParser.ExpresionContext e : ctx.expresion()) {
                arguments.add(visit(e));
            }
        }
        return arguments;
    }

    private int line(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    private int column(ParserRuleContext ctx) {
        return ctx.getStart().getCharPositionInLine() + 1;
    }
}