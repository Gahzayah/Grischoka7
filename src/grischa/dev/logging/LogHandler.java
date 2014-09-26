/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package grischa.dev.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author mhi
 */
public class LogHandler {
    
    Logger logger = Logger.getLogger("root");

    Handler dbhandler = new Handler() {

        @Override
        public void publish(LogRecord record) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void close() throws SecurityException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
    
    public void doasa(){
        logger.addHandler(dbhandler);
    }
    
}
