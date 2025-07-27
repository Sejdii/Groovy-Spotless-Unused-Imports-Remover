package pl.sejdii.groovy.parser

import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression


static void main(String[] args) {
	print "Hello World"
}

private void visitExpression(Expression expr) {
	switch (expr) {
		case ConstructorCallExpression:
			ConstructorCallExpression constructor = (ConstructorCallExpression) expr
			visitExpression(constructor.getArguments())
			break
	}
}