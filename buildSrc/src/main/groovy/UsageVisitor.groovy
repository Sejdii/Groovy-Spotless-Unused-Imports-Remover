import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*

/**
 * Visitor do znajdowania używanych klas w AST
 */
@CompileStatic
class UsageVisitor implements GroovyClassVisitor {
    Set<String> usedClasses = new HashSet<>()

    @Override
    void visitClass(ClassNode node) {
        // Dodaj superklasy
        if (node.getSuperClass()) {
            addUsedClass(node.getSuperClass().getName())
        }

        // Dodaj interfejsy
        node.getInterfaces().each { ClassNode interfaceNode ->
            addUsedClass(interfaceNode.getName())
        }

        // Odwiedź wszystkie fields
        node.getFields().each { FieldNode field ->
            visitField(field)
        }

        // Odwiedź wszystkie metody
        node.getMethods().each { MethodNode method ->
            visitMethod(method)
        }
    }

    @Override
    void visitMethod(MethodNode node) {
        // Dodaj typ zwracany
        if (node.getReturnType()) {
            addUsedClass(node.getReturnType().getName())
        }

        // Dodaj typy parametrów
        node.getParameters().each { Parameter param ->
            if (param.getType()) {
                addUsedClass(param.getType().getName())
            }
        }

        // Odwiedź kod metody
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

    /**
     * Odwiedza statement i wszystkie zagnieżdżone elementy
     */
    void visitStatement(Statement stmt) {
        if (stmt instanceof BlockStatement) {
            ((BlockStatement) stmt).getStatements().each { visitStatement(it) }
        } else if (stmt instanceof ExpressionStatement) {
            visitExpression(((ExpressionStatement) stmt).getExpression())
        } else if (stmt instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) stmt
            visitExpression(ifStmt.getBooleanExpression().getExpression())
            visitStatement(ifStmt.getIfBlock())
            if (ifStmt.getElseBlock()) {
                visitStatement(ifStmt.getElseBlock())
            }
        } else if (stmt instanceof ForStatement) {
            ForStatement forStmt = (ForStatement) stmt
            visitExpression(forStmt.getCollectionExpression())
            visitStatement(forStmt.getLoopBlock())
        } else if (stmt instanceof WhileStatement) {
            WhileStatement whileStmt = (WhileStatement) stmt
            visitExpression(whileStmt.getBooleanExpression().getExpression())
            visitStatement(whileStmt.getLoopBlock())
        } else if (stmt instanceof TryCatchStatement) {
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
        }
    }

    /**
     * Odwiedza expression i znajduje używane klasy
     */
    void visitExpression(Expression expr) {
        if (expr instanceof ConstructorCallExpression) {
            ConstructorCallExpression constructor = (ConstructorCallExpression) expr
            addUsedClass(constructor.getType().getName())
            visitExpression(constructor.getArguments())
        } else if (expr instanceof MethodCallExpression) {
            MethodCallExpression methodCall = (MethodCallExpression) expr
            visitExpression(methodCall.getObjectExpression())
            visitExpression(methodCall.getArguments())
            if (methodCall.getObjectExpression() instanceof ClassExpression) {
                ClassExpression classExpr = (ClassExpression) methodCall.getObjectExpression()
                addUsedClass(classExpr.getType().getName())
            }
        } else if (expr instanceof PropertyExpression) {
            PropertyExpression propExpr = (PropertyExpression) expr
            visitExpression(propExpr.getObjectExpression())
        } else if (expr instanceof VariableExpression) {
            VariableExpression varExpr = (VariableExpression) expr
            if (varExpr.getType()) {
                addUsedClass(varExpr.getType().getName())
            }
        } else if (expr instanceof CastExpression) {
            CastExpression castExpr = (CastExpression) expr
            addUsedClass(castExpr.getType().getName())
            visitExpression(castExpr.getExpression())
        } else if (expr instanceof ClassExpression) {
            ClassExpression classExpr = (ClassExpression) expr
            addUsedClass(classExpr.getType().getName())
        } else if (expr instanceof ArgumentListExpression) {
            ArgumentListExpression argList = (ArgumentListExpression) expr
            argList.getExpressions().each { visitExpression(it) }
        } else if (expr instanceof ListExpression) {
            ListExpression listExpr = (ListExpression) expr
            listExpr.getExpressions().each { visitExpression(it) }
        } else if (expr instanceof MapExpression) {
            MapExpression mapExpr = (MapExpression) expr
            mapExpr.getMapEntryExpressions().each { MapEntryExpression entry ->
                visitExpression(entry.getKeyExpression())
                visitExpression(entry.getValueExpression())
            }
        } else if (expr instanceof BinaryExpression) {
            BinaryExpression binExpr = (BinaryExpression) expr
            visitExpression(binExpr.getLeftExpression())
            visitExpression(binExpr.getRightExpression())
        }
    }

    /**
     * Dodaje używaną klasę do zbioru
     */
    private void addUsedClass(String className) {
        if (className && !className.startsWith("java.lang.")) {
            // Dodaj pełną nazwę
            usedClasses.add(className)

            // Dodaj także samą nazwę klasy (bez pakietu)
            int lastDot = className.lastIndexOf('.')
            if (lastDot > 0) {
                usedClasses.add(className.substring(lastDot + 1))
            }
        }
    }
}