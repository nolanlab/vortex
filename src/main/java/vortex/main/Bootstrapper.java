/*
 * Bootstrapper.java
 *
 * Created on December 11, 2007, 1:09 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package vortex.main;

import cern.colt.matrix.linalg.Algebra;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

/**
 *
 * @author Nikolay
 */
public class Bootstrapper {

    umontreal.iro.lecuyer.randvarmulti.MultinormalCholeskyGen MultiVarGen;

    /**
     * Creates a new instance of Bootstrapper
     */
    public Bootstrapper(java.sql.Connection con) {
        /*
         double [] nullArrayList = new double[MahalonobisSpace.getCovarianceMatrix().columns()];
         for(double f:nullArrayList)f = 0;
        
         this.MultiVarGen = new umontreal.iro.lecuyer.randvarmulti.MultinormalCholeskyGen(
         new umontreal.iro.lecuyer.randvar.NormalGen(
         new umontreal.iro.lecuyer.rng.GenF2w32(),
         new umontreal.iro.lecuyer.probdist.NormalDist(0,1))
         , MahalonobisSpace.getCovarianceMatrix().columns()
         );
         this.MultiVarGen.setSigma(MahalonobisSpace.getCovarianceMatrix());
        
         }
    
         public double[] bootstrap_measurement(double[] measurements) throws java.lang.IllegalArgumentException{
        
         if (measurements.length !=MultiVarGen.getDimension()){
         throw new java.lang.IllegalArgumentException("Length of measurement array (" + String.valueOf(measurements.length) + ") doesn't match random number generator dimension (" + String.valueOf(MultiVarGen.getDimension()) + ")");
         }
         double [] rndPoints = new double[measurements.length];
         this.MultiVarGen.setMu(measurements);
         MultiVarGen.nextPoint(rndPoints);
         return rndPoints;
         */
    }
}
