/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui;

import java.util.EventObject;
import sandbox.clustering.Dataset;


/**
 *
 * @author Nikolay
 */
  public class DatasetDeletionEvent extends EventObject{

    public DatasetDeletionEvent(Dataset source) {
        super(source);
    }

    @Override
    public Dataset getSource() {
        return (Dataset)super.getSource(); //To change body of generated methods, choose Tools | Templates.
    }
    
      
    }

