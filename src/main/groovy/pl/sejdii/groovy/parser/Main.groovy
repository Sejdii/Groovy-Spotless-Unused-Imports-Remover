package pl.sejdii.groovy.parser

import java.nio.file.Path
import java.net.URI

static void main(String[] args) {
	List.of(Path.of("x"), Path.of("y"))
		.stream()
		.map {it.toUri()}
		.each { URI parameter -> print parameter}
}