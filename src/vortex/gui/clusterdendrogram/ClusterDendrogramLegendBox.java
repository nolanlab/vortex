/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui.clusterdendrogram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;

/**
 *
 * @author Nikolay
 */
class ClusterDendrogramLegendBox extends JLabel implements zOrdered {

    private static final long serialVersionUID = 1L;
    private GridBagConstraints gbConst;
    private String caption = "";
    private Rectangle initBounds;
    private int initialDragX = -1;
    private Color lblColor;

    @Override
    public int getZOrder() {
        if (hasFocus()) {
            return 2;
        }
        if (getCaption().length() > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public GridBagConstraints getGridBagConstraints() {
        return gbConst;
    }

    public void setGridBagConstraints(GridBagConstraints gbConst) {
        this.gbConst = gbConst;
    }

    public void cpMouseDragged(java.awt.event.MouseEvent evt) {
        if (initialDragX != -1) {
            super.setBounds(initBounds.x + ((evt.getXOnScreen() - initialDragX)), initBounds.y, initBounds.width, initBounds.height);
        }
    }

    private void initDrag(MouseEvent e) {
        initialDragX = e.getXOnScreen();
        initBounds = this.getBounds();
    }

    private void cpMousePressed(MouseEvent e) {
        this.initDrag(e);
    }

    public ClusterDendrogramLegendBox(String label, Color lblColor) {
        this.lblColor = lblColor;
        this.caption = label;
        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                cpMouseDragged(evt);
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                cpMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                initialDragX = -1;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                e.getComponent().requestFocus();
            }
        });
        this.setFocusable(true);
    }

    @Override
    public void printAll(Graphics g) {
        for (Component f : getComponents()) {
            if (!(f instanceof ClusterDendrogramLegendBox)) {
                f.setVisible(false);
            }
        }
        paintAll(g);
        for (Component f : getComponents()) {
            f.setVisible(true);
        }
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

    @Override
    protected void paintComponent(Graphics g) {
        final int round_coeff = 1;
        RoundRectangle2D rect = new RoundRectangle2D.Double(0, 0, this.getWidth(), this.getHeight(), round_coeff, round_coeff);
        Graphics2D g2 = (Graphics2D) g;

        Shape tmp = g2.getClip();
        g2.clip(rect);

        g2.setPaint(new LinearGradientPaint(0.0f, 0.0f, 1.0f, 1.0f, new float[]{0.0f, 1.0f}, new Color[]{new Color(lblColor.getRed(), lblColor.getGreen(), lblColor.getBlue(), 180), lblColor}, CycleMethod.REFLECT));

        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setClip(tmp);
        try {
            g2.setPaint(((LineBorder) getBorder()).getLineColor());
        } catch (Exception e) {
            g2.setPaint(new Color(50, 50, 50));
        }
        if (this.hasFocus()) {
            g2.setStroke(new BasicStroke(5));
        } else {
            g2.setStroke(new BasicStroke(1));
        }

        g2.draw(new RoundRectangle2D.Double(0, 0, this.getWidth() - 2, this.getHeight() - 1, round_coeff, round_coeff));




        String dispStr = caption;

        if (dispStr.length() > 0) {

            Font f = g2.getFont();

            g2.setFont(getFont().deriveFont(Font.BOLD));

            FontRenderContext frc = g2.getFontRenderContext();
            TextLayout layout = new TextLayout(dispStr, g2.getFont(), frc);

            Rectangle2D r = layout.getBounds();

            int strX = 5;

            float[] strCoord = new float[]{strX, ((this.getHeight() + (float) r.getHeight()) / 2.0f)};

            if (getWidth() > 2) {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0));

                layout.draw(g2, -1f + strCoord[0], 1f + strCoord[1]);
                layout.draw(g2, -1f + strCoord[0], -1f + strCoord[1]);
                layout.draw(g2, 1f + strCoord[0], 1f + strCoord[1]);
                layout.draw(g2, 1f + strCoord[0], -1f + strCoord[1]);

                layout.draw(g2, 1f + strCoord[0], strCoord[1]);
                layout.draw(g2, strCoord[0], 1f + strCoord[1]);
                layout.draw(g2, -1f + strCoord[0], strCoord[1]);
                layout.draw(g2, strCoord[0], -1f + strCoord[1]);

                g2.setColor(new Color(255, 255, 255));
                layout.draw(g2, strCoord[0], strCoord[1]);
                // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            g2.setFont(f);
        }

    }
}