/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.moka7.asyn;

import grischa.dev.db.table.TableController;
import grischa.dev.db.utility.DBUtil;
import grischa.dev.moka7.data.ProcVars;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author MaHi
 */
public class S7Handler {

    // Class-Logger

    private static final Logger logger = LogManager.getLogger(S7Handler.class.getName());
    // Database
    private DBUtil db;
    private Connection conn;
//    private final DataSource dataSource;
    private ResultSet rs;
    // Memory
    private ArrayList<ProcVars> pv;
    private ArrayList<ProcVars> wpv;
    // Service
    private S7Service svc;

    private int result = 0;

    /**
     * Initialisierungs-Konstruktor
     */
    public S7Handler() {
        db = new DBUtil("SQLITE");
    }
    
    /**
     * Connect to Database
     */
    public void connectToDB() {

        try {
            this.conn = db.getConnection();
        } catch (SQLException ex) {
            logger.log(Level.ERROR, "Database Connection Error (driver = " + db.getDriver() + " )", ex);
        }
        logger.log(Level.INFO, "Connect to Database ... succesfully.");
    }

    public void showTableModel(String tableName) {
        TableController controller = new TableController(conn , tableName);
        try {
            controller.connect();
        } catch (SQLException ex) {
            logger.log(Level.INFO, "Fehler im TableModel");
        }
        controller.setVisible(true);
    }

    public void readProcVars() {
        /* ToDo */
        /* SELECT * from ProcessVariable */
        /* Papp die Variablen in eine Arraylist */
        /* Check die Liste */
    }

    public void readProcVarsTask() {

        /* ToDo */
        /* SELECT * from WriteTask */
        /* Papp die Variablen in eine Arraylist */
        clearProcVarsTask();
        /* Übergib den Auftrag den S7Service */
        svc.setWriteAreaTask(wpv);
    }

    private void clearProcVarsTask() {
        /* DELETE ALL IDS WHO RECEIVED */
    }

    public S7Service getService() {
        return svc;
    }

    public void setService(S7Service svc) {
        this.svc = svc;
    }

}
