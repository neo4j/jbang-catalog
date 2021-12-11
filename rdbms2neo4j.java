///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli-codegen:4.6.2
//DEPS org.neo4j.driver:neo4j-java-driver:4.4.1
//DEPS org.postgresql:postgresql:42.2.24
//DEPS mysql:mysql-connector-java:8.0.27

// export JDBC_URL="jdbc:postgresql://db-examples.cmlvojdj5cci.us-east-1.rds.amazonaws.com/northwind?user=n4examples&password=36gdOVABr3Ex"
// export NEO4J_URL="neo4j+s://xxxx.databases.neo4j.io"
// export NEO4J_PASSWORD="secret"
// jbang rdbms2neo4j -p <pass> -j jdbc-url -a neo4j-url Customers

import static java.lang.System.*;
import java.util.concurrent.Callable;

import java.sql.*;
import java.util.*;
import org.neo4j.driver.*;
import org.neo4j.driver.summary.*;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(name = "rdbms2neo4j")
public class rdbms2neo4j implements Callable {

    static final int BATCH_SIZE = 10000;

    @Option(names = "-u", description = "Neo4j user", required = false, defaultValue = "neo4j")
    String username;

    @Option(names = "-p", description = "Password of the provided Neo4j user", required=true, defaultValue = "${NEO4J_PASSWORD}")
    String password;

    @Option(names = "-a", description = "URI to connect to (defaults to ${DEFAULT-VALUE})", paramLabel = "URI", defaultValue = "${NEO4J_URL:-neo4j://localhost:7687}")
    String uri;

    @Option(names = "-j", description = "JDBC-URL to read from", paramLabel = "JDBC", defaultValue = "${JDBC_URL}")
    String jdbc;

    @Parameters(arity = "1..*", description = "JDBC Tables")
    String[] tables;

    public static void main(String... args) {
        System.exit(new CommandLine(new rdbms2neo4j()).execute(args));
    }

    static final String INSERT_STATEMENT = "UNWIND $data AS row CREATE (n:`%s`) SET n = row";

    private void importTable(Session session, Connection con, String table) throws Exception {
        String label = table.substring(0,1).toUpperCase() + table.substring(1).toLowerCase();
        String statement = String.format(INSERT_STATEMENT, label);
        int nodes = 0;
        try (Statement stmt=con.createStatement();
             ResultSet rs=stmt.executeQuery("SELECT * FROM "+table)) {
                ResultSetMetaData meta=rs.getMetaData();
                String[] cols=new String[meta.getColumnCount()];
                for (int c=1;c<=cols.length;c++) 
                    cols[c-1]=meta.getColumnName(c);
                List<Map<String,Object>> data = new ArrayList<>(BATCH_SIZE);
                while (rs.next()) {
                    Map<String,Object> row=new HashMap<>(cols.length);
                    for (int c=1;c<=cols.length;c++) {
                        // todo unsupported datatypes like BigDecimal
                        row.put(cols[c-1], rs.getObject(c)); 
                    }
                    data.add(row);
                    if (data.size() == BATCH_SIZE) {
                        nodes += executeStatement(session, statement, data);
                        data.clear();
                    }
                }
                if (!data.isEmpty()) {
                    nodes += executeStatement(session, statement, data);
                }
                out.println("Nodes created: " + nodes);
            }
    }

    private int executeStatement(Session session, String statement, List<Map<String,Object>>  data) {
        ResultSummary sum = session.writeTransaction(
            tx -> tx.run(statement, Collections.singletonMap("data", data)).consume());
        return sum.counters().nodesCreated();
    }

    @Override
    public Integer call() throws Exception {
        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(uri, 
                             AuthTokens.basic(username,password));
             Session session = driver.session();
             Connection con=DriverManager.getConnection(jdbc)) {
                for (String table : tables) {
                    importTable(session, con, table);
                }
                return 0;                
        }
    }
}
