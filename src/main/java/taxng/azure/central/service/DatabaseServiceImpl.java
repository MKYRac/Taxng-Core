package taxng.azure.central.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabaseServiceImpl implements DatabaseService {

	@Value("${host.name}")
	private String host;
	
	@Value("${database.name}")
	private String database;
	
	@Value("${user.name.postgresql}")
	private String userName;
	
	@Value("${password.db}")
	private String password;
	
	private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);
	
	@Override
	public void saveDataToDB(int txId, double bPrice, double sPrice, int nos, double taxRate, double result) throws Exception {
		
		Connection connection = null;
		
		try {
            String url = String.format("jdbc:postgresql://%s/%s", host, database);
            
            // Set up the connection properties
            Properties properties = new Properties();
            properties.setProperty("user", userName);
            properties.setProperty("password", password);
            properties.setProperty("ssl", "true");
            
            // Get connection
            connection = DriverManager.getConnection(url, properties);
        }
        catch (SQLException e) {
            throw new SQLException("Failed to create connection to database.", e);
        }
		
		if (connection != null) {
			log.info("Successfully created connection to Database.");
			
			// Compose statement string and execute it
			try {
	            Statement statement = connection.createStatement();
	            String sql = "INSERT INTO taxmessages (taxId, bPrice, sPrice, nos, taxRate, taxAmount) ";
	            sql = sql + "VALUES (" + txId + ", " + bPrice + ", " + sPrice + ", ";
	            sql = sql + nos + ", " + taxRate + ", " + result + ");";
	            statement.execute(sql);
	            
	    		// Log successful execution
	    		log.info("Data saved to database.");
            }
            catch (SQLException e) {
            	throw new SQLException("Encountered an error when executing given sql statement.", e);
            }       
        }
		else {
			log.info("Failed to create connection to database.");
		}

	}
	
}
