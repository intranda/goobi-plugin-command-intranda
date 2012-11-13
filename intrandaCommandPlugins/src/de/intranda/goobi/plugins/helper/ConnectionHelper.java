//package de.intranda.goobi.plugins.helper;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//
//import org.hibernate.engine.spi.SessionFactoryImplementor;
//
//import de.sub.goobi.Persistence.HibernateUtilOld;
//
//public class ConnectionHelper {
//
//	public static Connection getConnection()  {
//		SessionFactoryImplementor sfi = (SessionFactoryImplementor) HibernateUtilOld.getSessionFactory();
//		try {
//			Connection con = sfi.getConnectionProvider().getConnection();
//			return con;
//		} catch (SQLException e) {
//			
//			return null;
//		}
//		
//		
//	}
//
//}
