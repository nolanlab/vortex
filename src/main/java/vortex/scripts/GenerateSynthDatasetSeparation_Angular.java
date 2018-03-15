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
import umontreal.iro.lecuyer.rng.LFSR113;
import clustering.EuclideanDistance;
import vortex.clustering.XShiftClustering;
import clustering.Datapoint;
import clustering.Dataset;
import util.Shuffle;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class GenerateSynthDatasetSeparation_Angular {

    static int dim = 38;
    static int num_datapoints = 1000;
    static double separation_distance = 2.57;
    static int effective_num_dim = 11;

    static DenseDoubleMatrix1D[] mu = new DenseDoubleMatrix1D[2];
    static DenseDoubleMatrix2D[] sigma = new DenseDoubleMatrix2D[2];
    static RandomMultivariateGen[] gen = new RandomMultivariateGen[2];
    static MultiNormalDist[] dist = new MultiNormalDist[2];

    public static void main(String[] args) {

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

        //double coord_l =separation_distance/Math.sqrt(dim);
        mu[0] = new DenseDoubleMatrix1D(new double[dim]);
        mu[1] = new DenseDoubleMatrix1D(new double[dim]);

        for (int i = 0; i < dim; i++) {
            double val = Math.random() * 3;
            mu[1].setQuick(i, val);
            mu[0].setQuick(i, val);
        }

        mu[0].set(0, 0);
        mu[1].set(0, separation_distance);// / Math.sqrt(2));
        
        Boolean [] sparsity_ArrayList = new Boolean[dim-1];
        
        for (int i = 0; i < sparsity_ArrayList.length; i++) {
            sparsity_ArrayList[i] = i < effective_num_dim;
        }
        
        (new Shuffle<Boolean>()).shuffleArray(sparsity_ArrayList);

        //mu[0].set(1, separation_distance / Math.sqrt(2));
        //mu[1].set(1, 0);
        for (int i = 0; i < 2; i++) {
            sigma[i] = new DenseDoubleMatrix2D(dim, dim);
        }
        for (int j = 0; j < dim; j++) {
            boolean sparse = !(j > 0?sparsity_ArrayList[j-1]:true);
            for (int i = 0; i < 2; i++) {
                if (sparse) {
                    mu[i].setQuick(j, 0);
                    sigma[i].setQuick(j, j, 0.000001);
                } else {
                    sigma[i].setQuick(j, j, 1);
                }
            }
        }
        
        for (int i = 0; i < 2; i++) {
            gen[i] = /*new  MultinomialStudentGenerator(mu[i].toArray(), sigma[i]);*/ new MultinormalCholeskyGen(new NormalGen(new LFSR113()), mu[i].toArray(), sigma[i]);
            dist[i] = new MultiNormalDist(mu[i].toArray(), sigma[i].toArray());
        }

        Datapoint[] d = new Datapoint[num_datapoints * 2];
        int cnt = 0;
        for (int i = 0; i < num_datapoints; i++) {
            double[] p = new double[dim];
            gen[0].nextPoint(p);
            d[cnt] = new Datapoint("comp0_" + i, p, cnt);
            cnt++;
        }
        for (int i = 0; i < num_datapoints; i++) {
            double[] p = new double[dim];
            gen[1].nextPoint(p);
            d[cnt] = new Datapoint("comp1_" + i, p, cnt);
            cnt++;
        }
        String[] colNames = new String[dim];

        for (int i = 0; i < colNames.length; i++) {
            colNames[i] = "col" + i;
        }

        Dataset ds = new Dataset("eucl4_oneMarker_synth_2comp_" + dim + "dim_" + separation_distance + "sep_" + num_datapoints + "dp_" + effective_num_dim + "effDim", d, colNames);
        Integer[] K = new Integer[27];
        int cnt2 = 0;
        for (int i = 30; i > 3; i--) {
            K[cnt2++] = i;
        }

        try {
            ConnectionManager.getStorageEngine().saveDataset(ds, true);
            XShiftClustering clus = new XShiftClustering(new EuclideanDistance(),ds);
            clus.setK(K);
            clus.doBatchClustering(ds, "NULL");
            ConnectionManager.getStorageEngine().shutdown();
            Config.setDatasetIDsForLoading(new String[]{ds.getName()});
        } catch (SQLException e) {
            logger.print(e);
        }
    }
}
