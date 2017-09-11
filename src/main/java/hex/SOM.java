/**
 * This file is part of the Java Machine Learning Library
 *
 * The Java Machine Learning Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * The Java Machine Learning Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Java Machine Learning Library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Copyright (c) 2006-2012, Thomas Abeel
 *
 * Project: http://java-ml.sourceforge.net/
 *
 */
package hex;

import java.util.Arrays;
import java.util.Random;
import java.util.ArrayList;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.EuclideanDistance;

/**
 * An implementation of the Self Organizing Maps algorithm as proposed by
 * Kohonen.
 *
 * This implementation is derived from the Bachelor thesis of Tomi Suuronen
 * titled "Java2 implementation of Self-Organizing Maps based on Neural Networks
 * utilizing XML based Application Languages for Information Exchange and
 * Visualization.". (http://javasom.sourceforge.net/)
 *
 * @author Tomi Suuronen
 * @author Thomas Abeel
 *
 */
public class SOM {

    private static final long serialVersionUID = -2023942260277855655L;

    /**
     * Calculates the distance between two ArrayLists using the distance
     * function that was supplied during creation of the SOM. If no distance
     * measure was specified, the Euclidean Distance will be used by default.
     *
     * @param double[] x - 1st ArrayList.
     * @param double[] y - 2nd ArrayList.
     * @return double - returns the distance between two ArrayLists, x and y
     */
    private double getDistance(double[] x, double[] y) {
        return dm.measure(new DenseInstance(x), new DenseInstance(y));
    }

    private class JSomMath {

        private double[] cacheArrayList; // cache ArrayList for temporary storage.

        private int sizeArrayList; // size of the cache ArrayList.

        private double gaussianCache; // double cache for gaussian

        /**
         * Constructor.
         *
         * @param int ArrayListSize - Size of a weight/input ArrayList.
         */
        private JSomMath(int ArrayListSize) {
            cacheArrayList = new double[ArrayListSize];
            sizeArrayList = cacheArrayList.length;
        }

        /**
         * Calculates the exponential learning-rate parameter value.
         *
         * @param int n - current step (time).
         * @param double a - initial value for learning-rate parameter (should
         * be close to 0.1).
         * @param int A - time constant (usually the number of iterations in the
         * learning process).
         * @return double - exponential learning-rate parameter value.
         */
        private double expLRP(int n, double a, int A) {
            return (a * Math.exp(-1.0 * ((double) n) / ((double) A)));
        }

        /**
         * Calculates the linear learning-rate parameter value.
         *
         * @param int n - current step (time).
         * @param double a - initial value for learning-rate parameter (should
         * be close to 0.1).
         * @param int A - another constant (usually the number of iterations in
         * the learning process).
         * @return double - linear learning-rate parameter value.
         */
        private double linLRP(int n, double a, int A) {
            return (a * (1 - ((double) n) / ((double) A)));
        }

        /**
         * Calculates the inverse time learning-rate parameter value.
         *
         * @param int n - current step (time).
         * @param double a - initial value for learning-rate parameter (should
         * be close to 0.1).
         * @param double A - another constant.
         * @param double B - another constant.
         * @return double - inverse time learning-rate parameter value.
         */
        private double invLRP(int n, double a, double A, double B) {
            return (a * (A / (B + n)));
        }

        /**
         * Calculates the gaussian neighbourhood width value.
         *
         * @param double g - initial width value of the neighbourhood.
         * @param int n - current step (time).
         * @param int t - time constant (usually the number of iterations in the
         * learning process).
         * @return double - adapted gaussian neighbourhood function value.
         */
        private double gaussianWidth(double g, int n, int t) {
            return (g * Math.exp(-1.0 * ((double) n) / ((double) t)));
        }

        /**
         * Calculates the Gaussian neighbourhood value.
         *
         * @param double[] i - winning neuron location in the lattice.
         * @param double[] j - excited neuron location in the lattice.
         * @param double width - width value of the neighbourhood.
         * @return double - Gaussian neighbourhood value.
         */
        private double gaussianNF(double[] i, double[] j, double width) {
            gaussianCache = getDistance(i, j);
            return (Math.exp(-1.0 * gaussianCache * gaussianCache / (2.0 * width * width)));
        }

        /**
         * Calculates whether the excited neuron is in the Bubble neighbourhood
         * set.
         *
         * @param double[] i - winning neuron location in the lattice.
         * @param double[] j - excited neuron location in the lattice.
         * @param double g - width value of the neighbourhood.
         * @return boolean - true if located in the Bubble neighbourhood set.
         */
        private boolean bubbleNF(double[] i, double[] j, double g) {
            if (getDistance(i, j) <= g) {
                return true;
            }
            return false;
        }

        /**
         * Calculates the new adapted values for a weight ArrayList, based on
         * Bubble neighbourhood.
         *
         * @param double[] x - input ArrayList.
         * @param double[] w - weight ArrayList.
         * @param double[] i - winning neuron location in the lattice.
         * @param double[] j - excited neuron location in the lattice.
         * @param double g - adapted width value of the neighbourhood.
         * @param double lrp - adapted learning-rate parameter value.
         * @return double[] - Returns the adapted neuron values.
         */
        private double[] bubbleAdaptation(double[] x, double[] w, double[] i, double[] j, double g, double lrp) {
            if (bubbleNF(i, j, g)) {
                for (int k = 0; k < sizeArrayList; k++) {
                    cacheArrayList[k] = w[k] + lrp * (x[k] - w[k]);
                }
            } else {
                return w;
            }
            return cacheArrayList;
        }

        /**
         * Calculates the new adapted values for a weight ArrayList, based on
         * Gaussian neighbourhood.
         *
         * @param double[] x - input ArrayList.
         * @param double[] w - weight ArrayList.
         * @param double[] i - winning neuron location in the lattice.
         * @param double[] j - excited neuron location in the lattice.
         * @param double width - adapted width value of the neighbourhood.
         * @param double lrp - adapted learning-rate parameter value.
         * @return double[] - Returns the adapted neuron values.
         */
        private double[] gaussianAdaptation(double[] x, double[] w, double[] i, double[] j, double width, double lrp) {
            gaussianCache = gaussianNF(i, j, width);
            for (int k = 0; k < sizeArrayList; k++) {
                cacheArrayList[k] = w[k] + lrp * gaussianCache * (x[k] - w[k]);
            }
            return cacheArrayList;
        }
    }

    private class InputVectors extends ArrayList<SomNode> {

        // private ArrayList input; //input ArrayLists
        /**
         *
         */
        private static final long serialVersionUID = 703966236164827750L;

        /**
         * Main constructor for this map. Used to contain all the input
         * ArrayLists.
         */
        private InputVectors() {
            super(1000);
        }

        /**
         * Main constructor for this map. Used to contain all the input
         * ArrayLists.
         *
         * @param capacity Number of input ArrayLists.
         */
        private InputVectors(int capacity) {
            super(capacity);
        }

        /**
         * Returns a Node values of a specific input ArrayList from the
         * specified index.
         *
         * @param index The index of SomNode.
         * @return double[] - returns the Node values from the specified index.
         */
        private double[] getNodeValuesAt(int index) {
            SomNode cache = (SomNode) get(index);
            return (cache.getValues());
        }

        /**
         * Returns a Node label of a specific input ArrayList from the specified
         * index.
         *
         * @param index The index of SomNode.
         * @return String - returns the Node label from the specified index.
         */
        private String getNodeLabelAt(int index) {
            SomNode cache = (SomNode) get(index);
            return (cache.getLabel());
        }

        /**
         * Returns the number of input ArrayLists.
         *
         * @return int - returns the number of input ArrayLists.
         */
        private int getCount() {
            return size();
        }
    }

    private class WeightVectors extends ArrayList<SomNode> {

        /**
         *
         */
        private static final long serialVersionUID = -8922053499602333314L;

        private double[] values;

        private double[] location;

        private String lattice; // topology of the map

        private Random generator;

        private int dimension; // dimensionality of a node

        private final double YVALUE = 0.866;

        /**
         * Main constructor. Used to contain the synaptic weight ArrayLists
         * during the learning phase.
         *
         * @param xDim X-dimension of the map constructed.
         * @param yDim Y-dimension of the map constructed.
         * @param dimension dimensionality of a node. This is the dimension of
         * an instance
         * @param type Lattice type of the map constructed (hexa or rect)
         */
        private WeightVectors(int xDim, int yDim, int dimension, String type) {
            super(xDim * yDim);
            int size = xDim * yDim;

            this.dimension = dimension;
            values = new double[dimension];
            location = new double[2];
            generator = new Random();
            lattice = type;
            int yCounter = 0;
            int xCounter = 0;
            double xValue = 0;
            double yValue = 0;
            boolean evenRow = false; // for hexagonal lattice, checking if
            // the
            // current row number is even or odd
            if (lattice.equals("rect")) { // rectangular lattice
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < dimension; j++) {
                        values[j] = generator.nextDouble();
                    }
                    if (xCounter < xDim) {
                        location[0] = xCounter;
                        location[1] = yCounter;
                        xCounter++;
                    } else {
                        xCounter = 0;
                        yCounter++;
                        location[0] = xCounter;
                        location[1] = yCounter;
                        xCounter++;
                    }
                    add(new SomNode(values, location));
                }
            } else { // hexagonal lattice
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < dimension; j++) {
                        values[j] = generator.nextDouble();
                    }
                    if (xCounter < xDim) {
                        location[0] = xValue;
                        location[1] = yValue;
                        xValue += 1.0;
                        xCounter++;
                    } else {
                        xCounter = 0;
                        yValue += YVALUE;
                        if (evenRow) {
                            xValue = 0.0;
                            evenRow = false;
                        } else {
                            xValue = 0.5;
                            evenRow = true;
                        }
                        location[0] = xValue;
                        location[1] = yValue;
                        xValue += 1.0;
                        xCounter++;
                    }
                    add(new SomNode(values, location));
                }
            }
        }

        /**
         * Returns the node values at a specific node.
         *
         * @param index Index of the SomNode
         * @return double[] - Returns the Node values from the specified index.
         */
        private double[] getNodeValuesAt(int index) {
            SomNode cache = (SomNode) get(index);
            return (cache.getValues());
        }

        /**
         * Sets the node values at a specific node.
         *
         * @param index Index of the SomNode
         * @param values Values of the SomNode
         */
        private void setNodeValuesAt(int index, double[] values) {
            SomNode cache = (SomNode) get(index);
            cache.setValues(values);
            set(index, cache);
        }

        /**
         * Returns the node values at a specific node.
         *
         * @param index Index of the SomNode
         * @return double[] - Returns the Node location from the specified
         * index.
         */
        private double[] getNodeLocationAt(int index) {
            SomNode cache = (SomNode) get(index);
            return (cache.getLocation());
        }

        /**
         * Returns the dimensionality of a node (it is the same for all of
         * them).
         *
         * @return int - Dimensionality of nodes.
         */
        private int getDimensionalityOfNodes() {
            return dimension;
        }

        /**
         * Returns the number of weight ArrayLists.
         *
         * @return int - Returns the number of weight ArrayLists.
         */
        private int getCount() {
            return size();
        }

        /**
         * Sets the label of a specific weight ArrayList at the specified index.
         *
         * @param index The index of SomNode.
         * @param label The new label for this SomNode.
         * @return String - Returns the Node label from the specified index.
         */
        private void setNodeLabelAt(int index, String label) {
            SomNode cache = (SomNode) get(index);
            if (cache.isLabeled()) {
                cache.addLabel(label);
            } else {
                cache.setLabel(label);
            }
            set(index, cache);
        }

    }

    private class SomNode {

        @Override
        public String toString() {
            String out = "";
            out += "\tVAL: " + Arrays.toString(values);
            out += "\n\tPOS: " + Arrays.toString(location);
            out += "\n\tLAB: " + label;
            out += "\n";
            return out;
        }

        private String label;

        private double[] values;

        private double[] location;

        /**
         * Main constructor.
         */
        private SomNode() {
            label = "";
            values = new double[1];
            location = new double[1];
        }

        /**
         * Main constructor (for input ArrayLists).
         *
         * @param String label - Name of this node.
         * @param double[] values - All the values of this node.
         */
        private SomNode(String label, Double[] values) {
            this.label = label;
            this.values = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                this.values[i] = values[i];
            }
            // this.values = values.clone();
            location = new double[1];
        }

        /**
         * Main constructor (for weight ArrayLists).
         *
         * @param String label - Name of this node.
         * @param double[] values - All the values of this node.
         * @param double[] location - The location of this node.
         */
        private SomNode(double[] values, double[] location) {
            label = "";
            this.values = values.clone();
            this.location = location.clone();
        }

        /**
         * Sets values for every dimension in this node.
         *
         * @param double[] values - Sets all the values for this node.
         */
        private void setValues(double[] values) {
            this.values = values.clone();
        }

        /**
         * Returns all the values of this node.
         *
         * @return double[] - Returns the numerical presentation of this node.
         */
        private double[] getValues() {
            return values.clone();
        }

        /**
         * Set the label name for this node.
         *
         * @param String - Label of this node.
         */
        private void setLabel(String label) {
            this.label = label;
        }

        /**
         * Set the secondary label(s) for this node.
         *
         * @param String - Another label of this node.
         */
        private void addLabel(String label) {
            this.label += ", " + label;
        }

        /**
         * Returns the label of this node.
         *
         * @return String - Returns the label of this node if any.
         */
        private String getLabel() {
            return label;
        }

        /**
         * Returns the location of this node.
         *
         * @return double[] - Returns the location of this node if any.
         */
        private double[] getLocation() {
            return location.clone();
        }

        /**
         * Returns the information about wether labeling has been done.
         *
         * @return boolean - Returns true if this node has been labeled
         * otherwise false.
         */
        private boolean isLabeled() {
            return label.length() > 0;
        }
    }

    private class JSomTraining {

        private int index; // caching

        private JSomMath math;

        // private WeightArrayLists wArrayList;
        private InputVectors iArrayList;

        private String neigh; // the neighborhood function type used ::

        // step(bubble) | gaussian
        private int steps; // running length (number of steps) in training

        private double lrate; // initial learning rate parameter value

        private String lrateType; // learning rate parameter type ::

        // exponential | linear | inverse
        private double width; // initial "width" of training area

        private Random generator;

        private int wArrayListSize; // the number of weight ArrayLists

        private int iArrayListSize; // the number of input ArrayLists

        /**
         * Constructor.
         *
         * @param WeightVectors wVector - weight Vector.
         * @param InputVector InputVector - input Vector.
         */
        private JSomTraining(InputVectors iVectors) {
            // this.wArrayList = wArrayList;
            this.iArrayList = iVectors;
            math = new JSomMath(wV.getDimensionalityOfNodes());
            generator = new Random();
        }

        /**
         * Sets the ordering instructions for the ordering process.
         *
         * @param int steps - number of steps in this ordering phase.
         * @param double lrate - initial value for learning rate (usually near
         * 0.1).
         * @param int radius - initial radius of neighbors.
         * @param String lrateType - states which learning-rate parameter
         * function is used :: exponential | linear | inverse
         * @param String neigh - the neighborhood function type used ::
         * step(bubble) | gaussian
         */
        private void setTrainingInstructions(int steps, double lrate, int radius, String lrateType, String neigh) {
            this.steps = steps;
            this.lrate = lrate;
            this.lrateType = lrateType;
            this.neigh = neigh;
            width = radius;
        }

        /**
         * Does the training phase.
         *
         * @return WeightArrayLists - Returns the trained weight ArrayLists.
         */
        private void doTraining() {
            // fireBatchStart("Training phase");
            iArrayListSize = iArrayList.getCount();
            wArrayListSize = wV.getCount();
            if (lrateType.equals("exponential") && neigh.equals("step")) {
                doBubbleExpAdaptation();
            } else if (lrateType.equals("linear") && neigh.equals("step")) {
                doBubbleLinAdaptation();
            } else if (lrateType.equals("inverse") && neigh.equals("step")) {
                doBubbleInvAdaptation();
            } else if (lrateType.equals("exponential") && neigh.equals("gaussian")) {
                doGaussianExpAdaptation();
            } else if (lrateType.equals("linear") && neigh.equals("gaussian")) {
                doGaussianLinAdaptation();
            } else {
                // inverse and gaussian
                doGaussianInvAdaptation();
            }
            // return wV;
        }

        /*
         * Does the Bubble Exponential Adaptation to the Weight ArrayLists.
         */
        private void doBubbleExpAdaptation() {
            double[] input;
            double[] wLocation; // location of a winner node
            double s = (double) steps;
            double wCache; // width cache
            double exp;
            for (int n = 0; n < steps; n++) {
                wCache = Math.ceil(width * (1 - (n / s))); // adapts the width
                // function as it is
                // a function of
                // time.
                exp = math.expLRP(n, lrate, steps);
                input = iArrayList.getNodeValuesAt(generator.nextInt(iArrayListSize));
                index = resolveIndexOfWinningNeuron(input);
                wLocation = wV.getNodeLocationAt(index);
                for (int h = 0; h < wArrayListSize; h++) {
                    wV.setNodeValuesAt(h, math.bubbleAdaptation(input, wV.getNodeValuesAt(h), wLocation, wV
                            .getNodeLocationAt(h), wCache, exp));
                }
            }
        }

        /*
         * Does the Bubble Linear Adaptation to the Weight ArrayLists.
         */
        private void doBubbleLinAdaptation() {
            double[] input;
            double[] wLocation; // location of a winner node
            double s = (double) steps;
            double wCache; // width cache
            double lin;
            for (int n = 0; n < steps; n++) {
                wCache = Math.ceil(width * (1 - (n / s))); // adapts the width
                // function as it is
                // a function of
                // time.
                lin = math.linLRP(n, lrate, steps);
                input = iArrayList.getNodeValuesAt(generator.nextInt(iArrayListSize));
                index = resolveIndexOfWinningNeuron(input);
                wLocation = wV.getNodeLocationAt(index);
                for (int h = 0; h < wArrayListSize; h++) {
                    wV.setNodeValuesAt(h, math.bubbleAdaptation(input, wV.getNodeValuesAt(h), wLocation, wV
                            .getNodeLocationAt(h), wCache, lin));
                }
            }
        }

        /*
         * Does the Bubble Inverse-time Adaptation to the Weight ArrayLists.
         */
        private void doBubbleInvAdaptation() {
            double[] input;
            double[] wLocation; // location of a winner node
            double A; // constants A and B which are considered equal
            double s = (double) steps;
            double wCache; // width cache
            double inv;
            A = steps / 100.0;
            for (int n = 0; n < steps; n++) {
                wCache = Math.ceil(width * (1 - (n / s))); // adapts the width
                // function as it is
                // a function of
                // time.
                inv = math.invLRP(n, lrate, A, A);
                input = iArrayList.getNodeValuesAt(generator.nextInt(iArrayListSize));
                index = resolveIndexOfWinningNeuron(input);
                wLocation = wV.getNodeLocationAt(index);
                for (int h = 0; h < wArrayListSize; h++) {
                    wV.setNodeValuesAt(h, math.bubbleAdaptation(input, wV.getNodeValuesAt(h), wLocation, wV
                            .getNodeLocationAt(h), wCache, inv));
                }
            }
        }

        /*
         * Does the Gaussian Exponential Adaptation to the Weight ArrayLists.
         */
        private void doGaussianExpAdaptation() {
            // update the weightArrayLists with random instances.
            // the steps variable is the number of times this update is done
            for (int n = 0; n < steps; n++) {
                double wCache = math.gaussianWidth(width, n, steps);
                double exp = math.expLRP(n, lrate, steps);
                double[] input = iArrayList.getNodeValuesAt(generator.nextInt(iArrayListSize));
                index = resolveIndexOfWinningNeuron(input);
                // winning node
                double[] wLocation = wV.getNodeLocationAt(index);
                for (int h = 0; h < wArrayListSize; h++) {
                    wV.setNodeValuesAt(h, math.gaussianAdaptation(input, wV.getNodeValuesAt(h), wLocation, wV
                            .getNodeLocationAt(h), wCache, exp));
                }
            }
        }

        /*
         * Does the Gaussian Linear Adaptation to the Weight ArrayLists.
         */
        private void doGaussianLinAdaptation() {
            double[] input;
            double[] wLocation; // location of a winner node
            double wCache; // width cache
            double lin;
            for (int n = 0; n < steps; n++) {
                wCache = math.gaussianWidth(width, n, steps);
                lin = math.linLRP(n, lrate, steps);
                input = iArrayList.getNodeValuesAt(generator.nextInt(iArrayListSize));
                index = resolveIndexOfWinningNeuron(input);
                wLocation = wV.getNodeLocationAt(index);
                for (int h = 0; h < wArrayListSize; h++) {
                    wV.setNodeValuesAt(h, math.gaussianAdaptation(input, wV.getNodeValuesAt(h), wLocation, wV
                            .getNodeLocationAt(h), wCache, lin));
                }
            }
        }

        /*
         * Does the Gaussian Inverse-time Adaptation to the Weight ArrayLists.
         */
        private void doGaussianInvAdaptation() {
            double[] input;
            double[] wLocation; // location of a winner node
            double A; // constants A and B which are considered equal
            double wCache; // width cache
            double inv;
            A = steps / 100.0;
            for (int n = 0; n < steps; n++) {
                wCache = math.gaussianWidth(width, n, steps);
                inv = math.invLRP(n, lrate, A, A);
                input = iArrayList.getNodeValuesAt(generator.nextInt(iArrayListSize));
                index = resolveIndexOfWinningNeuron(input);
                wLocation = wV.getNodeLocationAt(index);
                for (int h = 0; h < wArrayListSize; h++) {
                    wV.setNodeValuesAt(h, math.gaussianAdaptation(input, wV.getNodeValuesAt(h), wLocation, wV
                            .getNodeLocationAt(h), wCache, inv));
                }
            }
        }
    }

    /**
     * Enumeration of all grid types that are supported in a self organizing
     * map.
     *
     * @author Thomas Abeel
     *
     */
    public enum GridType {
        /**
         * Hexagonal grid
         */
        HEXAGONAL("hexa"),
        /**
         * Rectangular grid
         */
        RECTANGLES("rect");
        private String tag;

        private GridType(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    public enum NeighbourhoodFunction {
        GAUSSIAN("gaussian"), STEP("step");
        private String tag;

        private NeighbourhoodFunction(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    public enum LearningType {
        EXPONENTIAL("exponential"), INVERSE("inverse"), LINEAR("linear");
        private String tag;

        private LearningType(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    /**
     * Create a 2 by 2 Self-organizing map, using a hexagonal grid, 1000
     * iteration, 0.1 learning rate, 8 initial radius, linear learning, a
     * step-wise neighborhood function and the Euclidean distance as distance
     * measure.
     */
    public SOM() {
        this(2, 2, GridType.HEXAGONAL, 1000, 0.1, 8, LearningType.LINEAR, NeighbourhoodFunction.STEP);
    }

    private GridType gridType;

    private LearningType learningType;

    private NeighbourhoodFunction neighbourhoodFunction;

    private int xdim, ydim, iterations, initialRadius;

    private double learningRate;

    private DistanceMeasure dm;

    /**
     *
     * Create a new self-organizing map with the provided parameters and the
     * Euclidean distance as distance metric.
     *
     * @param xdim number of dimension on x-axis
     * @param ydim number of dimension on y-axis
     * @param grid type of grid
     * @param iterations number of iterations
     * @param learningRate learning rate of the algorithm
     * @param initialRadius initial radius
     * @param learning type of learning to use
     * @param nbf neighborhood function
     */
    public SOM(int xdim, int ydim, GridType grid, int iterations, double learningRate, int initialRadius,
            LearningType learning, NeighbourhoodFunction nbf) {
        this(xdim, ydim, grid, iterations, learningRate, initialRadius, learning, nbf, new EuclideanDistance());
    }

    /**
     *
     * Create a new self-organizing map with the provided parameters.
     *
     * @param xdim number of dimension on x-axis
     * @param ydim number of dimension on y-axis
     * @param grid type of grid
     * @param iterations number of iterations
     * @param learningRate learning rate of the algorithm
     * @param initialRadius initial radius
     * @param learning type of learning to use
     * @param nbf neighborhood function
     * @param dm distance measure to use
     */
    public SOM(int xdim, int ydim, GridType grid, int iterations, double learningRate, int initialRadius,
            LearningType learning, NeighbourhoodFunction nbf, DistanceMeasure dm) {
        this.gridType = grid;
        this.learningType = learning;
        this.neighbourhoodFunction = nbf;
        this.xdim = xdim;
        this.ydim = ydim;
        this.iterations = iterations;
        this.learningRate = learningRate;
        this.initialRadius = initialRadius;
        this.dm = dm;
    }

    private WeightVectors wV;

    public Dataset[][] cluster(Dataset data) {
        // hexa || rect
        wV = new WeightVectors(xdim, ydim, data.noAttributes(), gridType.toString());
        InputVectors iV = convertDataset(data);
        JSomTraining jst = new JSomTraining(iV);
        // exponential || inverse || linear
        // gaussian || step
        jst.setTrainingInstructions(iterations, learningRate, initialRadius, learningType.toString(),
                neighbourhoodFunction.toString());
        // WeightArrayLists out = jst.doTraining();
        jst.doTraining();

        ArrayList<Dataset> clusters = new ArrayList<>();
        for (int i = 0; i < wV.size(); i++) {
            clusters.add(new DefaultDataset());
        }

        wV = doLabeling(wV, iV, data, clusters);

        Dataset[][] grid = new Dataset[xdim][ydim];

        for (int x = 0; x < xdim; x++) {
            for (int y = 0; y < ydim; y++) {
                grid[x][y] = clusters.get((y * xdim) + x);
            }
        }

        /*// Filter empty clusters out;
        int nonEmptyClusterCount = 0;
        for (int i = 0; i < clusters.size(); i++) {
            if (clusters.get(i).size() > 0)
                nonEmptyClusterCount++;
        }
        Dataset[] output = new Dataset[nonEmptyClusterCount];
        int index = 0;
        for (Dataset tmp : clusters) {
            if (tmp.size() > 0) {
                output[index] = tmp;
                index++;
            }
        }
        return output;*/
        return grid;
    }

    /**
     * Does the labeling phase.
     *
     * @return WeightArrayLists - Returns the labeled weight ArrayLists.
     */
    private WeightVectors doLabeling(WeightVectors wArrayList, InputVectors iArrayList, Dataset data, ArrayList<Dataset> clusters) {

        for (int i = 0; i < data.size(); i++) {
            int index = resolveIndexOfWinningNeuron(iArrayList.getNodeValuesAt(i));
            clusters.get(index).add(data.instance(i));
            wArrayList.setNodeLabelAt(index, iArrayList.getNodeLabelAt(i));
        }
        return wArrayList;
    }

    /**
     * Finds the winning neuron for this input ArrayList. Determines the winning
     * neuron by calculating the distance of two ArrayLists.
     *
     * @param double[] values - values of an input ArrayList.
     * @return int - index of the winning neuron.
     */
    private int resolveIndexOfWinningNeuron(double[] values) {
        double bestDistance = getDistance(values, wV.getNodeValuesAt(0));
        int index = 0;
        for (int i = 1; i < wV.size(); i++) {
            double dist = getDistance(values, wV.getNodeValuesAt(i));
            if (dist < bestDistance) {
                index = i;
                bestDistance = dist;
            }
        }
        return index;
    }

    private InputVectors convertDataset(Dataset data) {
        InputVectors iVS = new InputVectors();
        for (int i = 0; i < data.size(); i++) {
            Double[] values = data.instance(i).values().toArray(new Double[0]);
            SomNode tmp = new SomNode("node_" + i, values);
            iVS.add(tmp);
        }
        return iVS;
    }

}
