package com.cyl.spring.database.jdbc;

import org.junit.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CallableTest {
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

    /**
     * 调用自定义函数
     */
    @Test
    public void testCallableStatementCreator() {
        String callFunctionSql = "{call FUNCTION_TEST(?)}";
        List<SqlParameter> parameters = new ArrayList<SqlParameter>();
        parameters.add(new SqlParameter(Types.VARBINARY));
        parameters.add(new SqlReturnResultSet("result", new ResultSetExtractor<Integer>() {
            @Override
            public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                while (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }));

        Map<String, Object> outValues = jdbcTemplate.call(new CallableStatementCreator() {
            @Override
            public CallableStatement createCallableStatement(Connection con) throws SQLException {
                CallableStatement callableStatement = con.prepareCall(callFunctionSql);
                callableStatement.setString(1, "test");
                return callableStatement;
            }
        }, parameters);

        Assert.assertEquals(4, outValues.get("result"));
    }

    @Test
    public void testCallableStatementCreator1() {
        String callProcedure = "{call PROCEDURE_TEST(?,?)}";
        List<SqlParameter> parameters = new ArrayList<>();
        parameters.add(new SqlInOutParameter("inOutName",Types.VARBINARY));
        parameters.add(new SqlOutParameter("outId", Types.INTEGER));
        Map<String, Object> outValues = jdbcTemplate.call(new CallableStatementCreator() {
            @Override
            public CallableStatement createCallableStatement(Connection con) throws SQLException {
                CallableStatement callableStatement = con.prepareCall(callProcedure);
                callableStatement.registerOutParameter(1, Types.VARCHAR);
                callableStatement.registerOutParameter(2, Types.INTEGER);
                callableStatement.setString(1,"test");
                return callableStatement;
            }
        },parameters);

        Assert.assertEquals("Hello,test",outValues.get("inOutName"));
        Assert.assertEquals(0,outValues.get("outId"));



    }




}
