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

public class TabledFunction1Var implements Serializable {

    double[] X;
    double[] Y;
    private static final long serialVersionUID = 5526471155622776147L;

    private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
        //always perform the default de-serialization first
        aInputStream.defaultReadObject();

    }

    /**
     * This is the default implementation of writeObject. Customise if
     * necessary.
     */
    private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }

    public TabledFunction1Var(double[] X, double[] Y) throws IllegalArgumentException {
        if (X.length != Y.length) {
            throw new IllegalArgumentException("Data ArrayLists are not of equal size, X is " + String.valueOf(X.length) + ", Y is " + String.valueOf(Y.length));
        }

        this.X = X;
        this.Y = Y;

    }

    public double get(double x) throws IllegalArgumentException {

        if (x < X[0] || x > X[X.length - 1]) {
            throw new IllegalArgumentException("Argument is out of range,  value = " + String.valueOf(x));
        }

        int indX = 0;

        while (X[indX] < x) {
            indX++;
        }
        indX--;

        //linearly interpolating the result on basis of the table
        double interpX = ((Y[indX + 1] - Y[indX]) * (x - (double) X[indX]) / (X[indX + 1] - X[indX])) + Y[indX];

        return interpX;
    }

    @Override
    public String toString() {
        String tmp = "";
        for (double f : X) {
            tmp += String.valueOf(f) + "\t";
        }
        tmp += "\n";

        for (double f : Y) {
            tmp += String.valueOf(f) + "\t";
        }
        tmp += "\n";

        return tmp;
    }
}
