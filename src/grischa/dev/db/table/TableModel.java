/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grischa.dev.db.table;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 * @author MaHi
 */
public class TableModel extends JFrame {

    ResultSet rs = null;
    DefaultTableModel tableModel = null;

    /* Vector Elements */
    Vector<String> columnNames = null;
    Vector<Vector<Object>> data = null;

    /* UI Elements */
    JTable table = null;
    JTableHeader tableHeader = null;
    JScrollPane pane = null;

    private Dimension jTableSize = null;
    private final int jFrameHeight = 350;
    private final int colMargin = 5;

    public TableModel(String tableName) {
        super(tableName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Initialize TableModel
     *
     * @throws SQLException
     */
    public void initTableModel() throws SQLException {
        if (rs != null) {
            buildTableModel(rs);
            tableModel = new DefaultTableModel(data, columnNames);
            table = new JTable(tableModel);
            tableHeader = table.getTableHeader();
            
            sizeColumnsToFit(table, colMargin);
            
            jTableSize = table.getMaximumSize();  

            pane = new JScrollPane(table);
            pane.setPreferredSize(new Dimension(jTableSize.width, jFrameHeight));
            this.add(pane);
            centerJFrame(pane.getPreferredSize());
            pack();
            //JOptionPane.showMessageDialog(null, new JScrollPane(table));
        }else{
            System.out.println("Inkonsitentes Resultset");
        }

//       tableModel.fireTableDataChanged();      // Redraw JTable if this method call
//        tableModel.getDataVector();             // Contains the TableData Values
//         Vector<Vector<Object>> data = new Vector<>();
//       tableModel.setDataVector(data, null);   // Set the Vecotr Data
//        tableModel.getRowCount();                   // Number of Rows
    }

    public void centerJFrame(Dimension d) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point middle = new Point(screenSize.width / 2, screenSize.height / 2);
        Point newLocation = new Point(middle.x - (d.width / 2), middle.y - (d.height / 2) - 100);
        this.setLocation(newLocation);
    }

    /**
     * Build TableModel
     *
     * @param rs - Contains Data from Database
     * @throws SQLException
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public void buildTableModel(ResultSet rs)
            throws SQLException {

        ResultSetMetaData metaData = rs.getMetaData();

        // names of columns
        columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        // data of the table
        data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }
        
    }
    

    /**
     * Adjust the Column Size to MaxLength of the Content
     * @param table
     * @param columnMargin 
     */
    public void sizeColumnsToFit(JTable table, int columnMargin) {
        if (tableHeader == null) {
            // can't auto size a table without a header
            return;
        }

        FontMetrics headerFontMetrics = tableHeader.getFontMetrics(tableHeader.getFont());

        int[] minWidths = new int[table.getColumnCount()];
        int[] maxWidths = new int[table.getColumnCount()];

        for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
            int headerWidth = headerFontMetrics.stringWidth(table.getColumnName(columnIndex));

            minWidths[columnIndex] = headerWidth + columnMargin;

            int maxWidth = getMaximalRequiredColumnWidth(table, columnIndex, headerWidth);

            maxWidths[columnIndex] = Math.max(maxWidth, minWidths[columnIndex]) + columnMargin;
        }

        adjustMaximumWidths(table, minWidths, maxWidths);

        for (int i = 0; i < minWidths.length; i++) {
            if (minWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMinWidth(minWidths[i]);
            }

            if (maxWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMaxWidth(maxWidths[i]);

                table.getColumnModel().getColumn(i).setWidth(maxWidths[i]);
            }
        }
    }

    private static int getMaximalRequiredColumnWidth(JTable table, int columnIndex, int headerWidth) {
        int maxWidth = headerWidth;

        TableColumn column = table.getColumnModel().getColumn(columnIndex);

        TableCellRenderer cellRenderer = column.getCellRenderer();

        if (cellRenderer == null) {
            cellRenderer = new DefaultTableCellRenderer();
        }

        for (int row = 0; row < table.getModel().getRowCount(); row++) {
            Component rendererComponent = cellRenderer.getTableCellRendererComponent(table,
                    table.getModel().getValueAt(row, columnIndex),
                    false,
                    false,
                    row,
                    columnIndex);

            double valueWidth = rendererComponent.getPreferredSize().getWidth();

            maxWidth = (int) Math.max(maxWidth, valueWidth);
        }

        return maxWidth;
    }

    private static void adjustMaximumWidths(JTable table, int[] minWidths, int[] maxWidths) {
        if (table.getWidth() > 0) {
            // to prevent infinite loops in exceptional situations
            int breaker = 0;

            // keep stealing one pixel of the maximum width of the highest column until we can fit in the width of the table
            while (sum(maxWidths) > table.getWidth() && breaker < 10000) {
                int highestWidthIndex = findLargestIndex(maxWidths);

                maxWidths[highestWidthIndex] -= 1;

                maxWidths[highestWidthIndex] = Math.max(maxWidths[highestWidthIndex], minWidths[highestWidthIndex]);

                breaker++;
            }
        }
    }

    private static int findLargestIndex(int[] widths) {
        int largestIndex = 0;
        int largestValue = 0;

        for (int i = 0; i < widths.length; i++) {
            if (widths[i] > largestValue) {
                largestIndex = i;
                largestValue = widths[i];
            }
        }

        return largestIndex;
    }

    private static int sum(int[] widths) {
        int sum = 0;

        for (int width : widths) {
            sum += width;
        }

        return sum;
    }

    public ResultSet getResultSet() {
        return rs;
    }

    public void setResultSet(ResultSet rs) {
        this.rs = rs;
    }

}
