//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//DEPS org.liquibase.ext:liquibase-neo4j:5.0.3
//DEPS org.liquibase:liquibase-core:5.0.3
//DEPS info.picocli:picocli:4.7.7

public class liquibase_neo4j {
    public static void main(String... args) throws Exception {
        liquibase.integration.commandline.LiquibaseCommandLine.main(args);
    }
}
