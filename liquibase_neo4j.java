//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 8+
//DEPS org.liquibase.ext:liquibase-neo4j:4.9.1
//DEPS org.liquibase:liquibase-core:4.9.1
//DEPS info.picocli:picocli:4.6.1
//DEPS org.neo4j:neo4j-jdbc-bolt:4.0.5

public class liquibase_neo4j {
    public static void main(String... args) throws Exception {
        liquibase.integration.commandline.LiquibaseCommandLine.main(args);
    }
}
