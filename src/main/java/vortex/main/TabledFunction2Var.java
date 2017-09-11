/*
 * TabledFunction2Var.java
 *
 * Created on January 18, 2008, 6:57 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package vortex.main;

/**
 *
 * @author Nikolay
 */
import java.io.Serializable;
import java.io.*;
import util.logger;

public class TabledFunction2Var implements Serializable {

    double[] argX;
    double[] argY;
    double[][] tableValues;
    double minX, minY, maxX, maxY;
    private static final long serialVersionUID = 7526471155622756179L;

    private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
        //always perform the default de-serialization first
        aInputStream.defaultReadObject();

    }

    private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }

    public TabledFunction2Var(double[] argX, double[] argY, double[][] tableValues, double minX, double minY, double maxX, double maxY) {

        this.argX = argX;
        this.argY = argY;
        this.tableValues = tableValues;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double get(double x, double y) throws IllegalArgumentException {
        if (x < minX || x > maxX) {
            throw new IllegalArgumentException("First argument is out of range,  value = " + String.valueOf(x));
        }
        if (y < minY || y > maxY) {
            throw new IllegalArgumentException("Second argument is out of range,  value = " + String.valueOf(y));
        }

        int indX = 0;
        int indY = 0;

        while (x >= argX[indX + 1] && indX < argX.length - 2) {
            indX++;
        }

        while (y >= argY[indY + 1] && indY < argY.length - 2) {
            indY++;
        }

        //linearly interpolating the result on basis of the table
        double interpX1 = ((tableValues[indX + 1][indY] - tableValues[indX][indY]) * ((x - argX[indX]) / (argX[indX + 1] - argX[indX]))) + tableValues[indX][indY];
        double interpX2 = ((tableValues[indX + 1][indY + 1] - tableValues[indX][indY + 1]) * ((x - argX[indX]) / (argX[indX + 1] - argX[indX]))) + tableValues[indX][indY + 1];
        double interpY = ((interpX2 - interpX1) * (y - argY[indY])) + interpX1;

        if (Double.isNaN(interpY)) {
            logger.print(x + ", " + y);
        }
        if (interpY < 0) {
            logger.print(x + ", " + y);
        }
        return interpY;
    }
}
