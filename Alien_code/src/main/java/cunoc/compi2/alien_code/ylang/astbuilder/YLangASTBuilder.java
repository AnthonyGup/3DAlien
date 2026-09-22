package cunoc.compi2.alien_code.ylang.astbuilder;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.FunctionDeclNode;
import cunoc.compi2.alien_code.ast.decl.ParameterNode;
import cunoc.compi2.alien_code.ast.decl.StructDeclNode;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.ast.expr.BinaryOpNode;
import cunoc.compi2.alien_code.ast.expr.LiteralNode;
import cunoc.compi2.alien_code.ast.expr.StructLiteralNode;
import cunoc.compi2.alien_code.ast.expr.UnaryOpNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.stmt.ArrayDeclNode;
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
import cunoc.compi2.alien_code.ylang.grammar.YLangParserBaseVisitor;
import cunoc.compi2.alien_code.ylang.grammar.YLangLexer;
import cunoc.compi2.alien_code.ylang.grammar.YLangParser;

public class YLangASTBuilder extends YLangParserBaseVisitor<Node> {

    public ProgramNode construir(String codigo) {
        YLangLexer lexer = new YLangLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        YLangParser parser = new YLangParser(tokens);
        return (ProgramNode) visitPrograma(parser.programa());
    }

    @Override
    public Node visitPrograma(YLangParser.ProgramaContext ctx) {
        ProgramNode program = new ProgramNode(line(ctx), column(ctx));
        program.sourceLanguage = "Y?";
        List<Node> declarations = new ArrayList<>();
        if (ctx.seccionEstructuras() != null) {
            for (YLangParser.EstructuraContext e : ctx.seccionEstructuras().estructura()) {
                declarations.add(visitEstructura(e));
            }
        }
        if (ctx.seccionFunciones() != null) {
            for (YLangParser.FuncionContext f : ctx.seccionFunciones().funcion()) {
                declarations.add(visitFuncion(f));
            }
        }
        program.declarations = declarations;
        return program;
    }

    @Override
    public Node visitEstructura(YLangParser.EstructuraContext ctx) {
        List<VariableDeclNode> campos = new ArrayList<>();
        for (YLangParser.CampoEstructuraContext c : ctx.campoEstructura()) {
            campos.add(visitCampoEstructura(c));
        }
        return new StructDeclNode(ctx.IDENTIFICADOR().getText(), campos, line(ctx), column(ctx));
    }

    public VariableDeclNode visitCampoEstructura(YLangParser.CampoEstructuraContext ctx) {
        TipoY tipo = resolverTipo(ctx.tipoCampo().tipoPrimitivo(), ctx.tipoCampo().IDENTIFICADOR());
        String nombre = ctx.IDENTIFICADOR().getText();
        if (ctx.NUMERO().isEmpty()) {
            return new VariableDeclNode(nombre, tipo.tipo, tipo.tipoNombre, null, line(ctx), column(ctx));
        }
        VariableDeclNode campo = new VariableDeclNode(nombre, Type.ARRAY, tipo.tipoNombre, null,
                line(ctx), column(ctx));
        campo.tipoElemento = tipo.tipo;
        campo.dimensiones = ctx.NUMERO().size();
        return campo;
    }

    @Override
    public Node visitFuncion(YLangParser.FuncionContext ctx) {
        List<ParameterNode> parametros = new ArrayList<>();
        if (ctx.parametros() != null) {
            for (YLangParser.ParametroContext p : ctx.parametros().parametro()) {
                parametros.add(visitParametro(p));
            }
        }
        Type tipoRetorno = null;
        String tipoRetornoNombre = null;
        if (ctx.tipo() != null) {
            TipoY tipo = resolverTipo(ctx.tipo().tipoPrimitivo(), ctx.tipo().IDENTIFICADOR());
            tipoRetorno = tipo.tipo;
            tipoRetornoNombre = tipo.tipoNombre;
        }
        BlockNode cuerpo = bloque(ctx.sentencia(), ctx);
        FunctionDeclNode funcion = new FunctionDeclNode(ctx.IDENTIFICADOR().getText(), parametros,
                tipoRetorno, cuerpo, line(ctx), column(ctx));
        funcion.tipoRetornoNombre = tipoRetornoNombre;
        return funcion;
    }

    public ParameterNode visitParametro(YLangParser.ParametroContext ctx) {
        TipoY tipo = resolverTipo(ctx.tipo().tipoPrimitivo(), ctx.tipo().IDENTIFICADOR());
        boolean porReferencia = ctx.CORCHETES_VACIOS() != null || ctx.LLAVES_VACIAS() != null;
        if (ctx.CORCHETES_VACIOS() != null) {
            ParameterNode parametro = new ParameterNode(Type.ARRAY, ctx.IDENTIFICADOR().getText(),
                    porReferencia, tipo.tipoNombre, line(ctx), column(ctx));
            parametro.tipoElemento = tipo.tipo;
            return parametro;
        }
        return new ParameterNode(tipo.tipo, ctx.IDENTIFICADOR().getText(), porReferencia,
                tipo.tipoNombre, line(ctx), column(ctx));
    }

    @Override
    public Node visitSentencia(YLangParser.SentenciaContext ctx) {
        if (ctx.declaracionVariable() != null) return visitDeclaracionVariable(ctx.declaracionVariable());
        if (ctx.asignacion() != null) return visitAsignacion(ctx.asignacion());
        if (ctx.incremento() != null) return visitIncremento(ctx.incremento());
        if (ctx.decremento() != null) return visitDecremento(ctx.decremento());
        if (ctx.condicional() != null) return visitCondicional(ctx.condicional());
        if (ctx.seleccion() != null) return visitSeleccion(ctx.seleccion());
        if (ctx.paraCiclo() != null) return visitParaCiclo(ctx.paraCiclo());
        if (ctx.mientrasCiclo() != null) return visitMientrasCiclo(ctx.mientrasCiclo());
        if (ctx.hacerMientrasCiclo() != null) return visitHacerMientrasCiclo(ctx.hacerMientrasCiclo());
        if (ctx.retornoSentencia() != null) return visitRetornoSentencia(ctx.retornoSentencia());
        if (ctx.ROMPER() != null) return new BreakNode(line(ctx), column(ctx));
        if (ctx.CONTINUAR() != null) return new ContinueNode(line(ctx), column(ctx));
        return visitAccesoVariable(ctx.accesoVariable());
    }

    @Override
    public Node visitDeclaracionVariable(YLangParser.DeclaracionVariableContext ctx) {
        TipoY tipo = resolverTipo(ctx.tipo().tipoPrimitivo(), ctx.tipo().IDENTIFICADOR());
        String nombre = ctx.IDENTIFICADOR().getText();
        Node inicial = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        if (ctx.NUMERO().isEmpty()) {
            return new VariableDeclNode(nombre, tipo.tipo, tipo.tipoNombre, inicial, line(ctx), column(ctx));
        }
        int tamano = 1;
        for (TerminalNode numero : ctx.NUMERO()) {
            tamano *= Integer.parseInt(numero.getText());
        }
        if (inicial == null) {
            ArrayDeclNode arreglo = new ArrayDeclNode(nombre, tamano, tipo.tipo, tipo.tipoNombre,
                    new ArrayList<>(), line(ctx), column(ctx));
            arreglo.dimensiones = ctx.NUMERO().size();
            return arreglo;
        }
        if (inicial instanceof StructLiteralNode literal) {
            ArrayDeclNode arreglo = new ArrayDeclNode(nombre, tamano, tipo.tipo, tipo.tipoNombre,
                    new ArrayList<>(literal.valores), line(ctx), column(ctx));
            arreglo.dimensiones = ctx.NUMERO().size();
            return arreglo;
        }
        VariableDeclNode arreglo = new VariableDeclNode(nombre, Type.ARRAY, tipo.tipoNombre, inicial,
                line(ctx), column(ctx));
        arreglo.tipoElemento = tipo.tipo;
        arreglo.dimensiones = ctx.NUMERO().size();
        return arreglo;
    }

    @Override
    public Node visitAsignacion(YLangParser.AsignacionContext ctx) {
        AccessNode destino = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        Node valor = visit(ctx.expresion());
        return new AssignmentNode(destino, valor, line(ctx), column(ctx));
    }

    @Override
    public Node visitIncremento(YLangParser.IncrementoContext ctx) {
        AccessNode objetivo = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        return new IncrementNode(objetivo, line(ctx), column(ctx));
    }

    @Override
    public Node visitDecremento(YLangParser.DecrementoContext ctx) {
        AccessNode objetivo = (AccessNode) visitAccesoVariable(ctx.accesoVariable());
        return new DecrementNode(objetivo, line(ctx), column(ctx));
    }

    @Override
    public Node visitRetornoSentencia(YLangParser.RetornoSentenciaContext ctx) {
        return new ReturnNode(visit(ctx.expresion()), line(ctx), column(ctx));
    }

    @Override
    public Node visitCondicional(YLangParser.CondicionalContext ctx) {
        Node condicion = visit(ctx.expresion());
        BlockNode cuerpo = bloque(ctx.sentencia(), ctx);
        List<ElseIfNode> ramas = new ArrayList<>();
        for (YLangParser.RamaSinoContext rama : ctx.ramaSino()) {
            ramas.add(new ElseIfNode(visit(rama.expresion()), bloque(rama.sentencia(), rama),
                    line(rama), column(rama)));
        }
        if (ctx.ramaContrario() != null) {
            YLangParser.RamaContrarioContext contrario = ctx.ramaContrario();
            ramas.add(new ElseIfNode(null, bloque(contrario.sentencia(), contrario),
                    line(contrario), column(contrario)));
        }
        return new IfNode(condicion, cuerpo, ramas, line(ctx), column(ctx));
    }

    @Override
    public Node visitSeleccion(YLangParser.SeleccionContext ctx) {
        Node expresion = visit(ctx.expresion());
        List<CaseNode> casos = new ArrayList<>();
        for (YLangParser.CasoBloqueContext caso : ctx.casoBloque()) {
            Node valor = null;
            if (caso.CASO() != null) {
                valor = new LiteralNode(LiteralNode.Clase.ENTERO, caso.NUMERO().getText(),
                        line(caso), column(caso));
            }
            BlockNode cuerpo = bloque(caso.sentencia(), caso);
            casos.add(new CaseNode(valor, cuerpo.sentencias, line(caso), column(caso)));
        }
        return new SwitchNode(expresion, casos, line(ctx), column(ctx));
    }

    @Override
    public Node visitParaCiclo(YLangParser.ParaCicloContext ctx) {
        YLangParser.InicializadorParaContext inicio = ctx.inicializadorPara();
        Type tipo = Type.fromYLang(inicio.tipoPrimitivo().getText());
        Node inicializador = new VariableDeclNode(inicio.IDENTIFICADOR().getText(), tipo, null,
                visit(inicio.expresion()), line(inicio), column(inicio));
        Node condicion = visit(ctx.expresion());
        YLangParser.PasoParaContext paso = ctx.pasoPara();
        AccessNode objetivo = new AccessNode(paso.IDENTIFICADOR().getText(), line(paso), column(paso));
        Node incremento = paso.INCREMENTO() != null
                ? new IncrementNode(objetivo, line(paso), column(paso))
                : new DecrementNode(objetivo, line(paso), column(paso));
        BlockNode cuerpo = bloque(ctx.sentencia(), ctx);
        return new ForNode(inicializador, condicion, incremento, cuerpo, line(ctx), column(ctx));
    }

    @Override
    public Node visitMientrasCiclo(YLangParser.MientrasCicloContext ctx) {
        Node condicion = visit(ctx.expresion());
        BlockNode cuerpo = bloque(ctx.sentencia(), ctx);
        return new WhileNode(condicion, cuerpo, line(ctx), column(ctx));
    }

    @Override
    public Node visitHacerMientrasCiclo(YLangParser.HacerMientrasCicloContext ctx) {
        BlockNode cuerpo = bloque(ctx.sentencia(), ctx);
        Node condicion = visit(ctx.expresion());
        return new DoWhileNode(cuerpo, condicion, line(ctx), column(ctx));
    }

    @Override
    public Node visitExpresion(YLangParser.ExpresionContext ctx) {
        return visit(ctx.expresionOr());
    }

    @Override
    public Node visitExpresionOr(YLangParser.ExpresionOrContext ctx) {
        Node resultado = visit(ctx.expresionAnd(0));
        for (int i = 0; i < ctx.OR().size(); i++) {
            resultado = new BinaryOpNode(ctx.OR(i).getText(), resultado,
                    visit(ctx.expresionAnd(i + 1)), line(ctx), column(ctx));
        }
        return resultado;
    }

    @Override
    public Node visitExpresionAnd(YLangParser.ExpresionAndContext ctx) {
        Node resultado = visit(ctx.expresionIgualdad(0));
        for (int i = 0; i < ctx.AND().size(); i++) {
            resultado = new BinaryOpNode(ctx.AND(i).getText(), resultado,
                    visit(ctx.expresionIgualdad(i + 1)), line(ctx), column(ctx));
        }
        return resultado;
    }

    @Override
    public Node visitExpresionIgualdad(YLangParser.ExpresionIgualdadContext ctx) {
        Node resultado = visit(ctx.expresionRelacional(0));
        List<YLangParser.ExpresionRelacionalContext> lados = ctx.expresionRelacional();
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
    public Node visitExpresionRelacional(YLangParser.ExpresionRelacionalContext ctx) {
        Node resultado = visit(ctx.expresionAditiva(0));
        List<YLangParser.ExpresionAditivaContext> lados = ctx.expresionAditiva();
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
    public Node visitExpresionAditiva(YLangParser.ExpresionAditivaContext ctx) {
        Node resultado = visit(ctx.expresionMultiplicativa(0));
        List<YLangParser.ExpresionMultiplicativaContext> lados = ctx.expresionMultiplicativa();
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
    public Node visitExpresionMultiplicativa(YLangParser.ExpresionMultiplicativaContext ctx) {
        Node resultado = visit(ctx.expresionUnaria(0));
        List<YLangParser.ExpresionUnariaContext> lados = ctx.expresionUnaria();
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
    public Node visitExpresionUnaria(YLangParser.ExpresionUnariaContext ctx) {
        if (ctx.NEGACION() != null) {
            return new UnaryOpNode(ctx.NEGACION().getText(), visit(ctx.expresionUnaria()), line(ctx), column(ctx));
        }
        if (ctx.MENOS() != null) {
            return new UnaryOpNode("-", visit(ctx.expresionUnaria()), line(ctx), column(ctx));
        }
        return visit(ctx.factor());
    }

    @Override
    public Node visitAccesoVariable(YLangParser.AccesoVariableContext ctx) {
        AccessNode acceso = new AccessNode(ctx.IDENTIFICADOR().getText(), line(ctx), column(ctx));
        for (YLangParser.SufijoAccesoContext s : ctx.sufijoAcceso()) {
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
    public Node visitFactor(YLangParser.FactorContext ctx) {
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
        if (ctx.VERDADERO() != null) {
            return new LiteralNode(LiteralNode.Clase.BOOLEANO, "true", line(ctx), column(ctx));
        }
        if (ctx.FALSO() != null) {
            return new LiteralNode(LiteralNode.Clase.BOOLEANO, "false", line(ctx), column(ctx));
        }
        if (ctx.accesoVariable() != null) {
            return visitAccesoVariable(ctx.accesoVariable());
        }
        if (ctx.literal() != null) {
            return visitLiteral(ctx.literal());
        }
        return visit(ctx.expresion());
    }

    @Override
    public Node visitLiteral(YLangParser.LiteralContext ctx) {
        List<Node> valores = new ArrayList<>();
        for (YLangParser.ExpresionContext e : ctx.expresion()) {
            valores.add(visit(e));
        }
        return new StructLiteralNode(valores, line(ctx), column(ctx));
    }

    private List<Node> argumentosDe(YLangParser.ArgumentosContext ctx) {
        List<Node> argumentos = new ArrayList<>();
        if (ctx != null) {
            for (YLangParser.ExpresionContext e : ctx.expresion()) {
                argumentos.add(visit(e));
            }
        }
        return argumentos;
    }

    private BlockNode bloque(List<YLangParser.SentenciaContext> sentencias, ParserRuleContext ctx) {
        BlockNode bloque = new BlockNode(line(ctx), column(ctx));
        for (YLangParser.SentenciaContext s : sentencias) {
            bloque.sentencias.add(visitSentencia(s));
        }
        return bloque;
    }

    private TipoY resolverTipo(YLangParser.TipoPrimitivoContext primitivo,
            TerminalNode identificador) {
        TipoY tipo = new TipoY();
        if (primitivo != null) {
            tipo.tipo = Type.fromYLang(primitivo.getText());
        } else {
            tipo.tipo = Type.STRUCT;
            tipo.tipoNombre = identificador.getText();
        }
        return tipo;
    }

    private static final class TipoY {
        Type tipo;
        String tipoNombre;
    }

    private int line(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    private int column(ParserRuleContext ctx) {
        return ctx.getStart().getCharPositionInLine() + 1;
    }
}
