//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 8+
//DEPS org.liquibase.ext:liquibase-neo4j:4.19.0
//DEPS org.liquibase:liquibase-core:4.19.0
//DEPS info.picocli:picocli:4.6.1

public class liquibase_neo4j {
    public static void main(String... args) throws Exception {
        liquibase.integration.commandline.LiquibaseCommandLine.main(args);
    }
}
