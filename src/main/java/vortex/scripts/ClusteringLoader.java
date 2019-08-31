/*
 * Copyright (C) 2019 Nikolay Samusik and Stanford University
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import sandbox.clustering.EuclideanDistance;
import sandbox.clustering.Cluster;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Datapoint;
import sandbox.clustering.Dataset;
import sandbox.dataIO.DatasetStub;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class ClusteringLoader {

    public static ClusterSet[] loadFromSPADE(File dir, Dataset d, int[] freeParam) throws FileNotFoundException, IOException {

        ClusterSet[] cs = new ClusterSet[freeParam.length];
        for (int i = 0; i < freeParam.length; i++) {

            int currParam = freeParam[i];
            //logger.print("loading " +  currParam);
            List<Datapoint>[] clusters = new List[currParam];
            DatasetStub fcs = DatasetStub.createFromFCS(new File(dir.getPath() + File.separator + currParam + File.separator + d.getName() + "_Ungated.fcs.density.fcs.cluster.fcs"));

            int clusterChannelIdx = -1;

            for (int j = 0; j < fcs.getShortColumnNames().length; j++) {
                if (fcs.getShortColumnNames()[j].equalsIgnoreCase("cluster")) {
                    clusterChannelIdx = j;
                    break;
                }
            }
            
            for (int j = 0; j < fcs.getRowCount(); j++) {
                int cid = (int) fcs.getRow(j)[clusterChannelIdx] - 1;
                if (Integer.parseInt(d.getDatapoints()[j].getFullName().split("Event")[1].trim()) != j) {
                    logger.print("Error: " + d.getDatapoints()[j].getFullName() + " doesnt match " + j);
                }
                //logger.print(j, cid);
                if (clusters[cid] == null) {
                    clusters[cid] = new LinkedList<>();
                }
                clusters[cid].add(d.getDatapoints()[j]);
            }
            ArrayList<Cluster> cl = new ArrayList<>();
            for (int j = 0; j < clusters.length; j++) {
                if (clusters[j] == null) {
                    logger.print("clusters[j] == null, j =" + j);
                } else {
                    cl.add(new Cluster(clusters[j].toArray(new Datapoint[clusters[j].size()]), new double[d.getNumDimensions()], new double[d.getSideVarNames().length], "", new EuclideanDistance()));
                }
            }
            cs[i] = new ClusterSet(0, d, cl.toArray(new Cluster[cl.size()]), new EuclideanDistance(), "SPADE", "NumNodes" + currParam, currParam, "");
        }
        return cs;
    }
}
