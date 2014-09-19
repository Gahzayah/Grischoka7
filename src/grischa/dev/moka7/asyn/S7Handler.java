/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.moka7.asyn;

import grischa.dev.db.table.TableController;
import grischa.dev.db.utility.DBUtil;
import grischa.dev.moka7.client.S7Item;
import grischa.dev.moka7.data.ProcVars;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author MaHi
 */
public class S7Handler {

    // Class-Logger
    private static final Logger logger = Logger.getLogger(S7Handler.class.getName());
    // Database
    private DBUtil db;
    private Connection conn;
//    private final DataSource dataSource;
    private ResultSet rs;
    private PreparedStatement pstm;
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
            logger.log(Level.SEVERE, "Database Connection Error (driver = " + db.getDriver() + " )", ex);
        }
        logger.log(Level.INFO, "Connect to Database ... succesfully.");
    }

    public void showTableModel(String tableName) {
        TableController controller = new TableController(conn);
        controller.setTableName(tableName);
        try {
            controller.connect();
        } catch (SQLException ex) {
            logger.log(Level.INFO, "Fehler im TableModel", ex);
        }
        controller.setVisible(true);
    }

    public void readProcVars(int device) {
        rs = null;
        pstm = null;
        int[] array = null;
        String tableName = "ProcessVars";
        device = 1;

        try {
            pstm = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE Device = " + device);
            pstm.setString(1, tableName);
            rs = pstm.executeQuery();
            int end = 0;
            while (rs.next()) {
                array = getProcVarsParameter(rs.getString("DB"));
                S7Item item = new S7Item(array[1],array[2],end);
//                pv.add(S7)
            }
        } catch (SQLException ex) {
            Logger.getLogger(S7Handler.class.getName()).log(Level.SEVERE, null, ex);
        }


        /* ToDo */
        /* SELECT * from ProcessVariable */
        /* Papp die Variablen in eine Arraylist */
        /* Check die Liste */
    }
    
    private int[] getProcVarsParameter(String DBParam){
        int[] array = null;
        String[] arr = null;
        String d = DBParam;
        // Cut DB from String
        d = DBParam.substring(2);
        arr = d.split(".");
        array[1] = Integer.parseInt(arr[0]);
        array[2] = Integer.parseInt(arr[1]);
                
        
        return array;
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
