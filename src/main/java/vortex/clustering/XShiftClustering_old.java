/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.clustering;

import clustering.ClusteringAlgorithm;
import clustering.DistanceMeasure;
import clustering.EuclideanDistance;
import clustering.AngularDistance;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import vortex.mahalonobis.MahalonobisDistance;
import clustering.Cluster;
import clustering.ClusterMember;
import clustering.ClusterSet;
import clustering.Datapoint;
import java.util.HashMap;
import clustering.Dataset;
import vortex.util.ConnectionManager;
import util.DefaultEntry;
import util.MatrixOp;
import util.ProfileAverager;
import util.Shuffle;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class XShiftClustering_old extends ClusteringAlgorithm {

    private final XshiftParamPanel pan;
    private final boolean GRADIENT_ASSIGNMENT = true;
    private final boolean angular;
    private final boolean WEIGHTED_HULL_ASSIGNMENT = false;
    private Boolean vMF = null;
    private final AtomicInteger intCount = new AtomicInteger(0);

    public XShiftClustering_old(DistanceMeasure dm) {
        super(dm);
        logger.print("X-Shift init");
        if (dm instanceof AngularDistance) {
            angular = true;
        } else if (dm instanceof EuclideanDistance) {
            angular = false;
        } else {
            throw new IllegalArgumentException("X-shift works only with Angular or Euclidean distance");
        }
        logger.print("X-Shift init2");
        pan = new XshiftParamPanel(dm);
    }

    private boolean save = true;

    public void setSave(boolean save) {
        this.save = save;
    }

    private void getWeightedAverage(double[] a, double[] b, double k, double[] res) {
        double lenB = angular ? MatrixOp.lenght(b) : 1;
        double lenA = angular ? MatrixOp.lenght(a) : 1;
        for (int i = 0; i < res.length; i++) {
            res[i] = (k * (a[i] / lenA)) + ((1.0 - k) * (b[i] / lenB));
        }
    }

    private double acos(double x) {
        //Using fast acos approximation
        //http://stackoverflow.com/questions/3380628/fast-arc-cos-algorithm/

        double a = Math.sqrt(2 + 2 * x);
        double b = Math.sqrt(2 - 2 * x);
        double c = Math.sqrt(2 - a);
        return (8 * c - b) / 3;
    }

    Integer[] K = null;
    Integer nSize = -1;
    Boolean useVMF = null;

    public void setK(Integer[] K) {
        this.K = K;
    }

    public void setnSize(Integer nSize) {
        this.nSize = nSize;
    }

    public void setUseVMF(Boolean useVMF) {
        this.useVMF = useVMF;
    }

    @Override
    public ClusterSet[] doBatchClustering(final Dataset ds, String comment) {
        if (K == null) {
            K = pan.getKNN();
        }
        if (vMF == null) {
            vMF = pan.getvMF();
        }
        logger.print("nSize");
        logger.print(nSize);
        if (nSize == null) {
            nSize = pan.getNSize(ds);
            logger.print("getting nsize");
            logger.print(nSize);
        }
        if (nSize < 1) {
            nSize = pan.getNSize(ds);
            logger.print("getting nsize");
            logger.print(nSize);
        }

        ClusterSet[] css = new ClusterSet[K.length];
        int batchID = 0;//ClusterSet.getNextBatchID(ds.getID());
        int numRegions = (int) Math.sqrt(ds.size()) * 1;
        logger.print("Splitting the dataset into regions: " + numRegions);
        TesselationCell[] cells = splitDatasetIntoCells(ds, numRegions, 1);

        ArrayList<TesselationCell> filteredCells = new ArrayList<>();
        for (TesselationCell c : cells) {
            if (c != null) {
                if (c.size > 0) {
                    filteredCells.add(c);
                }
            }
        }
        cells = filteredCells.toArray(new TesselationCell[filteredCells.size()]);

        for (TesselationCell cell : cells) {
            logger.print("Region   size: " + cell.size);
        }
        ArrayList<TesselationCell> alCells = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] != null) {
                alCells.add(cells[i]);
            }
        }
        // final Tesselation tess = new Tesselation(cells);

        final int[][] sortedNeighborLists = new int[ds.size()][];
        intCount.addAndGet(ds.size());

        logger.print("nSize = " + nSize);
        final Integer maxKNN = Math.max(K[K.length - 1], Math.max(K[0], nSize));

        final ConcurrentLinkedQueue<TesselationCell> q = new ConcurrentLinkedQueue<>();
        q.addAll(alCells);
        final Datapoint[] dataset = ds.getDatapoints();
        final AtomicInteger numDone = new AtomicInteger(0);

        int cpu = Runtime.getRuntime().availableProcessors();
        Thread[] t = new Thread[cpu];
        ThreadGroup tg = new ThreadGroup("KNNthreads");
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(tg, new Runnable() {
                @Override
                public void run() {
                    TesselationCell d;
                    while (true) {
                        synchronized (q) {
                            d = q.poll();
                        }
                        if (d != null) {
                            logger.print("working on region with size" + d.size);
                            if (d.size > 0) {
                                Entry<Datapoint, int[]>[] res = getKNNArraysForPointsInCell(d, dataset, maxKNN, true, new int[d.size][maxKNN]);
                                for (Entry<Datapoint, int[]> n : res) {
                                    sortedNeighborLists[n.getKey().getID()] = n.getValue();
                                }
                                logger.print("regions done:" + numDone.addAndGet(1));
                            }
                        } else {
                            break;
                        }
                    }
                }
            });
            t[i].start();
        }
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.print(e);
                return null;
            }
        } while (tg.activeCount() > 0);

        cells = null;
        System.gc();
        for (int i = 0; i < css.length; i++) {

            logger.print("Clustering with " + getAlgorithmName() + ", K = " + K[i]);
            css[i] = new ClusterSet(batchID, ds, this.doClustering(ds, K[i], sortedNeighborLists, nSize), dm, this.getAlgorithmName(), " K=" + K[i], K[i], comment);
            if (save) {
                try {
                    logger.print("Saving cluster set: " + css[i]);
                    ConnectionManager.getStorageEngine().saveClusterSet(css[i], true);
                    css[i] = null;
                } catch (SQLException e) {
                    logger.print(e);
                }
            }
            System.gc();
        }
        return css;
    }

    @Override
    public JPanel getParamControlPanel() {
        return pan;
    }

    private class TesselationCell {

        Double maxRadius;
        double[] center;
        Datapoint[] points;
        int size;

        public TesselationCell(Double maxRadius, double[] center, Datapoint[] points) {
            this.maxRadius = maxRadius;
            this.center = center;
            this.points = points;
            this.size = points.length;
        }
    }

    private double dist(double[] v1, double[] v2) {
        if (angular) {
            double d = MatrixOp.getEuclideanCosine(v1, v2);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                d = 0;
            }
            return acos(d);
        }
        double ret = 0;
        for (int i = 0; i < v1.length; i++) {
            ret += Math.abs(v1[i] - v2[i]);
        }
        return ret;
    }

    private Double sim(double[] v1, double[] v2) {
        //BigDecimal res = 
        //logger.print(res);
        if (angular) {
            double d = MatrixOp.getEuclideanCosine(v1, v2);
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                d = 0;
            }
            return d;
        }
        return 1 / (dist(v1, v2) + 1);
    }

    private TesselationCell[] splitDatasetIntoCells(final Dataset nd, final int numCells, int opt_rounds) {
        Datapoint[] cent = Arrays.copyOf((new Shuffle<Datapoint>()).shuffleCopyArray(nd.getDatapoints()), numCells);
        final double[][] centroids = new double[cent.length][];
        final int[] sizes = new int[centroids.length];
        final int[] assignments = new int[nd.size()];
        for (int i = 0; i < centroids.length; i++) {
            centroids[i] = MatrixOp.copy(cent[i].getVector());
        }
        final Datapoint[] dp = nd.getDatapoints();
        for (int r = 0; r < opt_rounds; r++) {
            final ConcurrentLinkedQueue<Datapoint> q = new ConcurrentLinkedQueue<>();
            q.addAll(Arrays.asList(nd.getDatapoints()));
            int cpu = Runtime.getRuntime().availableProcessors();
            Thread[] t = new Thread[cpu];
            ThreadGroup tg = new ThreadGroup("NNthreads");
            for (int i = 0; i < t.length; i++) {
                t[i] = new Thread(tg, new Runnable() {
                    @Override
                    public void run() {
                        Datapoint d;
                        while ((d = q.poll()) != null) {
                            if (q.size() % 10000 == 0) {
                                logger.print("dp to go:" + q.size());
                            }
                            double[] vec = MatrixOp.copy(d.getVector());
                            double maxSim = -1;
                            int nIDX = -1;
                            for (int j = 0; j < numCells; j++) {
                                double sim = sim(vec, centroids[j]);
                                if (sim > maxSim) {
                                    nIDX = j;
                                    maxSim = sim;
                                }
                            }
                            if (nIDX == -1) {
                                nIDX = (int) Math.floor(Math.random() * (numCells));
                            }
                            assignments[d.getID()] = nIDX;
                        }
                    }
                });
                t[i].start();
            }
            do {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {

                    logger.print(e);
                    return null;
                }
            } while (tg.activeCount() > 0);

            double[][] newCentroids = new double[centroids.length][centroids[0].length];
            Arrays.fill(sizes, 0);
            for (int i = 0; i < assignments.length; i++) {
                int centIDX = assignments[i];

                double[] ulv = MatrixOp.copy(dp[i].getVector());//angular ? MatrixOp.toUnityLen() : MatrixOp.copy(d.getVector());
                double len = angular ? MatrixOp.lenght(ulv) : 1;
                for (int j = 0; j < ulv.length; j++) {
                    newCentroids[centIDX][j] += ulv[j] / len;
                }
                sizes[centIDX]++;
            }
            for (int i = 0; i < centroids.length; i++) {
                if (sizes[i] > 0) {
                    centroids[i] = newCentroids[i];
                    MatrixOp.mult(centroids[i], 1.0 / (angular ? MatrixOp.lenght(newCentroids[i]) : sizes[i]));
                }
            }
        }

        TesselationCell[] cells = new TesselationCell[centroids.length];

        for (int g = 0; g < assignments.length; g++) {
            int cI = assignments[g];

            if (cells[cI] == null) {
                cells[cI] = new TesselationCell(0d, centroids[cI], new Datapoint[sizes[cI]]);
            }
            cells[cI].points[--sizes[cI]] = dp[g];
            double dist = dist(cells[cI].center, dp[g].getVector());
            if (dist > cells[cI].maxRadius) {
                cells[cI].maxRadius = dist;
            }
        }
        return cells;
    }

    public double getMahalonobisDistance(Cluster a, Cluster b, HashMap<Cluster, MahalonobisDistance> hmMah) {
        /*
        if(hmMah.get(a)==null){
        hmMah.put(a, new MahalonobisDistance(a));
        }
        
        if(hmMah.get(b)==null){
        hmMah.put(b, new MahalonobisDistance(b));
        }
        
        return Math.max(hmMah.get(b).distTo(a.getMode().getVector()),hmMah.get(a).distTo(b.getMode().getVector()));
         */
        double[] avgA = a.getMode().getVector();
        double[] avgB = b.getMode().getVector();
        double[] sdVecA = new double[avgA.length];
        double[] sdVecB = new double[avgB.length];
        try {
            for (ClusterMember cmA : a.getClusterMembers()) {
                double[] vec = cmA.getDatapoint().getVector();
                for (int i = 0; i < sdVecA.length; i++) {
                    sdVecA[i] += (vec[i] - avgA[i]) * (vec[i] - avgA[i]);
                }
            }
            for (int i = 0; i < sdVecA.length; i++) {
                sdVecA[i] /= a.getClusterMembers().length - 1;
                sdVecA[i] = Math.sqrt(sdVecA[i]);
            }
            for (ClusterMember cmB : b.getClusterMembers()) {
                double[] vec = cmB.getDatapoint().getVector();
                for (int i = 0; i < sdVecB.length; i++) {
                    sdVecB[i] += (vec[i] - avgB[i]) * (vec[i] - avgB[i]);
                }
            }

            for (int i = 0; i < sdVecB.length; i++) {
                sdVecB[i] /= b.getClusterMembers().length - 1;
                sdVecB[i] = Math.sqrt(sdVecB[i]);
            }

        } catch (Exception e) {
            logger.showException(e);
        }
        double[] sdVec = new double[sdVecA.length];

        for (int i = 0; i < sdVec.length; i++) {
            sdVec[i] = (sdVecA[i] + sdVecB[i]) / 2;
        }

        double dist = 0;
        for (int i = 0; i < sdVec.length; i++) {
            dist += Math.pow((avgA[i] - avgB[i]) / sdVec[i], 2);
        }
        return Math.sqrt(dist);
    }

    public Cluster[] reassignDatapointsByMahDist(Datapoint[] d, Cluster[] cl) {

        MahalonobisDistance[] ms = new MahalonobisDistance[cl.length];
        for (int i = 0; i < cl.length; i++) {
            double[][] vec = new double[cl[i].getClusterMembers().length][];
            for (int j = 0; j < vec.length; j++) {
                vec[j] = cl[i].getClusterMembers()[j].getDatapoint().getVector();
            }
            ms[i] = new MahalonobisDistance(vec);
        }
        LinkedList<Datapoint>[] classifiedLists = new LinkedList[ms.length];
        for (int i = 0; i < classifiedLists.length; i++) {
            classifiedLists[i] = new LinkedList<>();
        }
        for (Datapoint nd : d) {
            int nnIDX = -1;
            double minDist = Double.MAX_VALUE;
            for (int i = 0; i < ms.length; i++) {
                double dist = ms[i].distTo(nd.getVector());

                //MatrixOp.getEuclideanDistance(ms[i].center.toArray(), nd.getVector()); //
                if (dist < minDist) {
                    minDist = dist;
                    nnIDX = i;
                }
            }
            classifiedLists[nnIDX].add(nd);
        }

        Cluster[] out = new Cluster[cl.length];
        for (int i = 0; i < classifiedLists.length; i++) {
            out[i] = new Cluster(classifiedLists[i].toArray(new Datapoint[classifiedLists[i].size()]), cl[i].getMode().getVector(), cl[i].getMode().getSideVector(), cl[i].getCaption(), dm);
        }
        return out;

    }

    public Cluster[] mergeClustersByMahDist(Cluster[] cl, double ths) {
        List<Cluster> list = new ArrayList(Arrays.asList(cl));
        double minDist = ths;
        int k = 0;
        HashMap<Cluster, MahalonobisDistance> hmMah = new HashMap<>();

        do {
            logger.print("merging round: " + (k++));
            minDist = ths;
            int[] bestPair = new int[]{-1, -1};
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    double dist = getMahalonobisDistance(list.get(i), list.get(j), hmMah);
                    if (dist < minDist) {
                        minDist = dist;
                        bestPair[0] = i;
                        bestPair[1] = j;
                    }
                }
            }
            if (minDist < ths) {
                Cluster c1 = list.remove(bestPair[1]);
                Cluster c2 = list.remove(bestPair[0]);
                hmMah.remove(c1);
                hmMah.remove(c2);
                list.add(mergeClusters(c1, c2));
            }
            logger.print("minDist: " + minDist);
        } while (minDist < ths && list.size() > 1);
        return list.toArray(new Cluster[list.size()]);
    }

    public static Cluster mergeClusters(Cluster a, Cluster b) {
        try {

            ArrayList<Datapoint> alCl = new ArrayList<>();

            for (Cluster c : new Cluster[]{a, b}) {
                for (ClusterMember cm : c.getClusterMembers()) {
                    alCl.add(cm.getDatapoint());
                }
            }

            if (alCl != null) {
                //alCl.add(roots.get(idx));
                @SuppressWarnings("unchecked")
                //AngularDistance dm = new AngularDistance();

                ProfileAverager pa = new ProfileAverager();
                ProfileAverager paMain = new ProfileAverager();
                for (Datapoint d : alCl) {
                    pa.addProfile(d.getSideVector());
                    paMain.addProfile(d.getVector());
                }

                if (!alCl.isEmpty()) {
                    return new Cluster(alCl.toArray(new Datapoint[alCl.size()]), paMain.getAverage(), pa.getAverage(), alCl.get(0).getFullName());
                }
            }

        } catch (Exception e) {
            logger.showException(e);
        }
        return null;
    }

    private BigDecimal getDensityWithKNN(Datapoint[] dp, int[] sortedN, double[] center, int knn) {
        BigDecimal dens = new BigDecimal(0, MathContext.DECIMAL128);
        for (int i = 0; i < knn; i++) {
            dens = dens.add(new BigDecimal(-dist(center, dp[sortedN[i]].getVector()), MathContext.DECIMAL128));
        }
        return dens;
    }
    private vMFKernel kernel;

    private BigDecimal getDensityVMF(Dataset ds, double[] center, double kappa) {
        if (kernel == null) {
            kernel = new vMFKernel(dm);
            kernel.init(ds, kappa);
        }
        kernel.setBandwidth(kappa);
        double[][] vec = new double[ds.size()][];
        for (int i = 0; i < vec.length; i++) {
            vec[i] = ds.getDatapoints()[i].getVector();
        }
        return kernel.getDensity(center, vec);
        /*
         BigDecimal dens = new BigDecimal(0,MathContext.DECIMAL128);
         for (int i = 0; i < dp.length; i++) {
         dens.add(new BigDecimal(Math.exp(MatrixOp.getEuclideanCosine(center, dp[i].getVector())*kappa),MathContext.DECIMAL128) ) ;
         }
         return dens;*/
    }

    private int[] getKNNExhaustive(double[] cent, Datapoint[] dataset, int knn) {
        DefaultEntry<Datapoint, Double>[] lst = new DefaultEntry[knn];

        for (int k = 0; k < knn; k++) {
            lst[k] = new DefaultEntry<>(dataset[k], dist(cent, dataset[k].getVector()));
        }
        Arrays.sort(lst, new Comparator<Entry<Datapoint, Double>>() {
            @Override
            public int compare(Entry<Datapoint, Double> o1, Entry<Datapoint, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        LinkedList<DefaultEntry<Datapoint, Double>> ln = new LinkedList<>();
        for (int k = 0; k < knn; k++) {
            ln.add(lst[k]);
        }

        lst = null;

        for (int k = knn; k < dataset.length; k++) {
            Double maxDist = ln.getLast().getValue();
            Double d = dist(cent, dataset[k].getVector());
            if (d.compareTo(maxDist) < 0) {
                for (int j = 0; j < knn; j++) {
                    if (ln.get(j).getValue().compareTo(d) > 0) {
                        ln.add(j, new DefaultEntry<>(dataset[k], d));
                        ln.removeLast();
                        break;
                    }
                }
            }
        }

        int[] res = new int[ln.size()];
        for (int p = 0; p < res.length; p++) {
            res[p] = ln.get(p).getKey().getID();
        }
        ln = null;
        //logger.print("getKNNExhaustive K"+knn + " took " + (Calendar.getInstance().getTimeInMillis()-time));
        return res;
    }

    private class DatapointDist implements Comparable {

        Datapoint dp;
        double dist;

        public DatapointDist(Datapoint dp, double dist) {
            this.dp = dp;
            this.dist = dist;
        }

        public void update(Datapoint dpn, double distn) {
            this.dp = dpn;
            this.dist = distn;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof DatapointDist) {
                return (int) Math.signum(this.dist - ((DatapointDist) o).dist);
            } else {
                return 0;
            }
        }
    }

    private Entry<Datapoint, int[]>[] getKNNArraysForPointsInCell(TesselationCell cell, Datapoint[] dataset, int knn, boolean printNumEvaluations, int[][] indexArray) {
        if (cell == null) {
            return new Entry[0];
        }
        Entry<Datapoint, int[]>[] retArray = new Entry[cell.size];

        if (!angular) {
            for (int i = 0; i < retArray.length; i++) {
                retArray[i] = new DefaultEntry<>(cell.points[i], getKNNExhaustive(cell.points[i].getVector(), dataset, knn));
            }
            return retArray;
        }

        double[] cent = cell.center;
        DatapointDist[] centralArray = new DatapointDist[dataset.length];
        for (int i = 0; i < centralArray.length; i++) {
            centralArray[i] = new DatapointDist(dataset[i], dist(dataset[i].getVector(), cent));
        }
        Arrays.sort(centralArray);
        /*
         for (int p = 0; p < knn; p++) {
         logger.print(centralArray[p]);
         }*/
        assert (indexArray.length == cell.size);

        for (int i = 0; i < indexArray.length; i++) {
            assert (indexArray[i].length == knn);
        }

        int numEval = 0;
        for (int i = 0; i < retArray.length; i++) {
            double curr[] = cell.points[i].getVector();
            DatapointDist[] lst = new DatapointDist[knn];
            for (int k = 0; k < knn; k++) {
                numEval++;
                lst[k] = new DatapointDist(centralArray[k].dp, dist(curr, centralArray[k].dp.getVector()));
            }
            Arrays.sort(lst);
            LinkedList<DatapointDist> ln = new LinkedList<>();
            for (int k = 0; k < knn; k++) {
                ln.add(lst[k]);
            }
            lst = null;

            double distToCent = dist(curr, cent);
            for (int k = knn; k < centralArray.length; k++) {
                double maxDist = ln.getLast().dist;
                if (centralArray[k].dist - distToCent > maxDist) {
                    break;
                } else {
                    numEval++;
                    double d = dist(curr, centralArray[k].dp.getVector());
                    if (d < maxDist) {
                        for (int j = 0; j < knn; j++) {
                            if (ln.get(j).dist > d) {
                                DatapointDist dd = ln.removeLast();
                                dd.update(centralArray[k].dp, d);
                                ln.add(j, dd);
                                break;
                            }
                        }
                    }
                }
            }

            int[] res = indexArray[i];
            for (int p = 0; p < res.length; p++) {
                res[p] = ln.get(p).dp.getID();
            }

            retArray[i] = new DefaultEntry<>(cell.points[i], res);
            if (printNumEvaluations) {
                int firstMismatchIDx = -1;
                int[] alt = getKNNExhaustive(cell.points[i].getVector(), dataset, knn);
                for (int j = 0; j < alt.length; j++) {
                    if (alt[j] != res[j]) {
                        firstMismatchIDx = j;
                        break;
                    }
                }
                //logger.print(Arrays.toString(alt));
                //logger.print(Arrays.toString(res));
                logger.print("numEvaluations:" + (numEval) + ((firstMismatchIDx >= 0) ? (", First mismatch" + firstMismatchIDx) : ""));

                printNumEvaluations = false;
            }
        }
        return retArray;
    }

    public Cluster[] doClustering(final Dataset ds, final int K, final int[][] sortedNeighborLists, int nSize) {
        //Select N centers
        //assign the rest of the dataset to the centeroids - Voronoi tesselation
        //Compute the Gabriel graph of the centroids
        //repeat n times
        //merge same-density neighboring regions in the graph
        //find modes and the shortest density-connected paths
        //.........
        //PROFIT!!!!

        //Select N centers
        //NormalGen ng = new NormalGen(new GenF2w32(), 0, 1);
        // NDatapoint[] dp = ds.getDatapoints();
        //System.gc();
        int len = ds.size();

        final Datapoint[] dp = ds.getDatapoints();

        final BigDecimal[] dens = new BigDecimal[len];

        final ConcurrentLinkedQueue<Datapoint> q = new ConcurrentLinkedQueue<>();
        q.addAll(Arrays.asList(dp));

        logger.print("Computing densities");
        int cpu = Runtime.getRuntime().availableProcessors();
        Thread[] threads = new Thread[cpu];
        ThreadGroup tg = new ThreadGroup("Densitythreads");
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(tg, new Runnable() {
                @Override
                public void run() {
                    Datapoint d;
                    while (!Thread.interrupted() && (d = q.poll()) != null) {
                        int[] nid = sortedNeighborLists[d.getID()];

                        dens[d.getID()] = vMF ? getDensityVMF(ds, d.getVector(), K) : getDensityWithKNN(dp, nid, d.getVector(), K);//getvMFDensityWithTesselation(t, d.getVector(), K, precision, d.getID() % 1000 == 0);
                        //logger.print(d,  dens[d.getID()]);
                    }
                }
            });
            threads[i].start();
        }
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.print(e);
                return null;
            }
        } while (tg.activeCount() > 0);

        final ArrayList<Datapoint> roots = new ArrayList<>();
        final int[] linkArray = new int[len];
        Arrays.fill(linkArray, -1);
        logger.print("Finding roots:");

        for (int i = 0; i < len; i++) {
            BigDecimal currDens = dens[i];
            double[] currVec = dp[i].getVector();
            Double minDist = Double.MAX_VALUE;
            int parent = -1;
            int[] nei = sortedNeighborLists[i];
            for (int j = 1; j < nSize; j++) {
                Double distance = dist(currVec, dp[nei[j]].getVector());// Math.exp(-Math.acos(dens[nei[j].getID()].doubleValue()))-Math.exp(-Math.acos(currDens.doubleValue())))/dist(currVec, nei[j].getVector());
                if (dens[nei[j]].compareTo(currDens) > 0 && distance.compareTo(minDist) < 0) { //
                    parent = nei[j];
                    minDist = distance;
                    //logger.print(parent = );
                }
            }

            if (parent == -1) {
                roots.add(dp[i]);
                //logger.print("dp#" + i + " is a root #" + roots.size());
            } else {
                linkArray[i] = parent;//, dm.getDistance(currVec, parent.getVector()));
            }
        }

        logger.print("Found roots: " + roots.size());

        final Datapoint[] rootsA = roots.toArray(new Datapoint[roots.size()]);
        logger.print("Merging roots");
        final ConcurrentLinkedQueue<Datapoint> q2 = new ConcurrentLinkedQueue<>();
        q2.addAll(roots);
        final double[][] rootVec = new double[rootsA.length][];

        for (int i = 0; i < rootVec.length; i++) {
            rootVec[i] = MatrixOp.copy(rootsA[i].getVector());
        }

        //logger.print("Computing densities");
        //int cpu = Runtime.getRuntime().availableProcessors();
        threads = new Thread[cpu];
        ThreadGroup tg3 = new ThreadGroup("mergingthreads");
        final int dim = ds.getNumDimensions();
        //final double[][] weightRatioMatrix = new double[rootVec.length][rootVec.length];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(tg3, new Runnable() {
                @Override
                public void run() {
                    double step = 0.05;
                    int numSteps = 0;
                    for (double k = step; k <= 1.0 - step; k += step) {
                        numSteps++;
                    }
                    Datapoint[] midWDatapoints = new Datapoint[numSteps];
                    double[][] midWvec = new double[numSteps][dim];
                    int[][] idxARR = new int[numSteps][K];

                    main:
                    while (!(q2.isEmpty() || Thread.interrupted())) {
                        Datapoint d;

                        d = q2.poll();
                        if (d == null) {
                            break main;
                        }

                        if (q2.size() % 10 == 0) {
                            logger.print("roots to go: " + q2.size());
                        }

                        int mergeIDX = -1;
                        Double maxSim = -1d;
                        double[] currVec = d.getVector();

                        merge:
                        for (int j = 0; j < rootsA.length; j++) {
                            int oID = rootsA[j].getID();
                            if (oID == d.getID() || dens[d.getID()].compareTo(dens[oID]) > 0) {
                                continue;
                            }
                            double[] mid;
                            if (angular) {
                                mid = MatrixOp.toUnityLen(MatrixOp.sum(MatrixOp.toUnityLen(currVec), MatrixOp.toUnityLen(rootVec[j])));
                            } else {
                                mid = MatrixOp.sum(currVec, rootVec[j]);
                                MatrixOp.mult(mid, 0.5);
                            }

                            int[] nnOTM = getKNNExhaustive(mid, rootsA, 2);
                            //Checking gabriel neighborhood
                            if (!(Math.min(nnOTM[0], nnOTM[1]) == Math.min(d.getID(), rootsA[j].getID()) && Math.max(nnOTM[0], nnOTM[1]) == Math.max(d.getID(), rootsA[j].getID()))) {
                                continue merge;
                            }

                            if (true) {
                                int n = 0;
                                for (double k = step; k <= 1.0 - step; k += step) {
                                    getWeightedAverage(currVec, rootVec[j], k, midWvec[n]);
                                    midWDatapoints[n] = new Datapoint("TMP", midWvec[n], n);
                                    n++;
                                }
                                TesselationCell cell = new TesselationCell(dist(currVec, mid), mid, midWDatapoints);
                                BigDecimal minDens = dens[d.getID()];

                                for (Entry<Datapoint, int[]> e : getKNNArraysForPointsInCell(cell, dp, K, false, idxARR)) {
                                    int[] nid = e.getValue();
                                    BigDecimal middens = vMF ? getDensityVMF(ds, e.getKey().getVector(), K) : getDensityWithKNN(dp, nid, e.getKey().getVector(), K);
                                    if (middens.compareTo(minDens) < 0) {
                                        continue merge;
                                    }
                                }
                                if (sim(d.getVector(), rootVec[j]).compareTo(maxSim) > 0) {
                                    maxSim = sim(d.getVector(), rootsA[j].getVector());
                                    mergeIDX = j;
                                }
                                cell = null;
                            }
                        }
                        synchronized (XShiftClustering_old.this) {
                            if (mergeIDX > -1 && linkArray[rootsA[mergeIDX].getID()] != d.getID()) {
                                //logger.print("merging " + d.getID() + " to " + rootsA[mergeIDX].getID());
                                linkArray[d.getID()] = rootsA[mergeIDX].getID();
                            }
                        }
                    }
                    //logger.print("eval:" + numRootsEval);
                }
            });
            threads[i].start();
        }
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.print(e);
                return null;
            }
        } while (tg3.activeCount() > 0);

        logger.print("end filtering");

        roots.clear();

        for (int i = 0; i < linkArray.length; i++) {
            if (linkArray[i] == -1) {
                roots.add(dp[i]);
            }
        }

        logger.print("Roots left after filtering: " + roots.size());

        ArrayList<Cluster> clusters = new ArrayList<>();
        final ConcurrentLinkedQueue<Datapoint>[] clusterMembers = new ConcurrentLinkedQueue[roots.size()];

        for (int i = 0; i < clusterMembers.length; i++) {
            clusterMembers[i] = new ConcurrentLinkedQueue<>();
        }

        final ConcurrentLinkedQueue<Datapoint> qdp = new ConcurrentLinkedQueue<>();
        qdp.addAll(Arrays.asList(ds.getDatapoints()));
        final java.util.concurrent.atomic.AtomicInteger a = new AtomicInteger(0);
        Thread[] t2 = new Thread[cpu];
        ThreadGroup tg2 = new ThreadGroup("RootNNThreads");

        if (GRADIENT_ASSIGNMENT) {
            logger.print("Assigning by gradient");
            for (int i = 0; i < t2.length; i++) {
                t2[i] = new Thread(tg2, new Runnable() {
                    @Override
                    public void run() {
                        Datapoint d;
                        while ((d = qdp.poll()) != null) {
                            if (qdp.size() % 10000 == 0) {
                                logger.print("dp to go:" + qdp.size());
                            }
                            a.addAndGet(1);
                            int idx = d.getID();

                            //ArrayList<Integer> alPrev = new ArrayList<>(10);
                            while (linkArray[idx] >= 0) {

                                //alPrev.add(idx);
                                idx = linkArray[idx];
                                /*if (alPrev.contains(idx)) {
                                 logger.print("cycle detected", alPrev, idx);
                                 }*/
                            }
                            for (int j = 0; j < roots.size(); j++) {
                                if (roots.get(j).getID() == idx) {
                                    clusterMembers[j].add(d);
                                    //linkArray[d.getID()]=roots.get(j).getID();
                                    break;
                                }
                            }
                        }
                    }
                });
                t2[i].start();
            }
        } else {
            for (int i = 0; i < t2.length; i++) {
                t2[i] = new Thread(tg2, new Runnable() {
                    @Override
                    public void run() {
                        Datapoint d;
                        while ((d = qdp.poll()) != null) {
                            if (qdp.size() % 10000 == 0) {
                                logger.print("dp to go:" + qdp.size());
                            }
                            a.addAndGet(1);
                            //ArrayList<Integer> alPrev = new ArrayList<>(10);
                            int nnIDX = -1;
                            Double maxSim = -1d;
                            for (int j = 0; j < roots.size(); j++) {
                                Double s = sim(roots.get(j).getVector(), d.getVector());
                                if (s.compareTo(maxSim) > 0) {
                                    maxSim = s;
                                    nnIDX = j;
                                }
                            }
                            clusterMembers[nnIDX].add(d);

                        }
                    }
                });
                t2[i].start();
            }
        }

        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.print(e);
                return null;
            }
        } while (tg2.activeCount() > 0);
        logger.print("datapoints done: " + a.toString() + " dp size: " + ds.getDatapoints().length);

        for (int idx = 0; idx < roots.size(); idx++) {
            ConcurrentLinkedQueue<Datapoint> alCl = clusterMembers[idx];
            if (alCl != null) {
                //alCl.add(roots.get(idx));
                @SuppressWarnings("unchecked")
                //AngularDistance dm = new AngularDistance();

                ProfileAverager pa = new ProfileAverager();
                ProfileAverager paMain = new ProfileAverager();
                for (Datapoint d : alCl) {
                    pa.addProfile(d.getSideVector());
                    paMain.addProfile(d.getVector());
                }

                if (!alCl.isEmpty()) {
                    Cluster c = new Cluster(alCl.toArray(new Datapoint[alCl.size()]), paMain.getAverage(), pa.getAverage(), roots.get(idx).getFullName(), dm);
                    clusters.add(c);
                }
                clusterMembers[idx].clear();
                clusterMembers[idx] = null;
            }
        }

        t2 = null;
        threads = null;
        tg = null;
        tg2 = null;
        tg3 = null;

        logger.print("Merging clusters by Mahalanobis distance");
        return mergeClustersByMahDist(clusters.toArray(new Cluster[clusters.size()]), 2.0);
    }

    @Override
    public String getAlgorithmDescription() {
        return "X-shift is a fast density-based algorithm capable of clustering large data in sub-quadratic time";
    }

    @Override
    public String getAlgorithmName() {
        return "X-shift_old (" + (WEIGHTED_HULL_ASSIGNMENT ? "Valley breakup" : (GRADIENT_ASSIGNMENT ? "Gradient" : "Nearest centroid")) + " assignment)";
    }

    @Override
    public boolean isAlgorithmPublic() {
        return true;
    }
}
