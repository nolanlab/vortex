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

import java.io.IOException;
import java.sql.SQLException;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import org.gephi.graph.api.Node;
import org.openide.util.Pair;
import util.MatrixOp;
import util.Shuffle;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;
import vortex.clustergraph.panScFDL;

/**
 *
 * @author Nikolay
 */
public class tSNE_dist_mtx {

  
    public static void main(String[] args) throws SQLException{

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
            
        
        Dataset orig = ConnectionManager.getStorageEngine().loadDataset("BM2_cct_normalized_07_non-Neutrophils_Ungated_viSNE_Ungated");
        Dataset tSNE = ConnectionManager.getStorageEngine().loadDataset("BM2_cct_normalized_07_non-Neutrophils_Ungated_viSNE_Untransformed");
        
        
        int k = 0;
        
        int numCells = 100;

        //List<Datapoint> dp = Arrays.asList(nodes).stream().map((c)->cid.get((int) c.getAttribute("clusterNode")).dp).collect(Collectors.toList());
        
        double [][] origLen = new double[orig.getDatapoints().length][numCells];
        double [][] embLen = new double[orig.getDatapoints().length][numCells];
        
        Pair<Datapoint,Datapoint> [] zip = new Pair[orig.getDatapoints().length];
        
        for (int i = 0; i < zip.length; i++) {
            zip[i] = Pair.of(orig.getDatapoints()[i], tSNE.getDatapoints()[i]) ;
        }
        
        Shuffle<Pair<Datapoint,Datapoint>> sh = new Shuffle<>();
        zip = sh.shuffleCopyArray(zip);
        
        for (Pair<Datapoint,Datapoint> n : Arrays.copyOf(zip, numCells)) {
            
           
            int t =0;
            for (Pair<Datapoint,Datapoint> m2 : zip) {
                origLen[t][k] = MatrixOp.getEuclideanDistance(n.first().getVector(), m2.first().getVector());
                embLen[t][k] = MatrixOp.getEuclideanDistance(n.second().getVector(), m2.second().getVector());
                t++;
            }
            k++;
        }
        
  
        try{
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("C:\\Users\\Nikolay Samusik\\Dropbox\\Diseffusion\\Nikolay analysis\\BM7_Eucl_scFDL_orig_dist_forTSNE_"+numCells+".csv")));
        bw.write(MatrixOp.toCSV(origLen));
        bw.flush();
        bw.close();
        bw = new BufferedWriter(new FileWriter(new File("C:\\Users\\Nikolay Samusik\\Dropbox\\Diseffusion\\Nikolay analysis\\BM7_Eucl_scFDL_embed_dist_forTSNE_"+numCells+".csv")));
        bw.write(MatrixOp.toCSV(embLen));
        bw.flush();
        bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        
        
    }
}
