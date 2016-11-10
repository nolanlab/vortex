/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.util;

/**
 *
 * @author Nikolay
 */
public class ClassWrapper {

    public Class item;

    public ClassWrapper(Class item) {
        this.item = item;
    }

    public Class getItem() {
        return item;
    }

    @Override
    public String toString() {
        if (item.getName().contains(".")) {

            String str = item.getName();
            str = str.substring(str.lastIndexOf(".") + 1);
            return str;

        } else {
            return item.getName();
        }
    }
}