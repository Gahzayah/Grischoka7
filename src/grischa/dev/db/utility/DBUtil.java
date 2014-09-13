package grischa.dev.db.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DB Main Information Nicht vergessen die entsprechende Driver-Libary zum Projekt hinzuzufügen!
 *
 * @author MaHi
 */
public class DBUtil {
    private static final Logger database = Logger.getLogger(DBUtil.class.getName());
    private String driver = null;
    private Connection conn = null;

    
    public DBUtil(String driver) {
        switch(driver){
            case "SQLITE":  this.driver="org.sqlite.JDBC";
                            break;
            case "MYSQL" :  this.driver="com.mysql.JDBC";
                            break;
        }
    }
    /**
     * Open Database Connection
     *
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName(driver);
//           conn = DriverManager.getConnection("jdbc:sqlite:F:/Netbeans-Poject/shiqhomepage/Grischokka7/src/grischa/dev/db/sqlite3/s7node.db");
//           conn = DriverManager.getConnection("jdbc:sqlite:src/_test/s7node.db");
             conn = DriverManager.getConnection("jdbc:sqlite:src/grischa/dev/db/sqlite3/s7node.db");

        } catch (ClassNotFoundException e) {
            database.log(Level.WARNING,"Driver Class not found", e);
        }
        return conn;
    }

    /**
     * Close Connection to Database
     */
    public void close() {
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBUtil.class.getName()).log(Level.SEVERE, null, ex);
            database.log(Level.WARNING,"Close Connection Error", ex);
        }
    }

}
