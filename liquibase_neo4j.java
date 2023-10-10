//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 8+
//DEPS org.liquibase.ext:liquibase-neo4j:4.24.0
//DEPS org.liquibase:liquibase-core:4.24.0
//DEPS info.picocli:picocli:4.7.5

public class liquibase_neo4j {
    public static void main(String... args) throws Exception {
        liquibase.integration.commandline.LiquibaseCommandLine.main(args);
    }
}
