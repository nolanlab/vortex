/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.launch;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 *
 * @author Nikolay Samusik
 */
public class Launch {


    private enum OperatingSystems {
        MAC, WIN, LINUX
    }

    /**
      * @param args the command line arguments
      */
    
    public static void main(String[] args) {
        int heapSizeInGB = 16;
        try {
            //Reading preferences
            Properties prop = new Properties();
            File propFile = new File("config.txt");
            if (propFile.exists()) {
                prop.load(new FileReader(propFile));
            }

            String heapSize = prop.getProperty("max_heap_size", "-1");

            boolean validHeapSize = !heapSize.equals("-1");

            
            if (validHeapSize) {
                try {
                    heapSizeInGB = Integer.parseInt(heapSize);
                } catch (NumberFormatException e) {
                    validHeapSize = false;
                }
            }

            while (!validHeapSize) {
                heapSize = JOptionPane.showInputDialog("Set maximum heap size in GB, valid input is an integer between 4 and 128\nMaximum heap size typically should not exceeed"
                        + "the physical size of RAM on your system. \n your choice will be remembered and you can change the heap size later by manually editing the 'config.txt' file", heapSizeInGB);
                try {
                    heapSizeInGB = Integer.parseInt(heapSize);
                    validHeapSize = heapSizeInGB >= 4 && heapSizeInGB <= 196;
                    if (validHeapSize) {
                        prop.setProperty("max_heap_size", heapSize);
                    }
                } catch (NumberFormatException e) {
                    validHeapSize = false;
                }
            }

            System.out.println(heapSize);

            OutputStream out = new FileOutputStream(propFile);
            prop.store(out,null);//, "This is a VorteX launcher configuration file");
            
            out.close();
            
            OsCheck.OSType os = OsCheck.getOperatingSystemType();
            Process p =null;
            switch(os){
                case Linux:
                case Windows:
                    String cmd = "java -Xmx" + (heapSizeInGB) + "G -cp \"./*\" vortex.gui.frmMain";
                    p = Runtime.getRuntime().exec(cmd);
                    break;
                case MacOS:
                    p = Runtime.getRuntime().exec(new String[]{"/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java", "-Xmx"+ (heapSizeInGB) + "G", "vortex.gui.frmMain"}, new String[]{"CLASSPATH=VorteX.jar"});
                    break;
            }
           
            if(p==null){
                throw new IllegalStateException("Failed to initialize the process.");
            }
            System.exit(0);
            
            
            /*
            do {

                InputStream is = p.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                InputStream is2 = p.getInputStream();
                BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
                String s = null;

                while ((s = br.readLine()) != null) {
                    System.out.println(s);
                }
                while ((s = br2.readLine()) != null) {
                    System.out.println(s);
                }

                Thread.sleep(10);

            } while (p.isAlive());*/

        } catch (Exception e) {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(bs);
            e.printStackTrace(ps);
            String s = bs.toString();
            String[] s2 = s.split("\n");
            String s3 = "";
            for (int i = 0; i < Math.min(s2.length, 10); i++) {
                s3 += s2[i] + "\n";
            }
            s3 += "...";
            JOptionPane.showMessageDialog(null, s3, "Exception", JOptionPane.ERROR_MESSAGE);
            JOptionPane.showMessageDialog(null, "Automatic launch failed. Please try launching manually:\njava -Xmx" + (heapSizeInGB) + "G -cp \"lib/*\" vortex.gui2.frmMain");
        }
        //
    }

}
