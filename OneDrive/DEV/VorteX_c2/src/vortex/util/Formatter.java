/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

import java.text.DecimalFormat;
import java.text.ParseException;
import javax.swing.text.NumberFormatter;

/**
 *
 * @author Nikolay
 */
public class Formatter {

    public static String formatNumber(Number num, int digits) {

        String s = "0.";
        for (int i = 0; i < digits; i++) {
            s += "0";
        }
        DecimalFormat df = new DecimalFormat(s);

        NumberFormatter nf = new NumberFormatter(df);

        String out = "Illegal Format";
        try {
            out = nf.valueToString(num);
        } catch (ParseException e) {
        }
        return out;
    }
}
