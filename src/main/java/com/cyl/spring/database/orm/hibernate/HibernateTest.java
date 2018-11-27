package com.cyl.spring.database.orm.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import com.cyl.spring.database.orm.hibernate.crud.UserModel2;
import com.cyl.spring.database.jdbc.UserModel;

import java.util.List;

/**
 */
public class HibernateTest {

    private static SessionFactory sessionFactory;
    @BeforeClass
    public static void beforClass() {
        String[] configLocations = {"com/cyl/spring/database/jdbc/example/application-resources.xml", "com/cyl/spring/database/orm/hibernate/hbm/application-hibernate.xml"};
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(configLocations);
        sessionFactory = applicationContext.getBean(SessionFactory.class);
    }

    @Before
    public void setUp() {
        final String creatSql = "CREATE MEMORY TABLE test " +
                "(id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(100))";
        sessionFactory.openSession().createSQLQuery(creatSql).executeUpdate();
        System.out.println();
    }

    @After
    public void tearDown() {
        String dropTableSql = "DROP TABLE test";
        sessionFactory.openSession().createSQLQuery(dropTableSql).executeUpdate();
    }

    @Test
    public void testFirst() {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = beginTransaction(session);
            UserModel model = new UserModel();
            model.setMyName("myName");
            session.save(model);
        } catch (HibernateException e) {
            rollBackTransaction(transaction);
            throw e;
        } finally {
            commitTransaction(session);
        }
    }

    private Transaction beginTransaction(Session session) {
        Transaction transaction = session.beginTransaction();
        transaction.begin();
        return transaction;
    }

    private void rollBackTransaction(Transaction transaction) {
        if (transaction != null) {
            transaction.rollback();
        }
    }

    private void commitTransaction(Session session) {
        session.close();

    }


    @Test
    public void testHibernateTemplate() {
        HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
        final UserModel model = new UserModel();
        model.setMyName("myName");
        hibernateTemplate.save(model);
        hibernateTemplate.execute(new HibernateCallback<Void>() {
            @Override
            public Void doInHibernate(Session session) throws HibernateException {
                session.save(model);
                return null;
            }
        });
    }

    @org.junit.Test
    public void testCURD() {
        String[] configLocations = {"com/cyl/spring/database/jdbc/example/application-resources.xml", "spring/orm/hibernate/crud/application-hibernate2.xml"};
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(configLocations);
        HibernateTemplate hibernateTemplate = applicationContext.getBean(HibernateTemplate.class);
        UserModel2 model = new UserModel2();
        model.setMyName("test");
        insert(hibernateTemplate,model);
        select(hibernateTemplate,model);
        update(hibernateTemplate,model);
        delete(hibernateTemplate,model);
    }

    private void delete(HibernateTemplate hibernateTemplate, UserModel2 model) {
        hibernateTemplate.delete(model);
    }

    private void update(HibernateTemplate hibernateTemplate, UserModel2 model) {
        model.setMyName("test2");
        hibernateTemplate.update(model);
    }

    private void select(HibernateTemplate hibernateTemplate, UserModel2 model) {
        UserModel2 model2 = hibernateTemplate.get(UserModel2.class, 0);
        Assert.assertEquals(model2.getMyName(),model.getMyName());
        List<UserModel2> list = (List<UserModel2>) hibernateTemplate.find("from UserModel2");
        Assert.assertEquals(list.get(0).getMyName(),model.getMyName());
    }

    private void insert(HibernateTemplate hibernateTemplate, UserModel2 model) {
        hibernateTemplate.save(model);
    }



}
