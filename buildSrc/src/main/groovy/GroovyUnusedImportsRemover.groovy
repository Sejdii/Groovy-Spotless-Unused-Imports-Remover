import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit

/**
 * Parser Groovy do usuwania nieużywanych importów
 * Używa Groovy 4.0 AST API do analizy kodu źródłowego
 */
@CompileStatic
class GroovyUnusedImportsRemover {

    /**
     * Główna metoda do usuwania nieużywanych importów z kodu Groovy
     * @param sourceCode Kod źródłowy Groovy jako String
     * @return Kod z usuniętymi nieużywanymi importami
     */
    static String removeUnusedImports(String sourceCode) {
        if (!sourceCode?.trim()) {
            return sourceCode
        }

        try {
            // Sprawdź czy plik zawiera wildcard importy (* imports)
            if (hasWildcardImports(sourceCode)) {
                // Jeśli zawiera wildcard importy, zwróć kod bez zmian
                // (zgodnie z sugestią z issue #245 w spotless)
                return sourceCode
            }

            // Parsowanie kodu do AST
            CompilerConfiguration config = new CompilerConfiguration()
            config.setTolerance(1) // Toleruj błędy podczas parsowania

            SourceUnit sourceUnit = SourceUnit.create("temp.groovy", sourceCode, 1)
            CompilationUnit compilationUnit = new CompilationUnit(config)
            compilationUnit.addSource(sourceUnit)

            // Kompiluj do fazy CONVERSION (po parsowaniu, przed resolve)
            compilationUnit.compile(Phases.CONVERSION)

            ModuleNode moduleNode = sourceUnit.getAST()
            if (!moduleNode) {
                return sourceCode
            }

            // Znajdź wszystkie importy
            List<ImportNode> allImports = collectAllImports(moduleNode)

            // Znajdź wszystkie użyte klasy/typy w kodzie
            Set<String> usedClasses = findUsedClasses(moduleNode)

            // Ustal które importy są nieużywane
            List<ImportNode> unusedImports = findUnusedImports(allImports, usedClasses)

            // Usuń nieużywane importy z kodu źródłowego
            return removeImportsFromSource(sourceCode, unusedImports)

        } catch (Exception e) {
            // W przypadku błędu parsowania, zwróć oryginalny kod
            System.err.println("Błąd podczas analizy kodu Groovy: ${e.message}")
            return sourceCode
        }
    }

    /**
     * Sprawdza czy kod zawiera wildcard importy (import pakiet.*)
     */
    private static boolean hasWildcardImports(String sourceCode) {
        return sourceCode.contains("import ") && sourceCode.contains(".*")
    }

    /**
     * Zbiera wszystkie importy z ModuleNode
     */
    private static List<ImportNode> collectAllImports(ModuleNode moduleNode) {
        List<ImportNode> allImports = []

        // Zwykłe importy
        allImports.addAll(moduleNode.getImports())

        // Statyczne importy
        allImports.addAll(moduleNode.getStaticImports().values())

        // Star importy (już sprawdziliśmy że ich nie ma)
        allImports.addAll(moduleNode.getStaticStarImports().values())

        return allImports
    }

    /**
     * Znajduje wszystkie użyte klasy w kodzie używając Visitor pattern
     */
    private static Set<String> findUsedClasses(ModuleNode moduleNode) {
        UsageVisitor visitor = new UsageVisitor()

        // Odwiedź wszystkie klasy w module
        moduleNode.getClasses().each { ClassNode classNode ->
            classNode.visitContents(visitor)
        }

        return visitor.usedClasses
    }

    /**
     * Określa które importy są nieużywane
     */
    private static List<ImportNode> findUnusedImports(List<ImportNode> allImports, Set<String> usedClasses) {
        return allImports.findAll { ImportNode importNode ->
            String importedClass = getImportedClassName(importNode)
            return !isClassUsed(importedClass, usedClasses)
        }
    }

    /**
     * Pobiera nazwę klasy z ImportNode
     */
    private static String getImportedClassName(ImportNode importNode) {
        if (importNode.getAlias()) {
            return importNode.getAlias()
        }

        String fullName = importNode.getClassName()
        if (fullName) {
            // Zwróć tylko nazwę klasy (bez pakietu)
            int lastDot = fullName.lastIndexOf('.')
            return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName
        }

        return ""
    }

    /**
     * Sprawdza czy klasa jest używana w kodzie
     */
    private static boolean isClassUsed(String className, Set<String> usedClasses) {
        if (!className) return true // Zachowaj bezpieczeństwo

        // Sprawdź dokładne dopasowanie
        if (usedClasses.contains(className)) {
            return true
        }

        // Sprawdź czy używana jest jako część qualified name
        return usedClasses.any { it.endsWith("." + className) || it.contains(className) }
    }

    /**
     * Usuwa nieużywane importy z kodu źródłowego
     */
    private static String removeImportsFromSource(String sourceCode, List<ImportNode> unusedImports) {
        if (!unusedImports) {
            return sourceCode
        }

        String[] lines = sourceCode.split("\n")
        StringBuilder result = new StringBuilder()

        for (String line : lines) {
            boolean shouldRemoveLine = false

            // Sprawdź czy linia zawiera nieużywany import
            for (ImportNode unusedImport : unusedImports) {
                if (isImportLine(line, unusedImport)) {
                    shouldRemoveLine = true
                    break
                }
            }

            if (!shouldRemoveLine) {
                result.append(line).append("\n")
            }
        }

        removeLastEmptyLine(result)

        return result.toString()
    }

    private static void removeLastEmptyLine(StringBuilder result) {
        result.delete(result.size() - 1, result.size())
    }

    /**
     * Sprawdza czy linia zawiera określony import
     */
    private static boolean isImportLine(String line, ImportNode importNode) {
        String trimmedLine = line.trim()
        if (!trimmedLine.startsWith("import ")) {
            return false
        }

        String fullClassName = importNode.getClassName()
        if (importNode.isStatic()) {
            return trimmedLine.contains("static " + fullClassName)
        } else {
            return trimmedLine.contains(" " + fullClassName)
        }
    }
}


