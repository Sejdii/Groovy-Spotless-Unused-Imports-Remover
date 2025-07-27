package pl.sejdii.groovy.parser

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GroovyClassVisitor
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement

/**
 * A visitor for finding class references in Groovy Abstract Syntax Tree (AST).
 * Traverses code structures to identify all class types being used.
 */
@CompileStatic
class UsageVisitor implements GroovyClassVisitor {
	private final Set<String> usedClasses = new HashSet<>()

	Set<String> getUsedClasses() {
		return Collections.unmodifiableSet(usedClasses)
	}

	@Override
	void visitClass(ClassNode node) {
		addUsedClass(node.getName())

		if (node.getSuperClass()) {
			addUsedClass(node.getSuperClass().getName())
		}

		node.getInterfaces().each { ClassNode interfaceNode ->
			addUsedClass(interfaceNode.getName())
		}

		node.getFields().each { FieldNode field ->
			visitField(field)
		}

		node.getMethods().each { MethodNode method ->
			visitMethod(method)
		}
	}

	@Override
	void visitMethod(MethodNode node) {
		if (node.getReturnType()) {
			addUsedClass(node.getReturnType().getName())
		}

		node.getParameters().each { Parameter param ->
			if (param.getType()) {
				addUsedClass(param.getType().getName())
			}
		}

		if (node.getCode()) {
			visitStatement(node.getCode())
		}
	}

	@Override
	void visitField(FieldNode node) {
		if (node.getType()) {
			addUsedClass(node.getType().getName())
		}

		if (node.getInitialExpression()) {
			visitExpression(node.getInitialExpression())
		}
	}

	@Override
	void visitProperty(PropertyNode node) {
		if (node.getType()) {
			addUsedClass(node.getType().getName())
		}
	}

	@Override
	void visitConstructor(ConstructorNode node) {
		visitMethod(node)
	}

	void visitStatement(Statement stmt) {
		if (stmt == null) {
			return
		}

		switch (stmt) {
			case BlockStatement:
				((BlockStatement) stmt).getStatements().each { visitStatement(it) }
				break

			case ExpressionStatement:
				visitExpression(((ExpressionStatement) stmt).getExpression())
				break

			case IfStatement:
				IfStatement ifStmt = (IfStatement) stmt
				visitExpression(ifStmt.getBooleanExpression().getExpression())
				visitStatement(ifStmt.getIfBlock())
				if (ifStmt.getElseBlock()) {
					visitStatement(ifStmt.getElseBlock())
				}
				break

			case ForStatement:
				ForStatement forStmt = (ForStatement) stmt
				visitExpression(forStmt.getCollectionExpression())
				visitStatement(forStmt.getLoopBlock())
				break

			case WhileStatement:
				WhileStatement whileStmt = (WhileStatement) stmt
				visitExpression(whileStmt.getBooleanExpression().getExpression())
				visitStatement(whileStmt.getLoopBlock())
				break

			case SwitchStatement:
				SwitchStatement switchStatement = (SwitchStatement) stmt
				switchStatement.getCaseStatements().each {visitStatement(it)}
				break

			case CaseStatement:
				CaseStatement caseStatement = (CaseStatement) stmt
				visitStatement(caseStatement.getCode())
				break

			case TryCatchStatement:
				TryCatchStatement tryStmt = (TryCatchStatement) stmt
				visitStatement(tryStmt.getTryStatement())
				tryStmt.getCatchStatements().each { CatchStatement catchStmt ->
					if (catchStmt.getExceptionType()) {
						addUsedClass(catchStmt.getExceptionType().getName())
					}
					visitStatement(catchStmt.getCode())
				}
				if (tryStmt.getFinallyStatement()) {
					visitStatement(tryStmt.getFinallyStatement())
				}
				break
		}
	}

	void visitExpression(Expression expr) {
		if (expr == null) {
			return
		}

		switch (expr) {
			case ConstructorCallExpression:
				ConstructorCallExpression constructor = (ConstructorCallExpression) expr
				addUsedClass(constructor.getType().getName())
				visitExpression(constructor.getArguments())
				break

			case MethodCallExpression:
				MethodCallExpression methodCall = (MethodCallExpression) expr
				visitExpression(methodCall.getObjectExpression())
				visitExpression(methodCall.getArguments())

				def objectExpr = methodCall.getObjectExpression()
				if (objectExpr instanceof ClassExpression) {
					addUsedClass(objectExpr.getType().getName())
				} else if (objectExpr instanceof VariableExpression) {
					addUsedClass(objectExpr.getName())
				}
				break

			case PropertyExpression:
				PropertyExpression propExpr = (PropertyExpression) expr
				visitExpression(propExpr.getObjectExpression())
				break

			case VariableExpression:
				VariableExpression varExpr = (VariableExpression) expr
				if (varExpr.getType()) {
					addUsedClass(varExpr.getType().getName())
				}
				break

			case CastExpression:
				CastExpression castExpr = (CastExpression) expr
				addUsedClass(castExpr.getType().getName())
				visitExpression(castExpr.getExpression())
				break

			case ClassExpression:
				ClassExpression classExpr = (ClassExpression) expr
				addUsedClass(classExpr.getType().getName())
				break

			case ArgumentListExpression:
				ArgumentListExpression argList = (ArgumentListExpression) expr
				argList.getExpressions().each { visitExpression(it) }
				break

			case ListExpression:
				ListExpression listExpr = (ListExpression) expr
				listExpr.getExpressions().each { visitExpression(it) }
				break

			case MapExpression:
				MapExpression mapExpr = (MapExpression) expr
				mapExpr.getMapEntryExpressions().each { MapEntryExpression entry ->
					visitExpression(entry.getKeyExpression())
					visitExpression(entry.getValueExpression())
				}
				break

			case BinaryExpression:
				BinaryExpression binExpr = (BinaryExpression) expr
				visitExpression(binExpr.getLeftExpression())
				visitExpression(binExpr.getRightExpression())
				break

			case ClosureExpression:
				ClosureExpression closureExpr = (ClosureExpression) expr
				closureExpr.getParameters().each { addUsedClass(it.getType().getName()) }
				break
		}
	}

	private void addUsedClass(String className) {
		if (className == null || className.isEmpty() || className.startsWith("java.lang.")) {
			return
		}

		usedClasses.add(className)

		int lastDot = className.lastIndexOf('.')
		if (lastDot > 0) {
			usedClasses.add(className.substring(lastDot + 1))
		}
	}
}