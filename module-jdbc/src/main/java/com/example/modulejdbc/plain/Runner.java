package com.example.modulejdbc.plain;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementSetter;

public class Runner {

    /**
     * {@link java.sql.DriverManager} {@link java.sql.Driver} {@link javax.sql.DataSource} {@link java.sql.Connection}
     * {@link java.sql.Statement} {@link java.sql.PreparedStatement} {@link java.sql.CallableStatement}
     * {@link java.sql.ResultSet} {@link java.sql.ResultSetMetaData} {@link java.sql.DatabaseMetaData}
     * {@link java.sql.SQLException}
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        DriverManager.drivers().forEach(driver -> System.out.println("Found driver: " + driver.getClass()));

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "")) {
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("DatabaseMajorVersion: " + metaData.getDatabaseMajorVersion());
            System.out.println("DriverMinorVersion: " + metaData.getDriverMinorVersion());
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:test");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");

        DataSource dataSource = new HikariDataSource(hikariConfig);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        for (int i = 0; i < 3; i++) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                System.out.println("DatabaseMajorVersion: " + metaData.getDatabaseMajorVersion());
                System.out.println("DriverMinorVersion: " + metaData.getDriverMinorVersion());
                System.out.println("===========================================");
            }
        }

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-persistence.xml");
        liquibase.setShouldRun(true);
        liquibase.afterPropertiesSet();

        jdbcTemplate.update(
            "INSERT INTO example (id, name) VALUES (?, ?)",
            new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement) throws SQLException {
                    statement.setLong(1, 1);
                    statement.setString(2, "One");
                }
            }
        );

        jdbcTemplate.update(
            "INSERT INTO example (id, name) VALUES (?, ?)",
            new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement) throws SQLException {
                    statement.setLong(1, 2);
                    statement.setString(2, "Two");
                }
            }
        );

        List<Person> toInsert = List.of(new Person(100L, "100"), new Person(200L, "200"));

        jdbcTemplate.batchUpdate(
            "INSERT INTO example (id, name) VALUES (?, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement, int i) throws SQLException {
                    statement.setLong(1, toInsert.get(i).id);
                    statement.setString(2, toInsert.get(i).name);
                }

                @Override
                public int getBatchSize() {
                    return toInsert.size();
                }
            }
        );


        List<Person> people = jdbcTemplate.query(
            "SELECT * FROM example",
            (resultSet, rowNum) -> {
                Person person = new Person();
                person.id = resultSet.getLong(1);
                person.name = resultSet.getString(2);
                return person;
            });

        people.forEach(System.out::println);

    }

}

class Person {

    Long id;
    String name;

    public Person() {
    }

    public Person(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Person{" +
            "id=" + id +
            ", name='" + name + '\'' +
            '}';
    }
}
