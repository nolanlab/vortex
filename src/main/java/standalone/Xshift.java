package standalone;

import vortex.clustergraph.MSTBuilder;
import sandbox.clustering.AngularDistance;
import sandbox.clustering.ClusterSet;
import sandbox.clustering.Dataset;
import sandbox.dataIO.DatasetStub;
import sandbox.dataIO.ImportConfigObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import vortex.dataIO.GMLWriter;

import org.gephi.graph.api.UndirectedGraph;
import util.IO;
import util.logger;
import vortex.clustering.XShiftClustering;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Nikolay
 */
public class Xshift {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        int K = 20;
        
        try {
            if (args.length > 0) {
                if (args[0].trim().equals("auto")) {
                    K = 20;
                } else {
                    K = Integer.parseInt(args[0].trim());
                }
            }
        } catch (Exception e) {
            logger.print("Error parsing arguments. \nUsage: java -jar X-shift.jar [NUM_NEAREST_NEIGHBORS(default=20)|auto]");
        }

        /*String CONFIG_FILE_PATH = "C:\\Users\\Nikolay\\YandexDisk\\Working folder\\Cytobank\\X-shift impl for Cytobank\\config.txt";
        String FCS_FILE_LIST_PATH = "C:\\Users\\Nikolay\\YandexDisk\\Working folder\\Cytobank\\X-shift impl for Cytobank\\fcsFileList.txt";*/
        String CONFIG_FILE_PATH = "importConfig.txt";
        String FCS_FILE_LIST_PATH = "fcsFileList.txt";

        String OUTPUT_PATH = "out";

        File f = new File(OUTPUT_PATH + File.separator);

        if (!f.exists()) {
            f.mkdirs();
        }

        //2. Read FCS File List
        ArrayList<String> fcsFilePaths = null;
        try {
            fcsFilePaths = IO.getListOfStringsFromStream(new FileInputStream(FCS_FILE_LIST_PATH));
        } catch (IOException e) {
            logger.print("Error reading the list of FCS files. Exiting");
            logger.print("Reason: " + e.toString() + ", " + e.getMessage());
            for (StackTraceElement st : e.getStackTrace()) {
                logger.print(st.toString());
            }
            System.exit(2);
        }

        DatasetStub[] stubs = new DatasetStub[fcsFilePaths.size()];

        try {
            for (int i = 0; i < stubs.length; i++) {
                stubs[i] = DatasetStub.createFromFCS(new File(fcsFilePaths.get(i)));

                logger.print("Reading " + stubs[i].getFileName() + ". Num Events:" + stubs[i].getRowCount());
            }
        } catch (Exception e) {
            logger.print("Error reading one of the FCS files. Exiting");
            logger.print("Reason: " + e.toString() + ", " + e.getMessage());
            for (StackTraceElement st : e.getStackTrace()) {
                logger.print(st.toString());
            }
            System.exit(1);
        }

        //1. Read config
        ImportConfigObject config = null;

        try {
            config = ImportConfigObject.readFromFile(new File(CONFIG_FILE_PATH));
            config.sideColumnNames = new String[0];
            logger.print("datasetName", config.datasetName);
            logger.print("euclidean_len_ths", config.euclidean_len_ths);

            String[] s = config.featureColumnNames[0].trim().split(",");

            int[] idx = new int[s.length];

            for (int i = 0; i < idx.length; i++) {
                idx[i] = Integer.parseInt(s[i]);
            }

            config.featureColumnNames = new String[idx.length];

            logger.print("Clustering columns");

            for (int i = 0; i < idx.length; i++) {
                config.featureColumnNames[i] = stubs[0].getShortColumnNames()[idx[i] - 1];
                logger.print(idx[i] + "=" + stubs[0].getLongColumnNames()[idx[i] - 1] + "|" + stubs[0].getShortColumnNames()[idx[i] - 1]);
            }

            logger.print("Clustering columns", Arrays.toString(config.featureColumnNames));

            logger.print("limitRowsPerFile", config.limitRowsPerFile);
            logger.print("noise_threshold", config.noise_threshold);
            logger.print("quantile", config.quantile);
            logger.print("rescale", config.rescale);
            logger.print("rescaleSeparately", config.rescaleSeparately);
            logger.print("scaling_factor", config.scaling_factor);
            logger.print("transf", config.transf);
        } catch (IOException e) {
            logger.print("Error reading config file. Exiting");
            logger.print("Reason: " + e.toString() + ", " + e.getMessage());
            for (StackTraceElement st : e.getStackTrace()) {
                logger.print(st.toString());
            }
            System.exit(2);
        }

        Dataset nd = null;
        try {
            nd = sandbox.dataIO.DatasetImporter.importDataset(stubs, config);
        } catch (Exception e) {
            logger.print("Error importing the dataset. Exiting");
            logger.print("Reason: " + e.toString() + ", " + e.getMessage());
            for (StackTraceElement st : e.getStackTrace()) {
                logger.print(st.toString());
            }
            System.exit(3);
        }

        XShiftClustering clus = new XShiftClustering(new AngularDistance(), nd);
        clus.setSave(false);

        ClusterSet cs;

        try {
            if (K < 3) {
                logger.print("dim" + nd.getNumDimensions());
                int max = (int) (2890 * Math.pow(nd.getNumDimensions(), -0.8));
                int min = (int) (1942 * Math.pow(nd.getNumDimensions(), -1.61));

                logger.print("Clustering from K=" + min + " to K=" + max + " with 30 steps ");
                cs = clus.doAutomaticClustering(nd, max, min, 30);
            } else {
                logger.print("Clustering wiht K=" + K);
                clus.setK(new Integer[]{K});
                cs = clus.doBatchClustering(nd, "")[0];
            }

            try {
                logger.print("Exporting FCS files");
                sandbox.dataIO.ClusterSetToFCSExporterWithNNReassignment.exportClusterSet(cs, fcsFilePaths.toArray(new String[fcsFilePaths.size()]), new File(OUTPUT_PATH), config);
            } catch (Exception e) {
                logger.print("Error exporting the data. Exiting");
                logger.print("Reason: " + e.toString() + ", " + e.getMessage());
                for (StackTraceElement st : e.getStackTrace()) {
                    logger.print(st.toString());
                }
                System.exit(6);
            }
            UndirectedGraph mst = null;
            try {
                logger.print("Building MST Graph");
                mst = (new MSTBuilder()).buildGraph(cs.getClusters(), false);
            } catch (Exception e) {
                logger.print("Error building the graph. Exiting");
                logger.print("Reason: " + e.toString() + ", " + e.getMessage());
                for (StackTraceElement st : e.getStackTrace()) {
                    logger.print(st.toString());
                }
                System.exit(7);
            }

            try {
                logger.print("Writing MST Graph");
                GMLWriter.writeGML(mst, new File(OUTPUT_PATH + File.separator + "mst.gml"));
            } catch (Exception e) {
                logger.print("Error writing the graph data. Exiting");
                logger.print("Reason: " + e.toString() + ", " + e.getMessage());
                for (StackTraceElement st : e.getStackTrace()) {
                    logger.print(st.toString());
                }
                System.exit(8);
            }

        } catch (Exception e) {
            logger.print("Error in clustering. Exiting");
            logger.print("Reason: " + e.toString() + ", " + e.getMessage());
            for (StackTraceElement st : e.getStackTrace()) {
                logger.print(st.toString());
            }
            System.exit(5);
        }
    }
}
