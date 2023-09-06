///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS info.picocli:picocli:4.7.5
//DEPS info.picocli:picocli-codegen:4.7.5
//DEPS org.neo4j:neo4j-cypher-dsl-parser:2023.7.0

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.parser.CypherParser;

import picocli.CommandLine;

/**
 * This program reads Cypher from standard in plus either a schema file containing one or more
 * <code>(sourceLabel, TYPE, targetLabel)</code> triplets describing single labels and their relationships or multiple
 * arguments like <code>--relationship "(sourceLabel, TYPE, targetLabel)"</code> this and validates / fixes all
 * relationships in the Cypher query to be in line with the schema.
 * <p>
 * In case a pattern is not found at all, the empty string will be printed to stdin, otherwise the validated and fixed
 * Cypher statement.
 * <p>
 * You can optionally choose to always escape names and / or pretty print the result.
 * Here's one complete example:
 *
 * <pre>
 * {@code
 *      jbang enforce_relationship_directions.java \
 *          --relationship "(Person, REVIEWED, Movie)" \
 *          --relationship "(Person, ACTED_IN, Movie)" \
 *          "MATCH (n:Person) <-[:ACTED_IN]-(:Movie) RETURN n"
 * }
 * </pre>
 *
 * The output will be {@code MATCH (n:Person)-[:ACTED_IN]->(:Movie) RETURN n}.
 * <p>
 * If you need to want to use this program in a pipe, just read Cypher from standard in.
 * In this case it is most likely easier to specify the schema as a file, too:
 *
 * <pre>
 * {@code
 *      echo "MATCH (n:Person) <-[:ACTED_IN]-(:Movie) RETURN n" |\
 *      jbang enforce_relationship_directions.java \
 *          --relationship "(Person, REVIEWED, Movie)" \
 *          --relationship "(Person, ACTED_IN, Movie)" 2>/dev/null \
 *          2>/dev/null
 * }
 * </pre>
 *
 * <p>
 * You need <a href="https://www.jbang.dev">JBang</a> to run this program.
 * JBang will automatically download the right Java version for you.
 * <p>
 * Install JBang first
 * <pre>
 * {@code
 *      # Using SDKMan
 *      sdk install jbang
 *      # Homebrew
 *      brew install jbangdev/tap/jbang
 *      # or cURL
 *      curl -Ls https://sh.jbang.dev | bash -s - app setup
 * }
 * </pre>
 * Now you can run the program like this
 *
 * <pre>
 * {@code
 *      jbang enforce_relationship_directions.java --help
 * }
 * </pre>
 *
 * or if you are feeling fancy
 * <pre>
 * {@code
 *     chmpd +x enforce_relationship_directions.java
 *     enforce_relationship_directions.java --help
 * }</pre>
 *
 * There program will print standard help options.
 * <p>
 * If you have <a href="https://www.graalvm.org">Oracle GraalVM</a> together with the <code>native-image</code> tool
 * installed, you can create a binary executable for your operating system, that does not need the JVM at all.
 *
 * <pre>
 * {@code
 *      jbang export native enforce_relationship_directions.java
 *      ./enforce_relationship_directions --help
 * }
 * </pre>
 *
 * Last but not least, it's also in the Neo4j JBang catalog, and you can directly run from there, too:
 * <pre>
 * {@code
 *      jbang enforce-relationship-directions@neo4j --help
 * }
 * </pre>
 * @author Michael J. Simons
 */
@CommandLine.Command(mixinStandardHelpOptions = true)
public class enforce_relationship_directions implements Callable<Integer> {

	public static void main(String... args) {

		int exitCode = new CommandLine(new enforce_relationship_directions()).execute(args);
		System.exit(exitCode);
	}

	@CommandLine.Option(
		names = {"--relationship"},
		description = "Definition of the relationships, can be used multiple times or as a list (as in the CSV file).",
		split = "\\),"
	)
	private List<String> relationships = new ArrayList<>();

	@CommandLine.Option(names = {"--schema"}, description = "File containing the comma separated relationship definitions")
	private Path schema;

	@CommandLine.Option(
		names = {"--always-escape"},
		description = "Whether the Cypher-DSL should always escape labels and types.",
		defaultValue = "false"
	)
	private boolean alwaysEscape;

	@CommandLine.Option(
		names = {"--pretty-print"},
		description = "Whether the Cypher-DSL should pretty print the result",
		defaultValue = "false"
	)
	private boolean prettyPrint;

	@CommandLine.Parameters(index = "0", description = "Cypher to format", arity = "0")
	String input;

	@Override
	public Integer call() throws IOException {
		var cypher = this.input == null ? readStdin() : this.input;
		if (cypher.isBlank()) {
			throw new IllegalArgumentException("Please enter some valid cypher");
		}
		var statement = CypherParser.parse(cypher);

		var configuration = Configuration.newConfig()
			.withPrettyPrint(prettyPrint)
			.alwaysEscapeNames(alwaysEscape)
			.withEnforceSchema(true);

		relationships
			.stream().map(Configuration::relationshipDefinition)
			.forEach(configuration::withRelationshipDefinition);
		readSchema().forEach(configuration::withRelationshipDefinition);

		var result = Renderer.getRenderer(configuration.build()).render(statement);
		if (!result.isBlank()) {
			System.out.println(result);
		}
		return 0;
	}

	private Collection<Configuration.RelationshipDefinition> readSchema() throws IOException {
		if (schema == null) {
			return Set.of();
		}

		if (!Files.exists(schema)) {
			throw new IllegalArgumentException("Schema file " + schema.toAbsolutePath() + " does not exists");
		}

		var p = Pattern.compile("\\(.*?\\)");
		var result = new HashSet<Configuration.RelationshipDefinition>();
		Files.readAllLines(schema, StandardCharsets.UTF_8)
			.forEach(l -> {
				var m = p.matcher(l);
				while (m.find()) {
					result.add(Configuration.relationshipDefinition(m.group(0)));
				}
			});
		return result;
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
