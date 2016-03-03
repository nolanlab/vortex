/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vss;

import java.sql.SQLException;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.ClusterSet;
import clustering.Datapoint;
import main.Dataset;
import vortex.main.VoronoiCell;
import annotations.Annotation;
import clustering.DistanceMatrix;
import clustering.DistanceMeasure;
import vortex.clustering.Kernel;

/**
 *
 * @author Nikolay
 */
public interface StorageEngine {

    public Dataset[] loadDatasets(int[] datasetIDs) throws SQLException;

    public Dataset[] loadDatasets() throws SQLException;

    public void shutdown();

    public int getNextClusterSetBatchID(int datasetID) throws SQLException;

    public Annotation[] loadAnnotations(Dataset ds) throws SQLException;

    public Annotation loadAnnotation(int annotationID, Dataset ds) throws SQLException;

    public void saveAnnotation(Annotation ann) throws SQLException;

    public void deleteAnnotation(Annotation ann) throws SQLException;

    public String[] getAvailableDatasetNames() throws SQLException;

    public Dataset loadDataset(int datasetID) throws SQLException;

    public Dataset loadDataset(String datasetName) throws SQLException;

    public int saveDataset(Dataset ds, boolean deep) throws SQLException;

    public void deleteDataset(Dataset ds) throws SQLException;

    public void saveDistanceMatrix(DistanceMatrix dm) throws SQLException;

    public DistanceMatrix loadDistanceMatrix(Dataset ds) throws SQLException;

    public void deleteDistanceMatrix(Dataset ds) throws SQLException;

    public Datapoint[] loadDatapoints(int datasetID) throws SQLException;

    public void saveDatapoints(int datasetID, Datapoint[] dp) throws SQLException;

    public Cluster[] loadClustersForClusterSet(ClusterSet cs) throws SQLException;

    public void clearPrecomputedData(int datasetID) throws SQLException;

    public ClusterSet loadClusterSet(int clusterSetID, Dataset nd) throws SQLException;

    public int[] getClusterSetIDs(int datasetID) throws SQLException;

    public void saveClusterSet(ClusterSet cs, boolean deep) throws SQLException;

    public void deleteClusterSet(int csid) throws SQLException;

    public Cluster loadCluster(int clusterID, ClusterSet cs) throws SQLException;

    public void saveCluster(Cluster c, boolean deep) throws SQLException;

    public ClusterMember[] loadClusterMembers(Cluster c) throws SQLException;

    public void saveClusterMembers(Cluster c, ClusterMember[] cm) throws SQLException;

    public VoronoiCell[] loadVoronoiCells(Dataset ds, DistanceMeasure dm) throws SQLException;

    public void saveVoronoiCell(VoronoiCell cell, int datasetID, DistanceMeasure dm) throws SQLException;

    public void deleteVoronoiCells(int datasetID, DistanceMeasure dm) throws SQLException;

    public double[] loadDensities(Dataset ds, Kernel k) throws SQLException;

    public void saveDensities(int datasetID, double[] densities, Kernel k) throws SQLException;

    public void deleteDensities(int datasetID, Kernel k) throws SQLException;

    public Datapoint[] loadSortedDatapoints(Dataset ds, DistanceMeasure dm) throws SQLException;

    public Datapoint[] loadSortedDatapoints(Dataset ds, DistanceMeasure dm, int limit) throws SQLException;

    public void saveSortedDatapoints(int datasetID, Datapoint[] datapoints, DistanceMeasure dm) throws SQLException;

    public void deleteSortedDatapoints(int datasetID, DistanceMeasure dm) throws SQLException;
}
