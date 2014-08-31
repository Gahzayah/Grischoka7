/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package grischa.dev.moka7.asyn;

/**
 *
 * @author MaHi
 */
public class ServiceCallback implements Runnable{
    private boolean[] array;
    private S7Listener h;
    
    
    @Override
    public void run() {
       int result = 0;
       
       for(int i = 0; i <= 12;i++){
           // Wenn die Threads beenden result zählt rauf
           if(array[i]){
               result++;
           }
       }
       // Wenn die updateS7 die zu erwarteten Anruf
       h.updateS7(result);
    }


    
}
