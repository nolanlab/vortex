/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataIO;

import clustering.Cluster;
import clustering.ClusterMember;
import clustering.ClusterSet;
import clustering.Dataset;
import flowcyt_fcs.ExportFCS;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import org.cytobank.fcs_files.events.FcsFile;
import org.cytobank.fcsconcat.FCSChannel;
import org.cytobank.fcsoutput.FCSParameterList;
import util.MatrixOp;
import util.Shuffle;

/**
 *
 * @author Nikolay
 */
public class ClusterSetToFCSExporterWithNNReassignment {

    private final static int MAX_CLUSTER_SAMPLE_SIZE = 1000;

    public static void exportClusterSet(ClusterSet cs, String[] fcsFilePaths, File out, DatasetStub[] stubs, ImportConfigObject ico) throws Exception {

        for (int idx = 0; idx < fcsFilePaths.length; idx++) {
            FcsFile fcsFile = new FcsFile(fcsFilePaths[idx]);

            ico.limitRowsPerFile = (int) stubs[idx].getRowCount();
            Dataset dsFullFile = DatasetImporter.importDataset(new DatasetStub[]{stubs[idx]}, ico);

            ExportFCS writer = new ExportFCS();

            Dataset ds = cs.getDataset();
            FCSParameterList pl = new FCSParameterList();
            String[] s = ds.getFeatureNamesCombined();

            
            String [] spn = Arrays.copyOf(fcsFile.channelShortname, fcsFile.channelShortname.length+1);
            
            String [] lpn = Arrays.copyOf(fcsFile.channelName, fcsFile.channelName.length+1);
            
            lpn[fcsFile.channelShortname.length] = "cluster_id";
            spn[fcsFile.channelShortname.length] = "cluster_id";
            
            int chIDX = 1;
            for (int i = 0; i < fcsFile.getChannelCount(); i++) {
                pl.addChannel(new FCSChannel(chIDX++, fcsFile.channelShortname[i], fcsFile.channelName[i]));
            }

            pl.addChannel(new FCSChannel(chIDX, "ClusterID", cs.toString()));

            int[] clusterIDmap = new int[(int) fcsFile.getEventCount()];
            int[] clusterDensityMap = new int[(int) fcsFile.getEventCount()];
            for (int i = 0; i < clusterIDmap.length; i++) {
                clusterIDmap[i] = -1;
            }
            
            int id = 0;

            for (Cluster c : cs.getClusters()) {
                c.setID(id++);
                for (ClusterMember cm : c.getClusterMembers()) {

                    String filename = fcsFile.getName().trim();

                    if (filename.endsWith(".fcs")) {
                        filename = filename.substring(0, filename.length() - 4);
                    }

                    if (cm.getDatapoint().getFilename().trim().equals(fcsFile.getName().trim().replace(".fcs", ""))) {
                        int evtidx = cm.getDatapoint().getIndexInFile();
                        clusterIDmap[evtidx] = c.getID();
                        clusterDensityMap[evtidx] = c.size();
                    }
                }
            }
            Shuffle<double[]> sh = new Shuffle<>();

            HashMap<Cluster, double[][]> clusRepVectors = new HashMap<>();
            for (Cluster c : cs.getClusters()) {
                double[][] vec = new double[c.size()][];
                for (int i = 0; i < vec.length; i++) {
                    vec[i] = c.getClusterMembers()[i].getDatapoint().getVector();
                }
                sh.shuffleArray(vec);
                clusRepVectors.put(c, Arrays.copyOf(vec, Math.min(vec.length, MAX_CLUSTER_SAMPLE_SIZE)));
            }

            for (int i = 0; i < clusterDensityMap.length; i++) {
                if (i % 1000 == 0) {
                    System.err.println("remapping " + i);
                }
                if (clusterIDmap[i] < 0) {
                    double[] dp = dsFullFile.getDatapoints()[i].getVector();
                    double maxSim = -1;
                    Cluster closestCluster = null;
                    for (Cluster c : cs.getClusters()) {
                        double[][] vec = clusRepVectors.get(c);
                        for (double[] v : vec) {
                            double cos = MatrixOp.getEuclideanCosine(v, dp);
                            if (cos > maxSim) {
                                maxSim = cos;
                                closestCluster = c;
                            }
                        }
                    }
                    clusterIDmap[i] = closestCluster.getID();
                    clusterDensityMap[i] = closestCluster.size();
                }
            }

            //double[][] inEvents = fcsFile.getEventCount();
            float[][] events = new float[(int)fcsFile.getEventCount()][fcsFile.getChannelCount() + 1];

            for (int i = 0; i < fcsFile.getEventCount(); i++) {
                for (int j = 0; j < fcsFile.getChannelCount(); j++) {
                    events[i][j] = (float)fcsFile.getChannels().getEvent(j,i);
                }
                events[i][events[i].length - 1] = clusterIDmap[i];
            }
            
            writer.writeFCSAsFloat(out +File.separator + fcsFile.getName(), events, spn, lpn);
        }
    }
}
