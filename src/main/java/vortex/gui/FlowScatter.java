
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import util.logger;

/**
 *
 * @author Nikolay
 */
public class FlowScatter extends JPanel {

    List<Series> series = new ArrayList<>();
    BufferedImage img;
    double margin = 0.025;
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;

    private RepaintWorker worker = null;

    private class RepaintWorker extends SwingWorker<BufferedImage, BufferedImage> {

        @Override
        protected BufferedImage doInBackground() throws Exception {
            logger.print("started");
            BufferedImage bi = generateImage();
            publish(bi);
            logger.print("did in bg and published");
            return bi;
        }

        @Override
        protected void process(List<BufferedImage> chunks) {
            if (chunks.size() > 0) {
                BufferedImage bi = chunks.get(0);
                img = bi;
                repaint();
                logger.print("processed");
                return;
            }
            logger.print("not processed");
        }
    };

    @Override
    protected void paintComponent(Graphics g) {
        if (img == null) {
            img = generateImage();
        }
        g.drawImage(img, 0, 0, null);
        g.setColor(Color.black);
        g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
    }

    public FlowScatter() {
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {

                if (worker != null) {
                    logger.print("cancelling");
                    worker.cancel(true);

                }
                worker = new RepaintWorker();
                worker.execute();
            }
        });
    }
    
    
    private int scatterPointSize = 1;

    public void setScatterPointSize(int scatterPointSize) {
        this.scatterPointSize = scatterPointSize;
        this.img = generateImage();
        this.repaint();
    }

    public int getScatterPointSize() {
        return scatterPointSize;
    }
    
    

    public BufferedImage generateScatterImage(Series s) {
        BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[][] binnedDS = new int[bi.getWidth()][bi.getHeight()];
        double maxSumBin = 0;
        for (double[] d : s.data) {
            int x = (int) (((d[0] - minX) / (maxX - minX)) * (w - 1));
            int y = (int) (((d[1] - minY) / (maxY - minY)) * (h - 1));
            binnedDS[x][y]++;
            maxSumBin = Math.max(binnedDS[x][y], maxSumBin);
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (Thread.interrupted()) {
                    return null;
                }
                if (binnedDS[x][y] > 0) {
                    g.setPaint(s.c);
                    g.fillOval(x, y, scatterPointSize, scatterPointSize);
                }
            }
        }
        return bi;
    }

    public BufferedImage generateDensityImage(Series s) {
        BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[][] binnedDS = new int[bi.getWidth()][bi.getHeight()];
        for (double[] d : s.data) {
            if (Thread.interrupted()) {
                return null;
            }
            int x = (int) (((d[0] - minX) / (maxX - minX)) * (w - 1));
            int y = (int) (((d[1] - minY) / (maxY - minY)) * (h - 1));
            binnedDS[x][y]++;
        }
        double maxSumBin = 0;
        for (double[] d : s.data) {
            if (Thread.interrupted()) {
                return null;
            }
            int x = (int) (((d[0] - minX) / (maxX - minX)) * (w - 1));
            int y = (int) (((d[1] - minY) / (maxY - minY)) * (h - 1));
            binnedDS[x][y]++;
            maxSumBin = Math.max(binnedDS[x][y], maxSumBin);
        }
        int sX = (int) ((s.sigmaX / (maxX - minX)) * w);
        int sY = (int) ((s.sigmaY / (maxY - minY)) * h);
        int kw = sX * 9;
        int kh = sY * 9;
        int kCX = kw / 2;
        int kCY = kh / 2;
        double[][] kernel = new double[kw][kh];
        Kernel k = new Kernel(sX, sY);
        for (int x = 0; x < kernel.length; x++) {
            for (int y = 0; y < kernel[0].length; y++) {
                if (Thread.interrupted()) {
                    return null;
                }
                kernel[x][y] = k.getKernelWeight(x, y, kCX, kCY);
            }
        }
        double[][] convovled = new double[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (Thread.interrupted()) {
                    return null;
                }
                if (binnedDS[x][y] == 0) {
                    continue;
                }
                for (int kx = 0; kx < kernel.length; kx++) {
                    for (int ky = 0; ky < kernel[0].length; ky++) {
                        int imgX = x + (kx - kCX);
                        if (imgX < 0 || imgX >= w) {
                            continue;
                        }
                        int imgY = y + (ky - kCY);
                        if (imgY < 0 || imgY >= h) {
                            continue;
                        }
                        convovled[imgX][imgY] += Math.max(0, (binnedDS[x][y] * kernel[kx][ky]) - 0.001);
                    }
                }
            }
        }
        double maxSumW = 0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                maxSumW = Math.max(maxSumW, binnedDS[x][y]);
            }
        }
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (convovled[x][y] > 0) {
                    g.setPaint(s.c);
                    g.fillRect(x, y, 1, 1);
                }
            }
        }

        return bi;
    }

    public BufferedImage generateContourImage(Series s, int numContours) {
        BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[][] binnedDS = new int[bi.getWidth()][bi.getHeight()];
        double maxSumBin = 0;
        for (double[] d : s.data) {
            int x = (int) (((d[0] - minX) / (maxX - minX)) * (w - 1));
            int y = (int) (((d[1] - minY) / (maxY - minY)) * (h - 1));
            binnedDS[x][y]++;
            maxSumBin = Math.max(binnedDS[x][y], maxSumBin);
        }

        int sX = (int) ((s.sigmaX / (maxX - minX)) * w);
        int sY = (int) ((s.sigmaY / (maxY - minY)) * h);
        int kw = sX * 9;
        int kh = sY * 9;
        int kCX = kw / 2;
        int kCY = kh / 2;
        double[][] kernel = new double[kw][kh];
        Kernel k = new Kernel(sX, sY);
        for (int x = 0; x < kernel.length; x++) {
            for (int y = 0; y < kernel[0].length; y++) {
                kernel[x][y] = k.getKernelWeight(x, y, kCX, kCY);
            }
        }
        double[][] convovled = new double[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (binnedDS[x][y] == 0) {
                    continue;
                }
                for (int kx = 0; kx < kernel.length; kx++) {
                    for (int ky = 0; ky < kernel[0].length; ky++) {
                        int imgX = x + (kx - kCX);
                        if (imgX < 0 || imgX >= w) {
                            continue;
                        }
                        int imgY = y + (ky - kCY);
                        if (imgY < 0 || imgY >= h) {
                            continue;
                        }
                        if (Thread.interrupted()) {
                            return null;
                        }
                        convovled[imgX][imgY] += Math.max(0, (binnedDS[x][y] * kernel[kx][ky]) - 0.001);
                    }
                }
            }
        }

        double maxSumW = 0;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (convovled[x][y] > maxSumW) {
                    if (Thread.interrupted()) {
                        return null;
                    }
                    maxSumW = Math.max(maxSumW, convovled[x][y]);
                }
            }
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                convovled[x][y] /= maxSumW;
            }
        }

        ArrayList<GeneralPath>[] pathsByThs = new ArrayList[numContours + 1];

        for (int i = 0; i < pathsByThs.length; i++) {
            pathsByThs[i] = new ArrayList<>();
        }
        int currContour = 0;

        double stepSize = 1.0 / numContours;
        for (double th = stepSize; th < 1.0; th += stepSize) {
            double ths = th;
            for (int x = 0; x < w - 1; x += 1) {
                for (int y = 0; y < h - 1; y += 1) {
                    if (Thread.interrupted()) {
                        return null;
                    }

                    boolean startX = x == 0 && convovled[x][y] > ths;
                    boolean startY = y == 0 && convovled[x][y] > ths;
                    boolean endX = x == w - 2 && convovled[x + 1][y] > ths;
                    boolean endY = y == h - 2 && convovled[x][y + 1] > ths;
                    boolean gY = (convovled[x][y] - ths) * (convovled[x][y + 1] - ths) < 0;
                    boolean gX = (convovled[x][y] - ths) * (convovled[x + 1][y] - ths) < 0;

                    if (gY || gX || startX || startY || endX || endY) {

                        GeneralPath closestCurve = null;
                        double minDist = 3.0;
                        for (GeneralPath p : pathsByThs[currContour]) {
                            Point2D pt = p.getCurrentPoint();
                            if (pt.distance(x, y) < minDist) {
                                closestCurve = p;
                                minDist = pt.distance(x, y);
                            }
                        }
                        if (closestCurve != null) {
                            closestCurve.lineTo(x, y);
                        } else {
                            GeneralPath crv = new GeneralPath();
                            crv.moveTo(x, y);
                            pathsByThs[currContour].add(crv);
                        }
                    }
                }

            }
            currContour++;
        }
        boolean filled = false;
        if (filled) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (convovled[x][y] > 0) {
                        g.setPaint(s.c);
                        g.fillRect(x, y, 1, 1);
                    }
                }
            }
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (Thread.interrupted()) {
                    return null;
                }
                if (binnedDS[x][y] > 0 && convovled[x][y] < stepSize) {
                    g.setPaint(s.c);
                    //g.setColor(new Color(s.c.getRed(), s.c.getGreen(), s.c.getBlue(), (int) ((binnedDS[x][y] / maxSumBin) * 255)));
                    g.fillOval(x, y, 1, 1);
                }
            }
        }

        g.setPaint(s.c);
        for (ArrayList<GeneralPath> paths : pathsByThs) {
            for (GeneralPath curve : paths) {
                if (Thread.interrupted()) {
                    return null;
                }

                //curve.closePath();
                if (!filled) {
                    g.draw(curve);
                }
            }
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (Thread.interrupted()) {
                    return null;
                }
                if (binnedDS[x][y] > 0) {
                    g.setPaint(s.c);// (int) Math.min(255, ((binnedDS[x][y] / maxSumBin) * 255))));
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        return bi;
    }

    public BufferedImage generateImage() {
        BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = bi.getWidth();
        int h = bi.getHeight();
        g.setPaint(Color.WHITE);
        g.fillRect(0, 0, w, h);
        for (Series s : series) {
            BufferedImage bimg = null;
            switch (s.type) {
                case SCATTER:
                    bimg = generateScatterImage(s);
                    break;
                case DENSITY:
                    bimg = generateDensityImage(s);
                    break;
                case CONTOUR:
                    bimg = generateContourImage(s, 15);
                    break;
            }
            if (bimg != null) {
                AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
                tx.translate(0, -bimg.getHeight(null));
                AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                ((Graphics2D) g).drawImage(op.filter(bimg, null), null, 0, 0);
            }
        }
        ((Graphics2D) g).setPaint(Color.BLACK);
        ((Graphics2D) g).drawRect(0, 0, w - 1, h - 1);
        Graphics2D g2 = ((Graphics2D) g);

        final boolean ASINH_SCALE = false;
        if (ASINH_SCALE) {
            int maxLog = 6;
            double prevX = 30;
            double prevY = h - 30;
            for (int log = 0; log < maxLog; log++) {
                double base = Math.pow(10, log);
                for (double tick = 0; tick < 10; tick++) {
                    double xx = base * tick;
                    double scaledXX = xx / 5;
                    double asinhXTick = Math.log(scaledXX + Math.sqrt(scaledXX * scaledXX + 1));
                    int scrX = (int) (w * (asinhXTick - minX) / (maxX - minX));
                    if (scrX > 30) {
                        g2.drawLine(scrX, h, scrX, h - 5);
                        String str = String.valueOf(xx).split("\\.")[0];
                        double width = g2.getFontMetrics().getStringBounds(str, g2).getWidth();
                        int currX = (int) (scrX + 1 - width / 2);
                        if (currX > prevX && currX + width < w) {
                            g2.drawString(String.valueOf(xx).split("\\.")[0], currX, h - 6);
                            prevX = currX + width;
                        }
                    }

                    int scrY = h - (int) (h * (asinhXTick - minY) / (maxY - minY));

                    if (scrY > 30 && scrY < h - 30) {
                        g2.drawLine(0, scrY, 5, scrY);
                        int currY = scrY + (g2.getFont().getSize() / 2);
                        if (currY < prevY) {
                            g2.drawString(String.valueOf(xx).split("\\.")[0], 6, currY);
                            prevY = currY - g2.getFont().getSize();
                        }
                    }
                }
            }
        } else {

            double prevX = 30;
            double prevY = h - 30;

            for (double tick = minX; tick < maxX; tick += ((maxX - minX) / 100)) {
                int scrX = (int) (w * (tick - minX) / (maxX - minX));

                String val = String.valueOf(((int) (tick * 100)) / 100.0);

                if (scrX > 30) {

                    double width = g2.getFontMetrics().getStringBounds(val, g2).getWidth()+3;
                    int currX = (int) (scrX + 1 - width / 2);
                    if (currX > prevX && currX + width < w) {
                        g2.drawLine(scrX, h, scrX, h - 5);
                        g2.drawString(val, currX, h - 6);
                        prevX = currX + width;
                    }
                }

                int scrY = h - (int) (h * (tick - minY) / (maxY - minY));

                if (scrY > 30 && scrY < h - 30) {

                    int currY = scrY + (g2.getFont().getSize() / 2);
                    if (currY < prevY) {
                        g2.drawLine(0, scrY, 5, scrY);
                        g2.drawString(val, 6, currY);
                        prevY = currY - g2.getFont().getSize();
                    }
                }
            }

        }
        if (!Thread.interrupted()) {
            // new CopyImageToClipboard(bi);
        }
        return bi;
    }

    public static class Series {

        double[][] data;
        String name;
        int size;
        Paint c;
        double sigmaX;
        double sigmaY;

        public enum Type {

            SCATTER, DENSITY, CONTOUR;
        };

        private Type type;

        public Type getType() {
            return type;
        }

        public Series(String name, Paint c, double[][] data, double smoothing, Type type) {
            this.size = data.length;
            this.c = c;
            this.data = data;
            this.name = name;

            double avgX = 0, avgY = 0;
            for (double[] d : data) {
                avgX += d[0];
                avgY += d[1];
            }
            avgX /= data.length;
            avgY /= data.length;
            double sdX = 0, sdY = 0;
            for (double[] d : data) {
                sdX += Math.pow(d[0] - avgX, 2);
                sdY += Math.pow(d[1] - avgY, 2);
            }
            sdX = Math.sqrt(sdX / data.length);
            sdY = Math.sqrt(sdY / data.length);
            sigmaX = smoothing * sdX * Math.pow(data.length, -0.2);
            sigmaY = smoothing * sdY * Math.pow(data.length, -0.2);
            this.type = type;
        }
    }

    public void reset() {
        series.clear();
        img = null;
        minX = Double.MAX_VALUE;
        maxX = -Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;
    }

    public void addSeries(Series s) {
        series.add(s);
        
        for (int z = 0; z < 2; z++) {
            double[] tmp = new double[s.data.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = s.data[i][z];
            }
        }

        for (double[] d : s.data) {
            maxX = Math.max(d[0], maxX);
            maxY = Math.max(d[1], maxY);
            minX = Math.min(d[0], minX);
            minY = Math.min(d[1], minY);
        }
        double mX = (maxX - minX) * margin;
        minX -= mX;
        maxX += mX;
        double mY = (maxY - minY) * margin;
        minY -= mY;
        maxY += mY;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FlowScatter fs = new FlowScatter();

        Series s = new Series("def", Color.red, new double[][]{
            {0, 0},
            {1, 1},
            {2, 2},
            {3, 3},
            {4, 4},
            {5, 5},
            {6, 6},
            {3, 0},
            {3, 1},
            {3, 2},
            {3, 3},
            {3, 4},
            {3, 5},
            {3, 6},
            {3.5, 1.5},
            {3.7, 2.7},
            {3.1, 2.1},
            {3.6, 3.6},
            {3.93, 4.4},
            {3.5, 5.5},
            {3.3, 6.6}
        }, 1.06, Series.Type.CONTOUR);
        fs.addSeries(s);
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        f.add(fs);
        f.setBounds(500, 500, 200, 200);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

    public static class Kernel {

        private final double sX, sY, c;

        public Kernel(double sX, double sY) {
            this.sX = sX;
            this.sY = sY;
            this.c = 1.0 / (2 * Math.PI * Math.sqrt(sX * sY));
        }

        private double getKernelWeight(double x, double y, double centerX, double centerY) {
            double dx = (x - centerX) / (2 * sX);
            double dy = (y - centerY) / (2 * sY);
            double dist = dx * dx + dy * dy;
            return c * Math.exp(-dist);
        }
    }
}
