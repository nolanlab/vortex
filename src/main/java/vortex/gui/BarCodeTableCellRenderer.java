/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui;

import clustering.BarCode;
import java.awt.Component;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import util.ColorScale;
import vortex.util.Config;

/**
 *
 * @author Nikolay
 */
public class BarCodeTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;
    private int width = 0;
    private int height = 0;
    private BarCode bc = null;
    private boolean odd = false;
    private static BarCodeOptions conf = null;

    public static void setConfig(BarCodeOptions newConf) {
        conf = newConf;
    }
    
    

    public static BarCodeOptions getOptions() {
        return conf;
    }

    public static class BarCodeOptions {

        private Color COLOR_UP;
        private Color COLOR_DOWN;
        private BarcodePaintStyle style;
        private double pos_neg_ratio;
        private boolean Rainbow;
        
        public BarCodeOptions(Color COLOR_UP, Color COLOR_DOWN, BarcodePaintStyle style, double pos_neg_ratio, boolean useRainbowPalette) {
            this.COLOR_UP = COLOR_UP;
            this.COLOR_DOWN = COLOR_DOWN;
            this.style = style;
            this.pos_neg_ratio = pos_neg_ratio;
            this.Rainbow = useRainbowPalette;
            
        }

        public Color getCOLOR_DOWN() {
            return COLOR_DOWN;
        }

        public Color getCOLOR_UP() {
            return COLOR_UP;
        }

        public BarcodePaintStyle getStyle() {
            return style;
        }

        public double getPos_neg_ratio() {
            return pos_neg_ratio;
        }
    }

    public static enum BarcodePaintStyle {
        DASHES, STRIPES
    }
    String[] paramNames;

    public BarCodeTableCellRenderer() {
        super();
        if (conf == null) {
            conf = Config.loadBarCodeOptions();
        }
    }

    @Override
    public String getToolTipText(MouseEvent arg0) {
        if (bc != null && arg0.getX() < width) {
            int idx = (int) Math.floor(((arg0.getX()) * paramNames.length) / (double) width);
            return paramNames[idx] + ": " + bc.getRawValues()[idx];
        } else {
            return super.getToolTipText(arg0);
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.setValue(value);
        odd = (row % 2 == 1);
        return this;
    }

    private Color getColor(double val){
        
                        if(conf.Rainbow){
                            Color col = (val > 0) ? ColorScale.getRainbowColorForValue(val): new Color (0,0,(int)(200+(Math.max(val, -1.0)*200.0)));
                            return col;
                        }
                        return ((val > 0) ? conf.getCOLOR_UP() : conf.getCOLOR_DOWN());
    }
    
    @Override
    public void paintComponent(Graphics arg0) {
        super.paintComponent(arg0);
        if (bc != null) {
            Graphics2D g = (Graphics2D) arg0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            width = this.getWidth();
            height = this.getHeight();
            double[] data = bc.getProfile();
            double step = (double) width / data.length;
            g.setPaint(Color.WHITE);
            g.fillRect(0, 0, width, height);

            switch (conf.getStyle()) {
                case STRIPES:
                    for (int i = 0; i < data.length; i++) {
                        double val = data[i];
                        float[] comp = getColor(val).getRGBColorComponents(null);
                        g.setPaint(new Color(comp[0], comp[1], comp[2], conf.Rainbow?1.0f:(float) Math.min(Math.abs(val), 1.0)));
                        Rectangle2D rect = new Rectangle2D.Double(i * step, 0, step, height);
                        g.fill(rect);
                    }
                    break;
                case DASHES:
                    if (odd) {
                        g.setPaint(new Color(242, 242, 242));
                        g.fillRect(-1, -1, width + 3, height + 1);
                    }
                    for (int i = 0; i < data.length; i++) {
                        double val = data[i];
                        g.setPaint(getColor(val));
                        Rectangle2D rect = (val > 0) ? new Rectangle2D.Double(i * step, (height / 2) * (1 - val), (step * 0.8), (height * val) / 2)
                                : new Rectangle2D.Double(i * step, (height / 2), (step * 0.8), (height * Math.abs(val)) / 2);
                        g.fill(rect);
                    }
                    g.setPaint(new Color(127, 127, 147, 50));
                    Line2D line = new Line2D.Double(0, height / 2, width, height / 2);
                    g.draw(line);
                    break;
                default:
                    break;
            }
            if (bc.getSideVectorBeginIdx() > 0 && bc.getSideVectorBeginIdx() < data.length) {
                g.setPaint(new Color(0, 0, 0, 30));
                Rectangle2D r = new Rectangle2D.Double(bc.getSideVectorBeginIdx() * step, 0, width, height);
                g.draw(r);
            }
            //g.setPaint(new Color(70, 110, 155));
            // g.drawRect(0, -1, this.getWidth() - 1, height + 1);
            //g.drawString("Barcode", 0, height);
        }
    }

    @Override
    public void setValue(Object value) {
        if (value == null) {
            bc = null;
            super.setValue(value);
            return;
        }
        if (BarCode.class.isAssignableFrom(value.getClass())) {
            bc = (BarCode) value;
            paramNames = bc.getParameterNames();
        } else {
            bc = null;
            super.setValue(value);
        }
    }
}
