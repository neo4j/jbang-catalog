import java.util.concurrent.Callable;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli-codegen:4.6.2 org.neo4j.driver:neo4j-java-driver:4.3.6 org.reactivestreams:reactive-streams:1.0.3

@Command(name = "hello_neo4j")
public class hello_neo4j implements Callable<Integer> {

    @Option(names = "-u", description = "Neo4j user", required = true)
    String username;

    @Option(names = "-p", description = "Password of the provided Neo4j user", required = true)
    String password;

    @Parameters(index = "0", description = "URI to connect to (defaults to neo4j://localhost:7687)", paramLabel = "URI", defaultValue = "neo4j://localhost:7687")
    String uri;

    public static void main(String... args) {
        int exitCode = new CommandLine(new hello_neo4j()).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        var driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), Config.builder().withLogging(Logging.none()).build());

        printStatistics(driver);

        driver.close();
        return 0;
    };

    private void printStatistics(Driver driver) {
        try (var session = driver.session()) {
            var totalNodes = session.run("MATCH (n) return count(n) as totalNodes").single().get("totalNodes").asLong();
            var totalRelationships = session.run("MATCH ()-[r]->() return count(r) as totalRelationships").single().get("totalRelationships").asLong();
            var version = session.run("call dbms.components() yield versions return versions[0] as version").single().get("version").asString();

            System.out.println(new Statistics(version, totalNodes, totalRelationships));
        }
    }

    private record Statistics(String version, Long totalNodes, Long totalRelationships){}

    
}
