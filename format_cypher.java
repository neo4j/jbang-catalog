///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS info.picocli:picocli:4.6.3
//DEPS org.neo4j:neo4j-cypher-dsl-parser:2022.5.0

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.parser.CypherParser;

public class format_cypher implements Callable<Integer> {

	public static void main(String... args) {

		int exitCode = new CommandLine(new format_cypher()).execute(args);
		System.exit(exitCode);
	}

	@CommandLine.Parameters(index = "0", description = "Cypher to format", arity = "0")
	String input;

	@Override
	public Integer call() {
		var cypher = this.input == null ? readStdin() : this.input;
		if (cypher.isBlank()) {
			throw new IllegalArgumentException("Please enter some valid cypher");
		}
		var statement = CypherParser.parse(cypher);
		System.out.println(Renderer.getRenderer(Configuration.prettyPrinting()).render(statement));
		return 0;
	}

	private static String readStdin() {
		System.err.println("Reading from stdin, end with EOF (most likely CTRL+D).");
		try (var in = new BufferedReader(new InputStreamReader(System.in))) {
			return in.lines().collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
