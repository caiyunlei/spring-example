package com.cyl.spring.database.jdbc;

import org.junit.*;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.cyl.spring.database.jdbc.example.UserModel;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * jdbc批处理用于减少与数据库交互的次数来提升性能，spring jdbc抽象框架通过封装批处理操作来简化
 */
public class JDBCBatchTest {
    private static JdbcTemplate jdbcTemplate;

    @BeforeClass
    public static void setUpClass() {
        String url = "jdbc:hsqldb:mem:test";
        String userName = "sa";
        String passWord = "";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, userName, passWord);
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Before
    public void setUp() {
        String creatTableSql = "CREATE MEMORY TABLE test " +
                "(id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(100))";
        jdbcTemplate.update(creatTableSql);

        String creatHsqldbFunctionSql = "CREATE FUNCTION FUNCTION_TEST(str CHAR(100)) " +
                "RETURNS INT BEGIN ATOMIC RETURN length(str);END";
        jdbcTemplate.update(creatHsqldbFunctionSql);

        String creatHsqldbProcedureSql = "CREATE PROCEDURE PROCEDURE_TEST" +
                "(INOUT inOutName VARCHAR(100), OUT outId INT) " +
                "MODIFIES SQL DATA " +
                "BEGIN ATOMIC " +
                "  INSERT INTO test(name) VALUES (inOutName); " +
                "  SET outId = IDENTITY(); " +
                "  SET inOutName = 'Hello,' + inOutName; " +
                "END";
        jdbcTemplate.execute(creatHsqldbProcedureSql);
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("DROP FUNCTION FUNCTION_TEST");
        jdbcTemplate.execute("DROP PROCEDURE PROCEDURE_TEST");
        String dropTableSql = "DROP TABLE test";
        jdbcTemplate.update(dropTableSql);
    }


    @Test
    public void testBatchByJdbcTemplate() {
        String insertSql = "insert into test(name) values('name5')";
        String[] batchSql = new String[]{insertSql,insertSql};
        jdbcTemplate.batchUpdate(batchSql);
        Assert.assertEquals(2,jdbcTemplate.queryForList("SELECT * FROM Test").size());
    }

    @Test
    public void testBatchByJdbcTemplate1() {
        String insertSql = "INSERT INTO test(name) VALUES(?)";
        String[] batchValues = {"name5","name6"};
        jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1,batchValues[i]);
            }

            @Override
            public int getBatchSize() {
                return batchValues.length;
            }
        });
        Assert.assertEquals(2, jdbcTemplate.queryForList("SELECT * FROM Test").size());
    }

    @Test
    public void testBatchByNamedParameterJdbcTemplate() {
        String insertSql = "INSERT INTO test(name) VALUES(:myName)";
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        UserModel userModel = new UserModel();
        userModel.setMyName("name5");
        SqlParameterSource[] parameterSources = SqlParameterSourceUtils.createBatch(new Object[]{userModel,userModel});
        namedParameterJdbcTemplate.batchUpdate(insertSql, parameterSources);

        Assert.assertEquals(2, jdbcTemplate.queryForList("SELECT * FROM Test").size());
    }

    @Test
    public void testBatchBySimpleJdbcInsert() {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate);
        insert.withTableName("test");
        Map<String, Object> vauleMap = new HashMap<>();
        vauleMap.put("name", "name5");
        insert.executeBatch(new Map[]{vauleMap,vauleMap});
        Assert.assertEquals(2,jdbcTemplate.queryForList("SELECT * FROM test").size());
    }

}
