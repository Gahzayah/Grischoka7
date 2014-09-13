/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package _test;

import grischa.dev.db.table.TableController;
import grischa.dev.db.utility.DBUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MaHi
 */
public class main_database {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        DBUtil dbutil = new DBUtil("SQLITE");
        Connection conn = null;
        try {
            conn = dbutil.getConnection();
            TableController controller = new TableController(conn);
            controller.connect();
            controller.setVisible(true);

        } catch (SQLException ex) {
            Logger.getLogger(main_database.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
