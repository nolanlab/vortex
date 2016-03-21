/*
 * Copyright (C) 2014 Nikolay
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package vortex.scripts;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.io.IOException;
import java.sql.SQLException;
import umontreal.iro.lecuyer.probdistmulti.MultiNormalDist;
import umontreal.iro.lecuyer.randvar.NormalGen;
import umontreal.iro.lecuyer.randvarmulti.MultinormalCholeskyGen;
import umontreal.iro.lecuyer.randvarmulti.RandomMultivariateGen;
import umontreal.iro.lecuyer.rng.MRG32k3a;
import clustering.Datapoint;
import main.Dataset;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;




/**
 *
 * @author Nikolay
 */
public class GenerateSynthDatasetSeparation {
    static int dim = 40;
    static int num_datapoints = 100;
    static double separation_distance = 5.0;
    static double sparsity_factor = 0.0;
    
    static   DenseDoubleMatrix1D[] mu = new DenseDoubleMatrix1D[2];
    static   DenseDoubleMatrix2D[] sigma = new DenseDoubleMatrix2D[2];
    static   RandomMultivariateGen[] gen = new RandomMultivariateGen[2];
    static   MultiNormalDist[] dist = new MultiNormalDist[2];
     
     public static void main(String[] args){
         
         do {
            try {
                if (Config.getDefaultDatabaseHost() == null) {
                    ConnectionManager.showDlgSelectDatabaseHost();
                }
                if (Config.getDefaultDatabaseHost() == null) {
                    System.exit(0);
                }
                ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());
            } catch (SQLException | IOException e) {
                logger.showException(e);
                ConnectionManager.showDlgSelectDatabaseHost();
            }
        } while (ConnectionManager.getDatabaseHost() == null);
        
        double coord_l =separation_distance/Math.sqrt(dim);
         
        mu[0] = new DenseDoubleMatrix1D(new double[dim]);
        mu[1] = new DenseDoubleMatrix1D(new double[dim]);
        
         for (int i = 0; i < dim; i++) {
             mu[1].setQuick(i, coord_l);
         }
         
         for (int i = 0; i < 2; i++) {
           
            sigma[i] = new DenseDoubleMatrix2D(dim, dim);
            for (int j = 0; j < dim; j++) {
                sigma[i].setQuick(j, j, Math.random()>sparsity_factor?1:0.00001);
            }
            gen[i] = /*new  MultinomialStudentGenerator(mu[i].toArray(), sigma[i]);*/ new MultinormalCholeskyGen(new NormalGen(new MRG32k3a()), mu[i].toArray(), sigma[i]);
            dist[i] = new MultiNormalDist(mu[i].toArray(), sigma[i].toArray());
        }
         
        Datapoint[] d = new Datapoint[num_datapoints*2];
        int cnt = 0;
         for (int i = 0; i < num_datapoints; i++) {
            double [] p = new double[dim];
            gen[0].nextPoint(p);
            d[cnt] = new Datapoint("comp0_"+i, p, cnt);
            cnt++;
         }
         for (int i = 0; i < num_datapoints; i++) {
            double [] p = new double[dim];
            gen[1].nextPoint(p);
            d[cnt] = new Datapoint("comp1_"+i, p, cnt);
            cnt++;
         }
         String [] colNames = new String[dim];
         
         for (int i = 0; i < colNames.length; i++) {
            colNames[i] = "col"+i;
         }
         
         Dataset ds = new Dataset("synth_2comp_"+dim+"dim_"+separation_distance+"sep_"+num_datapoints+"dp_"+sparsity_factor+"sparsity", d, colNames);
         try{
             ConnectionManager.getStorageEngine().saveDataset(ds, true);
             ConnectionManager.getStorageEngine().shutdown();
         }catch(SQLException e){
             logger.print(e);
         }
     }               
}
