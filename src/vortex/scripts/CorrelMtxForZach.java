/*
 * Copyright (C) 2015 Nikolay
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

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import main.Dataset;
import util.Correlation;
import util.MatrixOp;
import vortex.util.Config;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public class CorrelMtxForZach {
    static String[] dsNames = new String[]{
        "Cy",
        "Hu",
        "Rh"};
    
    public static void main(String[] args) throws SQLException, IOException {
        do {

            if (Config.getDefaultDatabaseHost() == null) {
                ConnectionManager.showDlgSelectDatabaseHost();
            }
            if (Config.getDefaultDatabaseHost() == null) {
                System.exit(0);
            }
            ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());

        } while (ConnectionManager.getDatabaseHost() == null);
        
        for (int i = 0; i < dsNames.length; i++) {
            String dsName = dsNames[i];
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
             double[][] vec = Algebra.DEFAULT.transpose(new DenseDoubleMatrix2D(getVectors(ds))).toArray();
             DenseDoubleMatrix2D corr = new DenseDoubleMatrix2D(vec.length, vec.length);
             
             long time = Calendar.getInstance().getTimeInMillis();
             System.err.println();
             for (int j = 0; j < vec.length; j++) {
                 for (int k = j; k < vec.length; k++) {
                     if(k==j){
                         corr.set(j, k, 1);
                         continue;
                     }
                     double c = Correlation.getUncenteredCorrelation(vec[j], vec[k]);
                     corr.set(j, k, c);
                     corr.set(k, j, c);
                 }
            }
             System.err.println(Calendar.getInstance().getTimeInMillis()-time);
             System.out.println(ds.getName());
             System.out.println(Arrays.toString(ds.getFeatureNamesCombined()));
             System.out.println(corr);
        }
    }
    
    public static double[][] getVectors(Dataset ds){
    double[][] res = new double[ds.getDatapoints().length][];
        for (int i = 0; i < res.length; i++) {
           res[i]= MatrixOp.concat(ds.getDatapoints()[i].getVector(), ds.getDatapoints()[i].getSideVector());
        }
        return res;
    }
    
}
