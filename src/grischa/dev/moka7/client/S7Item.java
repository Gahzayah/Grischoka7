/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package grischa.dev.moka7.client;

/**
 *
 * @author mhi
 */
public class S7Item {
    
    private int DBNumber;
    private int start;
    private int length;


    public S7Item(int DBNumber, int start, int length) {
        this.DBNumber = DBNumber;
        this.start = start;
        this.length = length;
    }

    public int getDBNumber() {
        return DBNumber;
    }

    public void setDBNumber(int DBNumber) {
        this.DBNumber = DBNumber;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    
    
}
