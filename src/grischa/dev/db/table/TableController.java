/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.db.table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author MaHi
 */
public class TableController {

    private TableModel model = null;
    private Connection conn = null;
    private PreparedStatement pstm = null;
    private ResultSet rs = null;
    private String tableName = null;

    public TableController(Connection conn) {
        model = new TableModel(tableName);
        this.conn = conn;
    }

    public void connect() throws SQLException {
        if (conn != null) {
            conn.setAutoCommit(false); //Aenderungen werden nicht gleich gemacht

            //vorbereitet Statement
            pstm = conn.prepareStatement(
                    "SELECT * FROM " + tableName,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            //pstm.setString(1, tableName);

            rs = pstm.executeQuery();
            model.setResultSet(rs);
            model.initTableModel();
        }
    }

    public void disconnect() throws SQLException {
        rs.close();
        conn.rollback();
        pstm.close();
        conn.close();
    }

    public void setVisible(boolean visible) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */

        /**
         * Fenster Darstellung wie in Betriebssystem normal
         */
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TableModel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                model.setVisible(true);
            }
        });

    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
