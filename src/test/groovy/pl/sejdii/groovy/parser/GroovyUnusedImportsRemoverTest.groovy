package pl.sejdii.groovy.parser

import spock.lang.Specification


class GroovyUnusedImportsRemoverTest extends Specification {

	def "Should remove unused imports"() {
		expect:
		GroovyUnusedImportsRemover.removeUnusedImports(code) == result

		where:
		code | result
		readFile("code_one_used_one_unused") | readFile("code_one_used_one_unused_result")
		readFile("code_two_unused") | readFile("code_two_unused_result")
		readFile("code_class_extends") | readFile("code_class_extends_result")
		readFile("code_class_extends_with_unused_import") | readFile("code_class_extends_with_unused_import_result")
		readFile("code_annotation") | readFile("code_annotation_result")
		readFile("code_all_imports_used") | readFile("code_all_imports_used_result")
		readFile("code_lambda_parameter") | readFile("code_lambda_parameter_result")
 	}

	private String readFile(String fileName) {
		InputStream is = getClass().getClassLoader().getResourceAsStream("tests/${fileName}.txt")
		assert is != null : "Resource not found: $fileName"
		return is.text
	}
}