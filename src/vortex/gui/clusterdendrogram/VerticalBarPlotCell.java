/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.gui.clusterdendrogram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import javax.swing.JLabel;

/**
 *
 * @author Nikolay
 */
public class VerticalBarPlotCell extends JLabel {

    private static final long serialVersionUID = 1L;
    private double value;
    private double[] valueRange;
    private Color fontColor;
    private Color fontGlowColor;

    public double getValue() {
        return value;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }

    public Color getFontColor() {
        return fontColor;
    }

    public void setFontGlowColor(Color fontGlowColor) {
        this.fontGlowColor = fontGlowColor;
    }

    public Color getFontGlowColor() {
        return fontGlowColor;
    }

    public VerticalBarPlotCell(String label, double value, double[] valueRange) {
        super();
        setText(label);
        this.setForeground(Color.GRAY);
        this.value = value;
        this.valueRange = valueRange;
        this.fontColor = Color.BLACK;
        this.fontGlowColor = new Color(255, 255, 255, 200);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        if (valueRange != null) {

            //g2.setPaint(new LinearGradientPaint(0.0f, 0.0f, 1.0f, 1.0f, new float[]{0.0f, 1.0f}, new Color[]{new Color(getBackground().getRed(), getBackground().getGreen(), getBackground().getBlue(), 180), getBackground()}, CycleMethod.REFLECT));
            //g2.setPaint();
            //g2.fillRect(0,0, getWidth(), getHeight());

            g2.setStroke(new BasicStroke(1));

            g2.setPaint(getForeground());

            double range = valueRange[1] - valueRange[0];
            double scale = this.getWidth() / range;
            double zeroX = -valueRange[0] * scale;
            double valueX = (value - valueRange[0]) * scale;
            Rectangle2D rect = new Rectangle2D.Double(Math.min(zeroX, valueX), (getHeight() * 0.0), Math.max(zeroX, valueX), getHeight() * 1.1);

            g2.fill(rect);
        } else {
            g2.setPaint(g2.getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        //g2.drawImage(img,(int)r.getX(), (int)r.getY(), this);
        Font f = g2.getFont();
        g2.setFont(this.getFont().deriveFont(Font.BOLD));

        FontRenderContext frc = g2.getFontRenderContext();
        String s = getText();
        TextLayout layout = new TextLayout(s, g2.getFont(), frc);

        Rectangle2D strBounds = layout.getBounds();

        float[] strCoord = new float[]{((float) this.getWidth() - (float) strBounds.getWidth()) / 2.0f, ((float) this.getHeight() + (float) strBounds.getHeight()) / 2.0f};

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(fontGlowColor);

        layout.draw(g2, -1f + strCoord[0], 1f + strCoord[1]);
        layout.draw(g2, -1f + strCoord[0], -1f + strCoord[1]);
        layout.draw(g2, 1f + strCoord[0], 1f + strCoord[1]);
        layout.draw(g2, 1f + strCoord[0], -1f + strCoord[1]);

        layout.draw(g2, -1f + strCoord[0], strCoord[1]);
        layout.draw(g2, strCoord[0], -1f + strCoord[1]);
        layout.draw(g2, 1f + strCoord[0], strCoord[1]);
        layout.draw(g2, strCoord[0], 1f + strCoord[1]);

        g2.setColor(fontColor);
        layout.draw(g2, strCoord[0], strCoord[1]);
        layout.draw(g2, strCoord[0], strCoord[1]);

        g2.setFont(f);

        g2.setPaint(Color.GRAY);
        g2.drawLine(0, 0, 0, getHeight());
        g2.drawLine(getWidth(), 0, getWidth(), getHeight());
        //g2.drawRect(0, 0, getWidth(), getHeight());
    }
}
