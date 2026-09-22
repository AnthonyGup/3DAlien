package cunoc.compi2.alien_code.zetariano.astbuilder;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.ClassDeclNode;
import cunoc.compi2.alien_code.ast.decl.ConstructorDeclNode;
import cunoc.compi2.alien_code.ast.decl.MethodDeclNode;
import cunoc.compi2.alien_code.ast.decl.ParameterNode;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.ast.expr.BinaryOpNode;
import cunoc.compi2.alien_code.ast.expr.ConditionalNode;
import cunoc.compi2.alien_code.ast.expr.LiteralNode;
import cunoc.compi2.alien_code.ast.expr.NewArrayNode;
import cunoc.compi2.alien_code.ast.expr.NewObjectNode;
import cunoc.compi2.alien_code.ast.expr.StructLiteralNode;
import cunoc.compi2.alien_code.ast.expr.UnaryOpNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.stmt.AssignmentNode;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.ast.stmt.BreakNode;
import cunoc.compi2.alien_code.ast.stmt.CaseNode;
import cunoc.compi2.alien_code.ast.stmt.ContinueNode;
import cunoc.compi2.alien_code.ast.stmt.DecrementNode;
import cunoc.compi2.alien_code.ast.stmt.DoWhileNode;
import cunoc.compi2.alien_code.ast.stmt.ElseIfNode;
import cunoc.compi2.alien_code.ast.stmt.ForNode;
import cunoc.compi2.alien_code.ast.stmt.IfNode;
import cunoc.compi2.alien_code.ast.stmt.IncrementNode;
import cunoc.compi2.alien_code.ast.stmt.ReturnNode;
import cunoc.compi2.alien_code.ast.stmt.SwitchNode;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.ast.stmt.WhileNode;
import cunoc.compi2.alien_code.zetariano.grammar.ZetarianoBaseVisitor;
import cunoc.compi2.alien_code.zetariano.grammar.ZetarianoLexer;
import cunoc.compi2.alien_code.zetariano.grammar.ZetarianoParser;

public class ZetarianoASTBuilder extends ZetarianoBaseVisitor<Node> {

    public ProgramNode construir(String codigo) {
        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ZetarianoParser parser = new ZetarianoParser(tokens);
        return (ProgramNode) visitPrograma(parser.programa());
    }

    @Override
    public Node visitPrograma(ZetarianoParser.ProgramaContext ctx) {
        String nombreClase = ctx.IDENTIFICADOR().getText();
        List<VariableDeclNode> atributos = new ArrayList<>();
        List<ConstructorDeclNode> constructores = new ArrayList<>();
        List<MethodDeclNode> metodos = new ArrayList<>();
        for (ZetarianoParser.MiembroClaseContext m : ctx.miembroClase()) {
            if (m.campoAtributo() != null) {
                atributos.add(visitCampoAtributo(m.campoAtributo()));
            } else if (m.constructor() != null) {
                constructores.add(visitConstructor(m.constructor()));
            } else {
                metodos.add((MethodDeclNode) visitMetodo(m.metodo()));
            }
        }
        ProgramNode program = new ProgramNode(line(ctx), column(ctx));
        program.sourceLanguage = "Zetariano";
        List<Node> declarations = new ArrayList<>();
        declarations.add(new ClassDeclNode(nombreClase, atributos, constructores, metodos,
                line(ctx), column(ctx)));
        program.declarations = declarations;
        return program;
    }

    public VariableDeclNode visitCampoAtributo(ZetarianoParser.CampoAtributoContext ctx) {
        TipoZ tipo = resolverTipo(ctx.tipo(), contarDims(ctx.arrayDims()));
        String nombre = ctx.IDENTIFICADOR().getText();
        Node inicial = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        return declararVariable(nombre, tipo, inicial, ctx);
    }

    public ConstructorDeclNode visitConstructor(ZetarianoParser.ConstructorContext ctx) {
        return new ConstructorDeclNode(parametrosDe(ctx.parametros()), bloqueDe(ctx.bloque()),
                line(ctx), column(ctx));
    }

    @Override
    public Node visitMetodo(ZetarianoParser.MetodoContext ctx) {
        List<ParameterNode> parametros = parametrosDe(ctx.parametros());
        Type tipoRetorno;
        String tipoRetornoNombre = null;
        Type tipoRetornoElemento = null;
        if (ctx.VOID() != null) {
            tipoRetorno = Type.VOID;
        } else {
            TipoZ tipo = resolverTipo(ctx.tipo(), contarDims(ctx.arrayDims()));
            if (tipo.esArreglo) {
                tipoRetorno = Type.ARRAY;
                tipoRetornoElemento = tipo.tipo;
                tipoRetornoNombre = tipo.tipoNombre;
            } else {
                tipoRetorno = tipo.tipo;
                tipoRetornoNombre = tipo.tipoNombre;
            }
        }
        MethodDeclNode metodo = new MethodDeclNode(ctx.IDENTIFICADOR().getText(), parametros,
                tipoRetorno, bloqueDe(ctx.bloque()), line(ctx), column(ctx));
        metodo.tipoRetornoNombre = tipoRetornoNombre;
        metodo.tipoRetornoElemento = tipoRetornoElemento;
        return metodo;
    }

    private List<ParameterNode> parametrosDe(ZetarianoParser.ParametrosContext ctx) {
        List<ParameterNode> parametros = new ArrayList<>();
        if (ctx != null) {
            for (ZetarianoParser.ParametroContext p : ctx.parametro()) {
                parametros.add(visitParametro(p));
            }
        }
        return parametros;
    }

    public ParameterNode visitParametro(ZetarianoParser.ParametroContext ctx) {
        TipoZ tipo = resolverTipo(ctx.tipo(), contarDims(ctx.arrayDims()));
        if (tipo.esArreglo) {
            ParameterNode parametro = new ParameterNode(Type.ARRAY, ctx.IDENTIFICADOR().getText(),
                    false, tipo.tipoNombre, line(ctx), column(ctx));
            parametro.tipoElemento = tipo.tipo;
            parametro.dimensiones = tipo.dims;
            return parametro;
        }
        return new ParameterNode(tipo.tipo, ctx.IDENTIFICADOR().getText(), false,
                tipo.tipoNombre, line(ctx), column(ctx));
    }

    @Override
    public Node visitSentencia(ZetarianoParser.SentenciaContext ctx) {
        if (ctx.declaracionVariable() != null) return visitDeclaracionVariable(ctx.declaracionVariable());
        if (ctx.asignacion() != null) return visitAsignacion(ctx.asignacion());
        if (ctx.asignacionCompuesta() != null) return visitAsignacionCompuesta(ctx.asignacionCompuesta());
        if (ctx.incremento() != null) return visitIncremento(ctx.incremento());
        if (ctx.decremento() != null) return visitDecremento(ctx.decremento());
        if (ctx.condicional() != null) return visitCondicional(ctx.condicional());
        if (ctx.seleccion() != null) return visitSeleccion(ctx.seleccion());
        if (ctx.forClasico() != null) return visitForClasico(ctx.forClasico());
        if (ctx.whileClasico() != null) return visitWhileClasico(ctx.whileClasico());
        if (ctx.doWhileClasico() != null) return visitDoWhileClasico(ctx.doWhileClasico());
        if (ctx.retorno() != null) return visitRetorno(ctx.retorno());
        if (ctx.BREAK() != null) return new BreakNode(line(ctx), column(ctx));
        if (ctx.CONTINUE() != null) return new ContinueNode(line(ctx), column(ctx));
        if (ctx.bloque() != null) return bloqueDe(ctx.bloque());
        return visitAccesoVariable(ctx.accesoVariable());
    }

    @Override
    public Node visitDeclaracionVariable(ZetarianoParser.DeclaracionVariableContext ctx) {
        TipoZ tipo = resolverTipo(ctx.tipo(), contarDims(ctx.arrayDims()));
        String nombre = ctx.IDENTIFICADOR().getText();
        Node inicial = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        return declararVariable(nombre, tipo, inicial, ctx);
    }

    private VariableDeclNode declararVariable(String nombre, TipoZ tipo, Node inicial,
            ParserRuleContext ctx) {
        if (!tipo.esArreglo) {
            return new VariableDeclNode(nombre, tipo.tipo, tipo.tipoNombre, inicial,
                    line(ctx), column(ctx));
        }
        VariableDeclNode arreglo = new VariableDeclNode(nombre, Type.ARRAY, tipo.tipoNombre, inicial,
                line(ctx), column(ctx));
        arreglo.tipoElemento = tipo.tipo;
        arreglo.dimensiones = tipo.dims;
        return arreglo;
    }

    @Override
    public Node visitAsignacion(ZetarianoParser.AsignacionContext ctx) {
        AccessNode destino = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        Node valor = visit(ctx.expresion());
        return new AssignmentNode(destino, valor, line(ctx), column(ctx));
    }

    @Override
    public Node visitAsignacionCompuesta(ZetarianoParser.AsignacionCompuestaContext ctx) {
        AccessNode destino = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        String operador = ctx.MAS_ASIGNA() != null ? "+"
                : ctx.MENOS_ASIGNA() != null ? "-" : "*";
        AccessNode lectura = new AccessNode(destino.nombre, line(ctx), column(ctx));
        lectura.sufijos.addAll(destino.sufijos);
        Node valor = new BinaryOpNode(operador, lectura, visit(ctx.expresion()),
                line(ctx), column(ctx));
        return new AssignmentNode(destino, valor, line(ctx), column(ctx));
    }

    @Override
    public Node visitIncremento(ZetarianoParser.IncrementoContext ctx) {
        AccessNode objetivo = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        return new IncrementNode(objetivo, line(ctx), column(ctx));
    }

    @Override
    public Node visitDecremento(ZetarianoParser.DecrementoContext ctx) {
        AccessNode objetivo = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        return new DecrementNode(objetivo, line(ctx), column(ctx));
    }

    @Override
    public Node visitRetorno(ZetarianoParser.RetornoContext ctx) {
        Node expresion = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        return new ReturnNode(expresion, line(ctx), column(ctx));
    }

    @Override
    public Node visitCondicional(ZetarianoParser.CondicionalContext ctx) {
        Node condicion = visit(ctx.expresion());
        BlockNode cuerpo = cuerpoOSentencia(ctx.cuerpoOSentencia());
        List<ElseIfNode> ramas = new ArrayList<>();
        aplanarRamaElse(ctx.ramaElse(), ramas);
        return new IfNode(condicion, cuerpo, ramas, line(ctx), column(ctx));
    }

    private void aplanarRamaElse(ZetarianoParser.RamaElseContext ctx, List<ElseIfNode> ramas) {
        if (ctx == null) {
            return;
        }
        if (ctx.condicional() != null) {
            ZetarianoParser.CondicionalContext anidado = ctx.condicional();
            ramas.add(new ElseIfNode(visit(anidado.expresion()),
                    cuerpoOSentencia(anidado.cuerpoOSentencia()), line(anidado), column(anidado)));
            aplanarRamaElse(anidado.ramaElse(), ramas);
        } else {
            ramas.add(new ElseIfNode(null, cuerpoOSentencia(ctx.cuerpoOSentencia()),
                    line(ctx), column(ctx)));
        }
    }

    @Override
    public Node visitSeleccion(ZetarianoParser.SeleccionContext ctx) {
        Node expresion = visit(ctx.expresion());
        List<CaseNode> casos = new ArrayList<>();
        for (ZetarianoParser.CasoBloqueContext caso : ctx.casoBloque()) {
            Node valor = caso.CASE() != null ? visit(caso.expresion()) : null;
            BlockNode cuerpo = new BlockNode(line(caso), column(caso));
            for (ZetarianoParser.SentenciaContext s : caso.sentencia()) {
                cuerpo.sentencias.add(visitSentencia(s));
            }
            casos.add(new CaseNode(valor, cuerpo.sentencias, line(caso), column(caso)));
        }
        return new SwitchNode(expresion, casos, line(ctx), column(ctx));
    }

    @Override
    public Node visitForClasico(ZetarianoParser.ForClasicoContext ctx) {
        Node inicio = ctx.forInit() != null ? visitarForInit(ctx.forInit()) : null;
        Node condicion = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        Node actualizacion = ctx.forUpdate() != null ? visitarForUpdate(ctx.forUpdate()) : null;
        return new ForNode(inicio, condicion, actualizacion,
                cuerpoOSentencia(ctx.cuerpoOSentencia()), line(ctx), column(ctx));
    }

    private Node visitarForInit(ZetarianoParser.ForInitContext ctx) {
        if (ctx.tipo() != null) {
            TipoZ tipo = resolverTipo(ctx.tipo(), contarDims(ctx.arrayDims()));
            Node inicial = visit(ctx.expresion());
            return declararVariable(ctx.IDENTIFICADOR().getText(), tipo, inicial, ctx);
        }
        AccessNode destino = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        return new AssignmentNode(destino, visit(ctx.expresion()), line(ctx), column(ctx));
    }

    private Node visitarForUpdate(ZetarianoParser.ForUpdateContext ctx) {
        AccessNode objetivo = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        if (ctx.INCREMENTO() != null) {
            return new IncrementNode(objetivo, line(ctx), column(ctx));
        }
        if (ctx.DECREMENTO() != null) {
            return new DecrementNode(objetivo, line(ctx), column(ctx));
        }
        return new AssignmentNode(objetivo, visit(ctx.expresion()), line(ctx), column(ctx));
    }

    @Override
    public Node visitWhileClasico(ZetarianoParser.WhileClasicoContext ctx) {
        return new WhileNode(visit(ctx.expresion()), cuerpoOSentencia(ctx.cuerpoOSentencia()),
                line(ctx), column(ctx));
    }

    @Override
    public Node visitDoWhileClasico(ZetarianoParser.DoWhileClasicoContext ctx) {
        return new DoWhileNode(cuerpoOSentencia(ctx.cuerpoOSentencia()), visit(ctx.expresion()),
                line(ctx), column(ctx));
    }

    @Override
    public Node visitExpresion(ZetarianoParser.ExpresionContext ctx) {
        Node base = visit(ctx.expresionOr());
        if (ctx.INTERROGACION() != null) {
            return new ConditionalNode(base, visit(ctx.expresion(0)), visit(ctx.expresion(1)),
                    line(ctx), column(ctx));
        }
        return base;
    }

    @Override
    public Node visitExpresionOr(ZetarianoParser.ExpresionOrContext ctx) {
        Node resultado = visit(ctx.expresionAnd(0));
        for (int i = 0; i < ctx.OR().size(); i++) {
            resultado = new BinaryOpNode(ctx.OR(i).getText(), resultado,
                    visit(ctx.expresionAnd(i + 1)), line(ctx), column(ctx));
        }
        return resultado;
    }

    @Override
    public Node visitExpresionAnd(ZetarianoParser.ExpresionAndContext ctx) {
        Node resultado = visit(ctx.expresionIgualdad(0));
        for (int i = 0; i < ctx.AND().size(); i++) {
            resultado = new BinaryOpNode(ctx.AND(i).getText(), resultado,
                    visit(ctx.expresionIgualdad(i + 1)), line(ctx), column(ctx));
        }
        return resultado;
    }

    @Override
    public Node visitExpresionIgualdad(ZetarianoParser.ExpresionIgualdadContext ctx) {
        Node resultado = visit(ctx.expresionRelacional(0));
        List<ZetarianoParser.ExpresionRelacionalContext> lados = ctx.expresionRelacional();
        int lado = 1;
        for (ParseTree hijo : ctx.children) {
            if (hijo instanceof TerminalNode terminal) {
                resultado = new BinaryOpNode(terminal.getText(), resultado,
                        visit(lados.get(lado++)), line(ctx), column(ctx));
            }
        }
        return resultado;
    }

    @Override
    public Node visitExpresionRelacional(ZetarianoParser.ExpresionRelacionalContext ctx) {
        Node resultado = visit(ctx.expresionAditiva(0));
        List<ZetarianoParser.ExpresionAditivaContext> lados = ctx.expresionAditiva();
        int lado = 1;
        for (ParseTree hijo : ctx.children) {
            if (hijo instanceof TerminalNode terminal) {
                resultado = new BinaryOpNode(terminal.getText(), resultado,
                        visit(lados.get(lado++)), line(ctx), column(ctx));
            }
        }
        return resultado;
    }

    @Override
    public Node visitExpresionAditiva(ZetarianoParser.ExpresionAditivaContext ctx) {
        Node resultado = visit(ctx.expresionMultiplicativa(0));
        List<ZetarianoParser.ExpresionMultiplicativaContext> lados = ctx.expresionMultiplicativa();
        int lado = 1;
        for (ParseTree hijo : ctx.children) {
            if (hijo instanceof TerminalNode terminal) {
                resultado = new BinaryOpNode(terminal.getText(), resultado,
                        visit(lados.get(lado++)), line(ctx), column(ctx));
            }
        }
        return resultado;
    }

    @Override
    public Node visitExpresionMultiplicativa(ZetarianoParser.ExpresionMultiplicativaContext ctx) {
        Node resultado = visit(ctx.expresionUnaria(0));
        List<ZetarianoParser.ExpresionUnariaContext> lados = ctx.expresionUnaria();
        int lado = 1;
        for (ParseTree hijo : ctx.children) {
            if (hijo instanceof TerminalNode terminal) {
                resultado = new BinaryOpNode(terminal.getText(), resultado,
                        visit(lados.get(lado++)), line(ctx), column(ctx));
            }
        }
        return resultado;
    }

    @Override
    public Node visitExpresionUnaria(ZetarianoParser.ExpresionUnariaContext ctx) {
        if (ctx.NEGACION() != null) {
            return new UnaryOpNode(ctx.NEGACION().getText(), visit(ctx.expresionUnaria()), line(ctx), column(ctx));
        }
        if (ctx.MENOS() != null) {
            return new UnaryOpNode("-", visit(ctx.expresionUnaria()), line(ctx), column(ctx));
        }
        return visit(ctx.factor());
    }

    @Override
    public Node visitAccesoVariable(ZetarianoParser.AccesoVariableContext ctx) {
        AccessNode acceso = new AccessNode(ctx.IDENTIFICADOR().getText(), line(ctx), column(ctx));
        for (ZetarianoParser.SufijoAccesoContext s : ctx.sufijoAcceso()) {
            if (s.PAREN_IZQ() != null) {
                acceso.sufijos.add(AccessNode.Sufijo.llamada(argumentosDe(s.argumentos())));
            } else if (s.CORCHETE_IZQ() != null) {
                acceso.sufijos.add(AccessNode.Sufijo.indice(visit(s.expresion())));
            } else {
                acceso.sufijos.add(AccessNode.Sufijo.campo(s.IDENTIFICADOR().getText()));
            }
        }
        return acceso;
    }

    @Override
    public Node visitFactor(ZetarianoParser.FactorContext ctx) {
        if (ctx.NEW() != null) {
            return visitarNew(ctx);
        }
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
        if (ctx.TRUE() != null) {
            return new LiteralNode(LiteralNode.Clase.BOOLEANO, "true", line(ctx), column(ctx));
        }
        if (ctx.FALSE() != null) {
            return new LiteralNode(LiteralNode.Clase.BOOLEANO, "false", line(ctx), column(ctx));
        }
        if (ctx.NULL() != null) {
            return new LiteralNode(LiteralNode.Clase.NULO, "null", line(ctx), column(ctx));
        }
        if (ctx.accesoVariable() != null) {
            return visitAccesoVariable(ctx.accesoVariable());
        }
        if (ctx.literalArreglo() != null) {
            return visitLiteralArreglo(ctx.literalArreglo());
        }
        return visit(ctx.expresion(0));
    }

    private Node visitarNew(ZetarianoParser.FactorContext ctx) {
        if (ctx.IDENTIFICADOR() != null) {
            String nombreClase = ctx.IDENTIFICADOR().getText();
            if (ctx.PAREN_IZQ() != null) {
                return new NewObjectNode(nombreClase, argumentosDe(ctx.argumentos()),
                        line(ctx), column(ctx));
            }
            List<Node> dims = new ArrayList<>();
            for (ZetarianoParser.ExpresionContext e : ctx.expresion()) {
                dims.add(visit(e));
            }
            return new NewArrayNode(Type.CLASS, nombreClase, dims, line(ctx), column(ctx));
        }
        List<Node> dims = new ArrayList<>();
        for (ZetarianoParser.ExpresionContext e : ctx.expresion()) {
            dims.add(visit(e));
        }
        return new NewArrayNode(Type.fromZetariano(ctx.tipoBase().getText()), dims,
                line(ctx), column(ctx));
    }

    @Override
    public Node visitLiteralArreglo(ZetarianoParser.LiteralArregloContext ctx) {
        List<Node> valores = new ArrayList<>();
        for (ZetarianoParser.ExpresionContext e : ctx.expresion()) {
            valores.add(visit(e));
        }
        return new StructLiteralNode(valores, line(ctx), column(ctx));
    }

    private List<Node> argumentosDe(ZetarianoParser.ArgumentosContext ctx) {
        List<Node> argumentos = new ArrayList<>();
        if (ctx != null) {
            for (ZetarianoParser.ExpresionContext e : ctx.expresion()) {
                argumentos.add(visit(e));
            }
        }
        return argumentos;
    }

    private BlockNode bloqueDe(ZetarianoParser.BloqueContext ctx) {
        BlockNode bloque = new BlockNode(line(ctx), column(ctx));
        for (ZetarianoParser.SentenciaContext s : ctx.sentencia()) {
            bloque.sentencias.add(visitSentencia(s));
        }
        return bloque;
    }

    private BlockNode cuerpoOSentencia(ZetarianoParser.CuerpoOSentenciaContext ctx) {
        if (ctx.bloque() != null) {
            return bloqueDe(ctx.bloque());
        }
        BlockNode bloque = new BlockNode(line(ctx), column(ctx));
        bloque.sentencias.add(visitSentencia(ctx.sentencia()));
        return bloque;
    }

    private TipoZ resolverTipo(ZetarianoParser.TipoContext tipo, int dims) {
        TipoZ t = new TipoZ();
        if (tipo.tipoBase() != null) {
            t.tipo = Type.fromZetariano(tipo.tipoBase().getText());
        } else {
            t.tipo = Type.CLASS;
            t.tipoNombre = tipo.IDENTIFICADOR().getText();
        }
        t.esArreglo = dims > 0;
        t.dims = dims;
        return t;
    }

    private int contarDims(ZetarianoParser.ArrayDimsContext ctx) {
        return ctx == null ? 0 : ctx.CORCHETE_IZQ().size();
    }

    private static final class TipoZ {
        Type tipo;
        String tipoNombre;
        boolean esArreglo;
        int dims;
    }

    private int line(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    private int column(ParserRuleContext ctx) {
        return ctx.getStart().getCharPositionInLine() + 1;
    }
}
