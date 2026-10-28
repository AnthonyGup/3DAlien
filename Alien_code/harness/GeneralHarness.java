import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.program.ImportNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.codegen.riscv.RiscVCodeGenerator;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.errors.ErrorListener;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.pigLatin.astbuilder.PigLatinASTBuilder;
import cunoc.compi2.alien_code.runner.RarsRunner;
import cunoc.compi2.alien_code.ylang.astbuilder.YLangASTBuilder;
import cunoc.compi2.alien_code.zetariano.astbuilder.ZetarianoASTBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GeneralHarness {
    private record Case(Path main, Path expected, Path input) {
    }

    private record BuildResult(String assembly, String error) {
    }

    private static ProgramNode parse(Path path, ErrorListener errors) throws Exception {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".pig")) {
            return new PigLatinASTBuilder().construir(source);
        }
        if (name.endsWith(".y")) {
            return new YLangASTBuilder().construir(source);
        }
        return new ZetarianoASTBuilder().construir(source);
    }

    private static Path resolveImport(Path root, String route) {
        String[] parts = route.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        Path path = root;
        for (int i = 0; i < parts.length - 1; i++) {
            path = path.resolve(parts[i]);
        }
        path = path.resolveSibling(path.getFileName() + "." + parts[parts.length - 1]);
        return Files.isRegularFile(path) ? path : null;
    }

    private static void collect(Path path, Path root, ErrorListener errors,
            List<ProgramNode> programs, Set<Path> visited) throws Exception {
        Path normalized = path.toAbsolutePath().normalize();
        if (!visited.add(normalized)) {
            return;
        }
        ProgramNode program = parse(normalized, errors);
        if (program == null) {
            throw new IllegalStateException("AST nulo: " + normalized);
        }
        programs.add(program);
        for (Node node : program.declarations) {
            if (node instanceof ImportNode imported) {
                Path importedPath = resolveImport(normalized.getParent(), imported.rutaCompleta);
                if (importedPath == null) {
                    throw new IllegalStateException("Import no encontrado: " + imported.rutaCompleta);
                }
                collect(importedPath, root, errors, programs, visited);
            }
        }
    }

    private static BuildResult build(Path main) {
        try {
            ErrorListener errors = new ErrorListener();
            List<ProgramNode> programs = new ArrayList<>();
            collect(main, main.getParent(), errors, programs, new HashSet<>());
            if (errors.hasErrors()) {
                return new BuildResult(null, errors.getErrors().toString());
            }
            var semantic = new cunoc.compi2.alien_code.semantic.SemanticAnalyzer(errors);
            semantic.analizar(programs);
            if (errors.hasErrors()) {
                return new BuildResult(null, errors.getErrors().toString());
            }
            var intermediate = new IntermediateCodeGenerator(errors, semantic.getSymbolTable());
            for (ProgramNode program : programs) {
                program.traducir(intermediate);
            }
            List<Cuarteta> instructions = intermediate.getInstrucciones();
            String assembly = new RiscVCodeGenerator().generate(
                    instructions,
                    semantic.getSymbolTable(),
                    intermediate.getTiposTemporales(),
                    intermediate.getLayouts()) + "\n"
                    + cunoc.compi2.alien_code.runner.RuntimeRiscV.texto();
            return new BuildResult(assembly, null);
        } catch (Exception exception) {
            return new BuildResult(null, exception.toString());
        }
    }

    private static List<Case> discover(Path root) throws Exception {
        List<Case> cases = new ArrayList<>();
        try (var files = Files.walk(root)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString();
                if (!name.equalsIgnoreCase("Main.pig") && !name.equalsIgnoreCase("Banco.pig")) {
                    continue;
                }
                Path directory = path.getParent();
                Path expected = directory.resolve("esperado.txt");
                Path input = directory.resolve("entrada.txt");
                cases.add(new Case(
                        path,
                        Files.isRegularFile(expected) ? expected : null,
                        Files.isRegularFile(input) ? input : null));
            }
        }
        cases.sort((a, b) -> a.main.toString().compareToIgnoreCase(b.main.toString()));
        return cases;
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String describeDifference(String expected, String actual) {
        String[] expectedLines = expected.split("\n", -1);
        String[] actualLines = actual.split("\n", -1);
        int limit = Math.min(expectedLines.length, actualLines.length);
        for (int i = 0; i < limit; i++) {
            if (!expectedLines[i].equals(actualLines[i])) {
                return "linea " + (i + 1) + " esperado=[" + expectedLines[i]
                        + "] actual=[" + actualLines[i] + "]";
            }
        }
        return "cantidad de lineas esperada=" + expectedLines.length
                + " actual=" + actualLines.length;
    }

    private static String label(Path root, Path main) {
        return root.relativize(main).toString().replace('\\', '/');
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        List<Case> cases = discover(root);
        int passed = 0;
        int failed = 0;
        for (Case testCase : cases) {
            String label = label(root, testCase.main);
            BuildResult build = build(testCase.main);
            if (build.error != null) {
                failed++;
                System.out.println("FAIL " + label + " BUILD " + build.error.replace('\n', ' '));
                continue;
            }
            Path output = testCase.main.getParent().resolve("build").resolve("programa.s");
            Files.createDirectories(output.getParent());
            Files.writeString(output, build.assembly, StandardCharsets.UTF_8);
            String input = testCase.input == null ? "" : read(testCase.input);
            RarsRunner.Resultado result = RarsRunner.ejecutar(build.assembly, input);
            String actual = result.salida.replace("\r\n", "\n");
            boolean matches = testCase.expected == null || actual.equals(read(testCase.expected));
            if (result.estado != RarsRunner.Estado.OK || !matches) {
                failed++;
                System.out.println("FAIL " + label + " " + result.estado);
                if (!result.mensaje.isEmpty()) {
                    System.out.println("  mensaje=" + result.mensaje.replace('\n', ' '));
                }
                if (!matches) {
                    System.out.println("  " + describeDifference(read(testCase.expected), actual));
                    String[] actualLines = actual.split("\n", -1);
                    int limit = Math.min(actualLines.length, 20);
                    for (int i = 0; i < limit; i++) {
                        System.out.println("  actual[" + (i + 1) + "]=" + actualLines[i]);
                    }
                }
                continue;
            }
            passed++;
            System.out.println("PASS " + label + (testCase.expected == null ? " (sin esperado)" : ""));
        }
        System.out.println("SUMMARY total=" + cases.size() + " passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
