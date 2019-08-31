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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import sandbox.annotations.Annotation;
import sandbox.clustering.AngularDistance;
import sandbox.clustering.EuclideanDistance;
import sandbox.clustering.ClusterMember;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Dataset;
import vortex.util.Config;
import vortex.util.ConnectionManager;
import util.logger;
import vortex.main.ClusterSetCache;

/**
 *
 * @author Nikolay
 */
public class ExportStrings {
    static String[] dsNames = new String[30];
    static{
        for (int i = 1; i <= dsNames.length; i++) {
           dsNames[i-1] = String.format("%03d",i);
        }
    }
   
    public static void main(String [] args) throws Exception  {
        do {
                if (Config.getDefaultDatabaseHost() == null) {
                    ConnectionManager.showDlgSelectDatabaseHost();
                }
                if (Config.getDefaultDatabaseHost() == null) {
                    System.exit(0);
                }
                ConnectionManager.setDatabaseHost(Config.getDefaultDatabaseHost());
            
        } while (ConnectionManager.getDatabaseHost() == null);
        for (String dsName : dsNames) {
            Dataset ds = ConnectionManager.getStorageEngine().loadDataset(dsName);
            if (ds == null) {
                continue;
            }
            logger.print("Exporting dataset:" + ds);
        try {
            File f = new File("C:\\Users\\Nikolay\\Local Working Folder\\FlowCapI\\X-shift NDD");
            int [] css = ConnectionManager.getStorageEngine().getClusterSetIDs(ds.getID());
            for (int csID : css) {
                ClusterSet cs = ClusterSetCache.getInstance(ds, csID);
                logger.print(cs);
                if(cs.getMainClusteringParameterValue()!=125) continue;
                if(!(cs.getDistanceMeasure() instanceof AngularDistance)) continue;
                File dir = new File(f.getPath());
                dir.mkdirs();
                Annotation ann = null;
                for (Annotation a : ds.getAnnotations()) {
                    if(a.getAnnotationName().startsWith("Auto")) {
                        ann = a;
                    }
                }
                if (ann == null) {
                    return;
                }
                for (String term : ann.getTerms()) {
                    int[] pidT = ann.getDpIDsForTerm(term);
                    int[] cidArray = new int[pidT.length];
                    File out = new File(dir.getPath() + File.separator + term + "_assgnments.txt");
                    for (int cid = 0; cid < cs.getClusters().length; cid++) {
                        for (ClusterMember cm : cs.getClusters()[cid].getClusterMembers()) {
                            String[] s = cm.getDatapointName().split(" Event ");
                            if (s.length > 1) {
                                if (s[0].equals(term)) {
                                    cidArray[Integer.parseInt(s[1])] = cid;
                                }
                            } else {
                                cidArray[cm.getDatapoint().getID()] = cid;
                            }
                        }
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(out));
                    for (int i = 0; i < cidArray.length; i++) {
                        bw.write(cidArray[i] + 1 + "\n");
                    }
                    bw.flush();
                    bw.close();
                }
            }
        } catch (Exception e) {
            logger.showException(e);
        }
        }
    }
}
