package pl.sejdii.groovy.parser

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.control.SourceUnit


/**
 * Removes all unused imports from groovy code.
 */
@CompileStatic
class GroovyUnusedImportsRemover {

	static String removeUnusedImports(String sourceCode) {
		if (!sourceCode?.trim()) {
			return sourceCode
		}

		try {
			if (hasWildcardImports(sourceCode)) {
				// wildcards are not supported
				return sourceCode
			}

			ModuleNode moduleNode = compileSourceCode(sourceCode)
			if (!moduleNode) {
				return sourceCode
			}

			List<ImportNode> unusedImports = findUnusedImports(moduleNode)

			return removeImportsFromSource(sourceCode, unusedImports)
		} catch (Exception e) {
			System.err.println("Błąd podczas analizy kodu Groovy: ${e.message}")
			return sourceCode
		}
	}

	private static boolean hasWildcardImports(String sourceCode) {
		return sourceCode.contains("import ") && sourceCode.contains(".*")
	}

	private static ModuleNode compileSourceCode(String sourceCode) {
		CompilerConfiguration config = new CompilerConfiguration()
		config.setTolerance(1)

		SourceUnit sourceUnit = SourceUnit.create("temp.groovy", sourceCode, 1)
		CompilationUnit compilationUnit = new CompilationUnit(config)
		compilationUnit.addSource(sourceUnit)

		compilationUnit.compile(Phases.CONVERSION)

		ModuleNode moduleNode = sourceUnit.getAST()
		moduleNode
	}

	private static List<ImportNode> findUnusedImports(ModuleNode moduleNode) {
		List<ImportNode> allImports = collectAllImports(moduleNode)
		Set<String> usedClasses = findUsedClasses(moduleNode)
		List<ImportNode> unusedImports = findUnusedImports(allImports, usedClasses)
		unusedImports
	}


	private static List<ImportNode> collectAllImports(ModuleNode moduleNode) {
		List<ImportNode> allImports = []

		allImports.addAll(moduleNode.getImports())
		allImports.addAll(moduleNode.getStaticImports().values())
		allImports.addAll(moduleNode.getStaticStarImports().values())

		return allImports
	}

	private static Set<String> findUsedClasses(ModuleNode moduleNode) {
		UsageVisitor visitor = new UsageVisitor()

		moduleNode.getClasses().each { ClassNode classNode ->
			classNode.visitContents(visitor)
			visitor.visitClass(classNode)
			classNode.getAnnotations().each { visitor.visitClass(it.getClassNode())}
		}

		return visitor.usedClasses
	}

	private static List<ImportNode> findUnusedImports(List<ImportNode> allImports, Set<String> usedClasses) {
		return allImports.findAll { ImportNode importNode ->
			String importedClass = getImportedClassName(importNode)
			return !isClassUsed(importedClass, usedClasses)
		}
	}

	private static String getImportedClassName(ImportNode importNode) {
		if (importNode.getAlias()) {
			return importNode.getAlias()
		}

		String fullName = importNode.getClassName()
		if (fullName) {
			int lastDot = fullName.lastIndexOf('.')
			return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName
		}

		return ""
	}

	private static boolean isClassUsed(String className, Set<String> usedClasses) {
		if (!className) return true

		if (usedClasses.contains(className)) {
			return true
		}

		return usedClasses.any { it.endsWith("." + className) || it.contains(className) }
	}

	private static String removeImportsFromSource(String sourceCode, List<ImportNode> unusedImports) {
		if (!unusedImports) {
			return sourceCode
		}

		String[] lines = sourceCode.split("\n")
		StringBuilder result = new StringBuilder()

		for (String line : lines) {
			boolean shouldRemoveLine = false

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


