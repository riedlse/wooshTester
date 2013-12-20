/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wooshcom.tester;

import java.awt.Color;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//com.whooshcom.tester//init//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "initTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = true)
@ActionID(category = "Window", id = "com.whooshcom.tester.initTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_initAction",
        preferredID = "initTopComponent"
)
@Messages({
    "CTL_initAction=init",
    "CTL_initTopComponent=CSX-1641 Tester",
    "HINT_initTopComponent=This window is for testing the CSX-1641 device"
})
public final class initTopComponent extends TopComponent {

    public class device {
        String serialNumber;
        String date;
        String operator;
        String boot;
        String code;
        String flash;
        String security;
        String Dthing;
        String comment;
    }
    
    public device[] dev = new device[8192];
    
    public static String csvFileToRead = System.getProperty("user.home") + "/CSX1641.csv";
    public static String printText = " ";
    public static String FPGAfilename = " ";
    public File FPGAfileptr;
    public static String maintMAC = "";
    public static String gigeMAC = "";
    public static String smaintMAC = "";
    public static String sgigeMAC = "";
    public static String sserialNumber = "";
    public static String boot = "boot";
    public static String code= "code";
    public static String flash = "flash";
    public static boolean cbsn;
    public static boolean cbglm1;
    public static boolean cbglm2;
    public static boolean cbglgi;
    public static boolean cbglgo;
    public static boolean cborm1;
    public static boolean cborm2;
    public static boolean cborgi;
    public static boolean cborgo;
    public static boolean cbredpwr;
    public static boolean cbredfault;
    public static boolean cbredrestore;
    public static boolean cbgreen;
    public static boolean cbasi1;
    public static boolean cbasi2;
    public static boolean cbasi3;
    public static boolean cbasi4;
    public static boolean FPGAselected = false;
    public static boolean tPresent = false;
    public static boolean ok = false;
    public boolean prReady = false;
    public String errorMessage = "";
    public String oper = "";
    public long errorDispStart = 0L;
    public Date testStart;
    public Date testEnd;
    public boolean printDialog = true;
    public boolean printInteractive = true;
    public static JTextArea ptext;
    private static boolean startProgram = false;
    private static boolean runTest = false;
    private static boolean stopTest = false;
    private final String useFont = "Consolas";
    private final int baseMAC = 0x0A5000;
    private final int getTimeout = 1000;
    private int stage = 0;
    private int serialNumber;
    private int maintMac;
    private int gigeMac;
    private int m3;
    private int m2;
    private int m1;
    private int e3;
    private int e2;
    private int e1;
    private static int lastSerial = 0;

    public initTopComponent() {
               
        initComponents();
        setName(Bundle.CTL_initTopComponent());
        setToolTipText(Bundle.HINT_initTopComponent());
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);

        long oor = System.currentTimeMillis();
        if (true) {
            try {
                System.setErr(new PrintStream(new FileOutputStream(System.getProperty("user.home") + "error_" + oor + ".txt")));
                System.setOut(new PrintStream(new FileOutputStream(System.getProperty("user.home") + "output_" + oor + ".txt")));
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        System.out.println("WooshCom CSX-1641 Test v" + 1 + "." + 0 + "." + 0 + " (c)2013 Bruce Marler");
        System.out.println(System.getProperty("java.version") + " " + System.getProperty("sun.arch.data.model") + " bit");
        System.out.println(System.getProperty("java.vm.name"));
        System.out.printf("Current System Time=0x%08x dec=%d\n", oor, oor);
  
        OK.setVisible(false);
        programFPGA.setEnabled(false);
        // create a blind text area for the print function to use.
        ptext = new javax.swing.JTextArea();
        ptext.setLineWrap(true);
        ptext.setFont(new java.awt.Font(useFont, Font.PLAIN, 10));
        ptext.setColumns(130);
        ptext.setRows(100);
        
        readCsv();
        int minSerNum = 1001;
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(lastSerial+1, minSerNum, minSerNum + 0x0fff, 1);
        serNum.setModel(spinnerNumberModel);
        stage = 0;
        Thread t = new Thread(new ExecTest());
        t.start();  
    }
    
    public void readCsv() {
        int sn = 0;
        BufferedReader br = null;
        String line = "";
        String splitBy = ",";
        try {

            br = new BufferedReader(new FileReader(csvFileToRead));
            while ((line = br.readLine()) != null) {
                String[] csx = line.split(splitBy);
                int num = csx.length;
                if (num > 0) {
                   sn = Integer.parseInt(csx[0]);
                   if (sn > lastSerial) {
                       lastSerial = sn;
                   }
                   dev[sn] = new device();
                } else {
                    sn = 0;
                }
                System.out.println("got " + num);
                String prcsx = "";
                switch (num) {
                    case 11:
                    case 10:
                    case 9:
                        prcsx += " , comments=" + csx[8];
                        dev[sn].comment = csx[8];
                    case 8:
                        prcsx = " , something=" + csx[7] + prcsx;
                        dev[sn].Dthing = csx[7];
                    case 7:
                        prcsx = " , Security=" + csx[6] + prcsx;
                        dev[sn].security = csx[6];
                    case 6:
                        prcsx = " , flash=" + csx[5] + prcsx;
                        dev[sn].flash = csx[5];
                    case 5:
                        prcsx = " , code=" + csx[4] + prcsx;
                        dev[sn].code = csx[4];
                    case 4:
                        prcsx = " , boot=" + csx[3] + prcsx;
                        dev[sn].boot = csx[3];
                    case 3:
                        prcsx = " , operator=" + csx[2] + prcsx;
                        dev[sn].operator = csx[2];
                    case 2:
                        prcsx = " , date=" + csx[1] + prcsx;
                        dev[sn].date = csx[1];
                    case 1:
                        prcsx = "CSX1641 [Serial= " + csx[0] + prcsx;
                        break;
                    case 0:
                }
                System.out.println(prcsx + "]");
            }
        } catch (FileNotFoundException ex) {
            //Exceptions.printStackTrace(ex);
            System.out.println("File does not exist, it will be created - " + csvFileToRead);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }

            }
        }
    }
  
    public void appendCsv() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( csvFileToRead , true ) );
            int sn = serialNumber;
            dev[sn] = new device();
            String timeStamp = new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
            writer.write(sn + "," + timeStamp + "," + oper + "," + boot + "," + code + "," + flash + ","
            + "security" + "," + "Dsomething" + "," + "comments" + "\n");
            writer.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
    
    private void message(boolean error, String msg) {
        int type = (error ? JOptionPane.ERROR_MESSAGE
                : JOptionPane.INFORMATION_MESSAGE);
        //JOptionPane.showMessageDialog(this.getComponent(), msg, "Printing", type);
    }

    public void print() {
        String startStamp = new SimpleDateFormat("MM/dd/yyyy HH:MM:SS").format(testStart);
        String endStamp = new SimpleDateFormat("MM/dd/yyyy HH:MM:SS").format(testEnd);
        printText = lserNum.getText() + "\n";
        printText += lmMAC.getText() + "\n";
        printText += lgMAC.getText() + "\n";
             
        long duration = testEnd.getTime() - testStart.getTime();
        if (duration > 0) {
            long milli = duration % 1000;
            duration /= 1000;
            long secs = duration % 60;
            duration /= 60;
            long min =  duration % 60;
            duration /= 60;
            long hours = duration % 24;
            printText += "Test Started " + startStamp + "\n";
            printText += "Test Ended " + endStamp + "\n";
            printText += "Test Duration " + hours + ":" + min + ":" + secs + "." + milli + "\n";
        }
        
        if (cbsn) {
            printText += "\nOK  - Serial Number match\n";
        } else {
            printText += "\nERR - Serial Number match\n";           
        }
        
        printText += "\n\nLEDs Illuminate\n";
        
        if (cborm1) {
            printText += "OK  - Management Port 1 Orange/Right LED\n";
        } else {
            printText += "ERR - Management Port 1 Orange/Right LED\n";           
        }
        
        if (cbglm1) {
            printText += "OK  - Management Port 1 Green/Left LED\n";
        } else {
            printText += "ERR - Management Port 1 Green/Left LED\n";           
        }

        if (cborm2) {
            printText += "OK  - Management Port 2 Orange/Right LED\n";
        } else {
            printText += "ERR - Management Port 2 Orange/Right LED\n";           
        }
        
        if (cbglm2) {
            printText += "OK  - Management Port 2 Green/Left LED\n";
        } else {
            printText += "ERR - Management Port 2 Green/Left LED\n";           
        }
        
        if (cborgi) {
            printText += "OK  - GigE Input Orange/Right LED\n";
        } else {
            printText += "ERR - GigE Input Orange/Right LED\n";           
        }
        
        if (cbglgi) {
            printText += "OK  - GigE Input Green/Left LED\n";
        } else {
            printText += "ERR - GigE Input Green/Left LED\n";           
        }
        
        if (cborgo) {
            printText += "OK  - GigE Output Orange/Right LED\n";
        } else {
            printText += "ERR - GigE Output Orange/Right LED\n";           
        }
        
        if (cbglgo) {
            printText += "OK  - GigE Output Green/Left LED\n";
        } else {
            printText += "ERR - GigE Output Green/Left LED\n";           
        }
        
        printText += "ASI Sync LEDs Green 1-";
        if (cbasi1) {
            printText += "OK  2-";
        } else {
            printText += "ERR 2-";
        }
        if (cbasi2) {
            printText += "OK  3-";
        } else {
            printText += "ERR 3-";
        }
        if (cbasi3) {
            printText += "OK  4-";
        } else {
            printText += "ERR 4-";
        }
        if (cbasi4) {
            printText += "OK\n";
        } else {
            printText += "ERR\n";
        }
        
        if (cbredpwr) {
            printText += "OK  - PWR/FAULT RED\n";
        } else {
            printText += "ERR - PWR/FAULT RED\n";           
        }
        
        if (cbredfault) {
            printText += "OK  - ALARM RED\n";
        } else {
            printText += "ERR - ALARM RED\n";           
        }

        if (cbredrestore) {
            printText += "OK  - RESTORE RED\n";
        } else {
            printText += "ERR - RESTORE RED\n";           
        }

        if (cbgreen) {
            printText += "OK  - PWR/FAULT GREEN\n";
        } else {
            printText += "ERR - PWR/FAULT GREEN\n";           
        }
        
        MessageFormat header = new MessageFormat(" Wooshcom Production Test Follower");
        MessageFormat footer = new MessageFormat("Page - {0}");
        ptext.setText(printText);
        PrintingTask task = new PrintingTask(header, footer);
        task.execute();

        cbsn = false;
        SNmatch.setSelected(false);
        cborm1 = false;
        ORM1.setSelected(false);
        cborm2 = false;
        ORM2.setSelected(false);
        cbglm1 = false;
        GLM1.setSelected(false);
        cbglm2 = false;
        GLM2.setSelected(false);
        cborgi = false;
        ORGI.setSelected(false);
        cborgo = false;
        ORGO.setSelected(false);
        cbglgi = false;
        GLGI.setSelected(false);
        cbglgo = false;
        GLGO.setSelected(false);
        cbredpwr = false;
        RedPwr.setSelected(false);
        cbredfault = false;
        redAlarm.setSelected(false);
        cbredrestore = false;
        redRestore.setSelected(false);
        cbgreen = false;
        greenPwr.setSelected(false);
        cbasi1 = false;
        ASI1.setSelected(false);
        cbasi2 = false;
        ASI2.setSelected(false);
        cbasi3 = false;
        ASI3.setSelected(false);
        cbasi4 = false;
        ASI4.setSelected(false);

    }

    private class PrintingTask extends SwingWorker<Object, Object> {

        private final MessageFormat headerFormat;
        private final MessageFormat footerFormat;
        private volatile boolean complete = false;
        private volatile String message;

        public PrintingTask(MessageFormat header, MessageFormat footer) {
            this.headerFormat = header;
            this.footerFormat = footer;
        }

        @Override
        protected Object doInBackground() {
            try {
                PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
               
                aset.add(OrientationRequested.PORTRAIT);
                complete = ptext.print(headerFormat, footerFormat, printDialog, null, aset, printInteractive);
                message = "Printing " + (complete ? "complete" : "canceled");
            } catch (PrinterException ex) {
                message = "A printer error occurred";
            } catch (SecurityException ex) {
                message = "Cannot access the printer due to security reasons";
            }
            return null;
        }

        @Override
        protected void done() {
            errorMessage = message;
            errorDispStart = System.currentTimeMillis();
            message(!complete, message);
        }
    }

    private void error(String msg) {
        errorMessage = msg;
        errorDispStart = System.currentTimeMillis();
//        message(true, msg);
    }
    
    private boolean connect123() {
        System.out.println("Trying to connect to device at http://192.168.34.123");
        try {
            // try to connect to device
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123").timeout(getTimeout).get();
            deviceStatus.setText("Device found at 192.168.34.123 - Ready to program");
            Elements inputElements = doc.getElementsByTag("input");
            for (Element inputElement : inputElements) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
                System.out.println("name=" + key + " value=" + value);
                if (key.equals("M2")) {
                    lserNum.setText("Serial Number set=" + serialNumber + " Read=" + value);
                }
                if (key.equals("M0")) {
                    maintMAC = value;
                    lmMAC.setText(String.format("Maintenance MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, m3, m2, m1));
                }
                if (key.equals("M1")) {
                    gigeMAC = value;
                    lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, e3, e2, e1));
                }
            }
        } catch (IOException ex) {
            //deviceStatus.setText("No Device at http://192.168.34.123");
            System.out.println(" No device at http://192.168.34.123");
            //Exceptions.printStackTrace(ex);
            return (false);
        }
        return (true);
    }

    private boolean deleteFPGA() {
        System.out.println("Deleting FPGA code");
        // delete FPGA code
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                    .data("d", " DeleteFPGA code")
                    .timeout(getTimeout)
                    .get();
        } catch (IOException ex) {
            errorStatus.setText("Error deleting FPGA code");
            //Exceptions.printStackTrace(ex);
            return (false);
        }

        // Will have to loop here until it's done
        boolean erasing = true;
        while (erasing) {
            // delete FPGA code
            try {
                Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123")
                        .timeout(getTimeout)
                        .get();
                System.out.println("Erasing");
                Elements eraseElements = doc.getElementsByTag("input");
                for (Element eraseElement : eraseElements) {
                    String key = eraseElement.attr("name");
                    String value = eraseElement.attr("value");
                    //System.out.println("name=" + key + " value=" + value);
                    if (value.equals("Update-FPGA")) {
                        erasing = false;
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error erasing FPGA");
                //Exceptions.printStackTrace(ex);
                return (false);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex6) {
                //Exceptions.printStackTrace(ex);
            }

        }
        return (true);
    }

    private boolean deleteFPGA50() {
        System.out.println("Deleting FPGA code");
        // delete FPGA code
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.50/webpage.html")
                    .data("d", " DeleteFPGA code")
                    .timeout(getTimeout)
                    .get();
        } catch (IOException ex) {
            errorStatus.setText("Error deleting FPGA code");
            //Exceptions.printStackTrace(ex);
            return (false);
        }

        // Will have to loop here until it's done
        boolean erasing = true;
        while (erasing) {
            // delete FPGA code
            try {
                Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.50")
                        .timeout(getTimeout)
                        .get();
                System.out.println("Erasing");
                Elements eraseElements = doc.getElementsByTag("input");
                for (Element eraseElement : eraseElements) {
                    String key = eraseElement.attr("name");
                    String value = eraseElement.attr("value");
                    //System.out.println("name=" + key + " value=" + value);
                    if (value.equals("Update-FPGA")) {
                        erasing = false;
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error erasing FPGA");
                //Exceptions.printStackTrace(ex);
                return (false);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex6) {
                //Exceptions.printStackTrace(ex);
            }

        }
        return (true);
    }

    private boolean saveGige() {
        boolean rc = true;
        // Save gigE MAC
        System.out.println("Saving GigE Mac=" + sgigeMAC);
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                    .data("r", "Save-MAC-(GIG-E)")
                    .data("M1", sgigeMAC)
                    .timeout(getTimeout)
                    .get();
            Elements inputgElements = doc.getElementsByTag("input");
            for (Element inputgElement : inputgElements) {
                String key = inputgElement.attr("name");
                String value = inputgElement.attr("value");
                //System.out.println("name=" + key + " value=" + value);
                if (key.equals("M2")) {
                    lserNum.setText("Serial Number set=" + serialNumber + " Read=" + value);
                }
                if (key.equals("M0")) {
                    lmMAC.setText(String.format("Maintenance MAC set=40-d8-55-%02x-%02x-%02x Read=" + value, m3, m2, m1));
                }
                if (key.equals("M1")) {
                    if (sgigeMAC.equals(value)) {
                        lgMAC.setForeground(Color.green);
                    } else {
                        lgMAC.setForeground(Color.red);
                        rc = false;
                    }
                    lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, e3, e2, e1));
                }
            }
        } catch (IOException ex) {
            errorStatus.setText("Error Setting Gig E MAC");
            //Exceptions.printStackTrace(ex);
            rc = false;
        }
        return (rc);
    }

    private boolean connectTester() {
        System.out.println("Trying to connect to tester at http://192.168.34.121/Tester.htm");
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm").timeout(getTimeout).get();
            //deviceStatus.setText("Tester found at 192.168.34.121");
            Elements inputElements = doc.getElementsByTag("input");
            for (Element inputElement : inputElements) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
                System.out.println("name=" + key + " value=" + value);
                if (key.equals("gmac")) {
                    testerPresent.setForeground(Color.green);
                    testerPresent.setText("Tester Present: GigE MAC=" + value);
                }
            }
        } catch (IOException ex) {
            //Exceptions.printStackTrace(ex);
            testerPresent.setText("ERROR: No Tester Present");
            return (false);
        }
        return (true);
    }

    private boolean checkFail() {
        System.out.println("Trying to connect to tester at http://192.168.34.121/XML_state.htm");
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/XML_state.htm").timeout(getTimeout).get();
            //Element link = doc.select("state").first();
            if (!stopTest) {
                String state = doc.body().text();
                System.out.println("State=" + state);
                if (state.equalsIgnoreCase("failed")) {
                    errorStatus.setText("ERROR: Device Failed Test");
                    //testerPresent.setForeground(Color.red);
                    //testerPresent.setText("Tester Present: FAILED");
                    return (false);
                } else {
                    testerPresent.setForeground(Color.green);
                    testerPresent.setText("Tester Present: TESTING");
                }
            }
        } catch (IOException ex) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: No Tester Present");
            return (false);
        }
        return (true);
    }
    
    private boolean programFPGA() {
        boolean rc = true;
        System.out.println("Programming FPGA with " + FPGAfileptr.getName());
        //program FPGA
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost("http://ADMIN:admin@192.168.34.123/eS.bin");

            FileBody bin = new FileBody(FPGAfileptr, ContentType.APPLICATION_OCTET_STREAM, FPGAfileptr.getName());
            //StringBody comment = new StringBody("A binary file of some kind", ContentType.TEXT_PLAIN);

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("datafile", bin)
                    .build();

            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } catch (IOException ex) {
            //Exceptions.printStackTrace(ex);
            rc = false;
        } finally {
            try {
                httpclient.close();
            } catch (IOException ex) {
                //Exceptions.printStackTrace(ex);
                rc = false;
            }
        }
        return(rc);
    }

    private boolean programFPGA50() {
        boolean rc = true;
        System.out.println("Programming FPGA with " + FPGAfileptr.getName());
        //program FPGA
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost("http://ADMIN:admin@192.168.34.50/eS.bin");

            FileBody bin = new FileBody(FPGAfileptr, ContentType.APPLICATION_OCTET_STREAM, FPGAfileptr.getName());
            //StringBody comment = new StringBody("A binary file of some kind", ContentType.TEXT_PLAIN);

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("datafile", bin)
                    .build();

            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } catch (IOException ex) {
            //Exceptions.printStackTrace(ex);
            rc = false;
        } finally {
            try {
                httpclient.close();
            } catch (IOException ex) {
                //Exceptions.printStackTrace(ex);
                rc = false;
            }
        }
        return(rc);
    }

    private boolean saveMaint() {
        boolean rc = true;
        System.out.println("Saving Maintenance MAC=" + smaintMAC);
        // Save Maintenance MAC
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                    .data("r", "Save-MAC-(maintenance)")
                    .data("M0", smaintMAC)
                    .timeout(getTimeout)
                    .get();
            Elements inputgElements = doc.getElementsByTag("input");
            for (Element inputgElement : inputgElements) {
                String key = inputgElement.attr("name");
                String value = inputgElement.attr("value");
                //System.out.println("name=" + key + " value=" + value);
                if (key.equals("M2")) {
                    lserNum.setText("Serial Number set=" + serialNumber + " Read=" + value);
                }
                if (key.equals("M0")) {
                    if (smaintMAC.equals(value)) {
                        lmMAC.setForeground(Color.green);
                    } else {
                        lmMAC.setForeground(Color.red);
                        rc = false;
                    }
                    lmMAC.setText(String.format("Maintenance MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, m3, m2, m1));
                }
                if (key.equals("M1")) {
                    lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, e3, e2, e1));
                }
            }
        } catch (IOException ex) {
            System.out.println("Error Setting maintenance MAC");
            //Exceptions.printStackTrace(ex);
            rc = false;
        }
        return(rc);
    }
    
    private boolean saveSerial() {
        boolean rc = true;
        System.out.println("Saving Serial Number=" + sserialNumber);

        // Save Serial Number
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                    .data("r", "Save-S/N")
                    .data("M2", sserialNumber)
                    .timeout(getTimeout)
                    .get();
            Elements inputgElements = doc.getElementsByTag("input");
            for (Element inputgElement : inputgElements) {
                String key = inputgElement.attr("name");
                String value = inputgElement.attr("value");
                //System.out.println("name=" + key + " value=" + value);
                if (key.equals("M2")) {
                    if (sserialNumber.equals(value)) {
                        lserNum.setForeground(Color.green);
                    } else {
                        lserNum.setForeground(Color.red);
                        rc = false;
                    }
                    lserNum.setText("Serial Number set=" + serialNumber + " Read=" + value);
                }
                if (key.equals("M0")) {
                    lmMAC.setText(String.format("Maintenance MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, m3, m2, m1));
                }
                if (key.equals("M1")) {
                    lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, e3, e2, e1));
                }
            }
        } catch (IOException ex) {
            errorStatus.setText("Error Setting Serial Number");
            //Exceptions.printStackTrace(ex);
            rc = false;
        }
        return(rc);
    }

    private boolean startTester() {
        System.out.println("Trying to start tester");
        // Start Tester
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .data("eT", "Turn ON")
                    .timeout(getTimeout)
                    .get();
        } catch (IOException ex10) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: Starting Tester");
            return(false);
        }
        return(true);
    }
   
    private boolean setTesterMAC() {
        System.out.println("Trying to set tester MAC");
        // Set mac on tester
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .data("gmac", gigeMAC)
                    .timeout(getTimeout)
                    .get();
        } catch (IOException ex10) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: setting mac on tester");
            return(false);
        }

        // Modify Settings
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .data("A0g", "Modify Settings - 2")
                    .timeout(getTimeout)
                    .get();
        } catch (IOException ex10) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: tester modify settings");
            return(false);
        }

        // save and apply changes
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .data("esf", "Save and apply changes")
                    .timeout(getTimeout)
                    .get();
        } catch (IOException ex10) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: tester save and apply changes");
            return(false);
        }

        // Verify mac set correctly
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .timeout(getTimeout)
                    .get();
            //deviceStatus.setText("Tester found at 192.168.34.121");
            Elements input3Elements = doc.getElementsByTag("input");
            for (Element input3Element : input3Elements) {
                String key = input3Element.attr("name");
                String value = input3Element.attr("value");
                System.out.println("name=" + key + " value=" + value);
                if (key.equals("gmac")) {
                    if (gigeMAC.equals(value)) {
                        testerPresent.setForeground(Color.green);
                    } else {
                        testerPresent.setForeground(Color.red);
                    }
                    testerPresent.setText("Tester Present: GigE MAC=" + value + " set=" + gigeMAC);
                }
            }
        } catch (IOException ex11) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: tester checking mac address");
            return(false);
        }
        return(true);
    }

    private void setupScreen() {
        // Calculate mac addresses
        serialNumber = (Integer) serNum.getValue();
        maintMac = baseMAC + ((serialNumber - 1001) * 2);
        gigeMac = baseMAC + ((serialNumber - 1001) * 2) + 1;
        m3 = (maintMac >> 16) & 0x00ff;
        m2 = (maintMac >> 8) & 0x00ff;
        m1 = maintMac & 0x00ff;
        e3 = (gigeMac >> 16) & 0x00ff;
        e2 = (gigeMac >> 8) & 0x00ff;
        e1 = gigeMac & 0x00ff;
        lgMAC.setForeground(Color.black);
        lmMAC.setForeground(Color.black);
        lserNum.setForeground(Color.black);
        smaintMAC = String.format("40-d8-55-%02x-%02x-%02x", m3, m2, m1);
        sgigeMAC = String.format("40-d8-55-%02x-%02x-%02x", e3, e2, e1);
        sserialNumber = String.format("%05d", serialNumber);
        lmMAC.setText("Maintenance MAC set=" + smaintMAC + " Read=xx-xx-xx-xx-xx-xx");
        lgMAC.setText("GigE MAC set=" + sgigeMAC + " Read=xx-xx-xx-xx-xx-xx");
        lserNum.setText("Serial Number set=" + sserialNumber + " Read=xxxxx");
        errorStatus.setText("Error:");
    }

    private void delay(long seconds) {
        //System.out.println("Sleeping " + seconds + " seconds");
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex6) {
            //Exceptions.printStackTrace(ex);
        }
    }

    private void resetDevice(int lastOctet) {
        System.out.println("Reset 192.168.34." + lastOctet);
        // try to reset device
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34." + lastOctet + "/0")
                    .data("b", "")
                    .timeout(getTimeout)
                    .get();
        } catch (IOException ex1) {
            errorStatus.setText("Error resetting device at http://ADMIN:admin@192.168.34.123");
            //Exceptions.printStackTrace(ex);
        }
    }

    private boolean connect124() {
        // try to read back device status
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.124").timeout(getTimeout).get();
            deviceStatus.setText("Device found at 192.168.34.124 - Already Programmed RESETING to 192.168.34.50");
            Elements inputElements = doc.getElementsByTag("input");
            for (Element inputElement : inputElements) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
                System.out.println("name=" + key + " value=" + value);
                if (key.equals("M2")) {
                    lserNum.setText("Serial Number set=" + serialNumber + " Read=" + value);
                }
                if (key.equals("M0")) {
                    lmMAC.setText(String.format("Maintenance MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, m3, m2, m1));
                }
                if (key.equals("M1")) {
                    lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, e3, e2, e1));
                }
            }
        } catch (IOException ex2) {
            System.out.println("Error reading 124 back box status");
            //Exceptions.printStackTrace(ex);
            return (false);
        }
        return (true);
    }

    private boolean connect50() {
        try {
            Document doc = Jsoup.connect("http://192.168.34.50/device.htm").timeout(getTimeout).get();
            deviceStatus.setText("Device found at 192.168.34.50 - Ready to test / Device");
            //String Serial = Jsoup.parse(doc).select("tr:matchesOwn(Serial Number)").first().nextSibling().toString());
            Elements inputElements = doc.getElementsByTag("tr");
            for (Element inputElement : inputElements) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
                String text = inputElement.attr("text");
                System.out.println("name=" + key + " value=" + value + " text=" + text);

            }
            Document doc1 = Jsoup.connect("http://192.168.34.50/mNetwork.htm").timeout(getTimeout).get();
            deviceStatus.setText("Device found at 192.168.34.50 - Ready to test / Maint");
            Elements inputElements1 = doc1.getElementsByTag("input");
            for (Element inputElement : inputElements1) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
                System.out.println("name=" + key + " value=" + value);
                if (value.contains("40-d8-55")) {
                    lmMAC.setText(String.format("Maintenance MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, m3, m2, m1));
                    System.out.println("Maint MAC=" + value);
                }
            }
            Document doc2 = Jsoup.connect("http://192.168.34.50/tStream.htm").timeout(getTimeout).get();
            deviceStatus.setText("Device found at 192.168.34.50 - Ready to test / Maint");
            Elements inputElements2 = doc2.getElementsByTag("input");
            for (Element inputElement : inputElements2) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
                System.out.println("name=" + key + " value=" + value);
                if (value.contains("40-d8-55")) {
                    lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, e3, e2, e1));
                    System.out.println("GigE MAC=" + value);
                    gigeMAC = value;
                }

            }
        } catch (IOException ex3) {
            //errorStatus.setText("Error reading back box status ip 192.168.34.50");
            //Exceptions.printStackTrace(ex);
            return(false);
        }
        return(true);
    }

    private boolean testerOff() {
        // VInsure tester is not running
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .timeout(getTimeout)
                    .get();
            //deviceStatus.setText("Tester found at 192.168.34.121");
            Elements input3Elements = doc.getElementsByTag("input");
            for (Element input3Element : input3Elements) {
                String key = input3Element.attr("name");
                String value = input3Element.attr("value");
                System.out.println("name=" + key + " value=" + value);
                if (key.equals("eT")) {
                    if (value.equals("Turn OFF")) {
                        // Stop Tester
                        try {
                            Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                                    .data("eT", "Turn OFF")
                                    .timeout(3000)
                                    .get();
                        } catch (IOException ex10) {
                            //Exceptions.printStackTrace(ex);
                            errorStatus.setText("ERROR: Turning Tester OFF");
                            return(false);
                        }

                    }
                }
            }
        } catch (IOException ex11) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: Getting Tester run status");
            return(false);
        }
        return(true);
    }

    private class ExecTest implements Runnable {

        @Override
        public void run() {
            setupScreen();
            runTest = false;
            cbsn = false;
            SNmatch.setSelected(false);
            SNmatch.setEnabled(false);
            ORM1.setEnabled(false);
            ORM2.setEnabled(false);
            GLM1.setEnabled(false);
            GLM2.setEnabled(false);
            FPGALED.setVisible(false);
            ASILED.setVisible(false);
            ORGI.setEnabled(false);
            ORGO.setEnabled(false);
            GLGI.setEnabled(false);
            GLGO.setEnabled(false);
            printit.setEnabled(false);
            boolean setupDone = false;
            int loops = 0;
            instructions.setText("Instructions: Please enter Operator initials");
            while (!setupDone) {
                oper = operator.getText();
                if (oper != null) {
                    if (oper.equals("abc")) {
                        //instructions.setText("Instructions: Please enter Operator initials");
                    } else {
                        lOper.setText("Operator=" + oper);
                        lOper.setForeground(Color.green);
                        loops++;
                    }
                }
                delay(1);
                if (loops > 4) {
                    setupDone = true;
                }
            }
            operator.setEnabled(false);
            tPresent = connectTester();
            if (tPresent) {
                testerOff();
            }

            while (true) {
                SNmatch.setEnabled(true);
                instructions.setText("Instructions: Searching for Device");
                while (connect123()) {
                    stage = 1;
                    while (!FPGAselected) {
                        instructions.setText("Instructions: Please select FPGA file");
                        delay(1);
                    }
                    errorStatus.setText("Error:");
                    errorStatus.setForeground(Color.green);
                    initialize.setEnabled(true);
                    startTest.setEnabled(false);
                    ORM1.setEnabled(true);
                    ORM2.setEnabled(true);
                    GLM1.setEnabled(true);
                    GLM2.setEnabled(true);
                    printit.setEnabled(true);
/*                    cbglm1 = false;
                    GLM1.setSelected(false);
                    cbglm2 = false;
                    GLM2.setSelected(false);
                    cborm1 = false;
                    ORM1.setSelected(false);
                    cborm2 = false;
                    ORM2.setSelected(false);
                    */
                    instructions.setText("Instructions: Check serial numbers, Manamgement port LEDs and click Initialize");
                    if (startProgram) {
                        stage = 2;
                        startProgram = false;
                        cborgi = false;
                        ORGI.setSelected(false);
                        cborgo = false;
                        ORGO.setSelected(false);
                        cbglgi = false;
                        GLGI.setSelected(false);
                        cbglgo = false;
                        GLGO.setSelected(false);
                        cbredpwr = false;
                        RedPwr.setSelected(false);
                        cbredfault = false;
                        redAlarm.setSelected(false);
                        cbredrestore = false;
                        redRestore.setSelected(false);
                        cbgreen = false;
                        greenPwr.setSelected(false);
                        cbasi1 = false;
                        ASI1.setSelected(false);
                        cbasi2 = false;
                        ASI2.setSelected(false);
                        cbasi3 = false;
                        ASI3.setSelected(false);
                        cbasi4 = false;
                        ASI4.setSelected(false);

                        if (saveGige()) {
                            stage = 3;
                            FPGALED.setVisible(true);
                            instructions.setText("Instructions: Check LED sequence");
                            if (deleteFPGA()) {
                                stage = 4;
                                ok = false;
                                OK.setVisible(true);
                                while (!ok) {
                                    delay(1);
                                }
                                if (programFPGA()) {
                                    stage = 5;
                                    if (saveMaint()) {
                                        stage = 6;
                                        if (!saveSerial()) {
                                            errorStatus.setText("Error: Failed to program Serial Number");
                                            errorStatus.setForeground(Color.red);
                                        } else {
                                            stage = 7;
                                        }
                                        resetDevice(123);
                                    } else {
                                        errorStatus.setText("Error: Failed to program Maintenance MAC");
                                        errorStatus.setForeground(Color.red);
                                    }
                                } else {
                                    errorStatus.setText("Error: Failed to program FPGA");
                                    errorStatus.setForeground(Color.red);
                                }
                            } else {
                                errorStatus.setText("Error: Failed to erase FPGA");
                                errorStatus.setForeground(Color.red);
                            }
                        } else {
                            errorStatus.setText("Error: Failed to program GigE MAC");
                            errorStatus.setForeground(Color.red);
                        }
                    }
                    delay(1);
                }
                initialize.setEnabled(false);
                while (connect124()) {
                    stage = 8;
                    resetDevice(124);
                    delay(1);
                }
                while (connect50()) {
                    // TODO need to get code revisions off this screen, somehow....
                    while (!tPresent) {
                        tPresent = connectTester();
                        instructions.setText("Instructions: Please connect Tester");
                        delay(1);
                    } 
                    testerOff();
                    errorStatus.setText("Status:");
                    printit.setEnabled(true);
                    errorStatus.setForeground(Color.green);
                    instructions.setText("Instructions: Connect tester to unit, move management port and start test");
                    startTest.setEnabled(true);
                    if (runTest) {
                        errorStatus.setText("Status: TESTING");
                        ASILED.setVisible(true);
                        ORM1.setEnabled(true);
                        ORM2.setEnabled(true);
                        GLM1.setEnabled(true);
                        GLM2.setEnabled(true);
                        ORGI.setEnabled(true);
                        ORGO.setEnabled(true);
                        GLGI.setEnabled(true);
                        GLGO.setEnabled(true);
                        runTest = false;
                        stopTest = false;
                        testerOff();
                        setTesterMAC();
                        startTester();
                        testStart = Calendar.getInstance().getTime();
                        instructions.setText("Instructions: Verify ASI and GigE LEDs");
                        boolean failed = false;
                        int fails = 0;
                        delay(1);
                        while (!failed && !stopTest) {
                            failed = !checkFail();
                            if (failed) {
                                fails++;
                            }
                            if (fails == 1) {
                                testerOff();
                                errorStatus.setText("Status: Restart");
                                delay(1);
                                startTester();
                                delay(1);
                                failed = false;
                            }
                            delay(1);
                        }
                        stopTest = false;
                        testerOff();
                        testEnd = Calendar.getInstance().getTime();
                        if (failed) {
                            instructions.setText("Instructions: Test Failed");
                            errorStatus.setText("Error: Test Failed");
                            errorStatus.setForeground(Color.red);
                                ok = false;
                                OK.setVisible(true);
                                while (!ok) {
                                    delay(1);
                                }
                        } else {
                            if (cbsn && cbglm1 && cbglm2 && cbglgi && cbglgo
                                    && cborm1 && cborm2 && cborgi && cborgo
                                    && cbredpwr && cbredfault && cbredrestore && cbgreen
                                    && cbasi1 && cbasi2 && cbasi3 && cbasi4) {
                                appendCsv();
                                print();
                                cbsn = false;
                                SNmatch.setSelected(false);
                                FPGALED.setVisible(false);
                                ASILED.setVisible(false);
                                ORGI.setEnabled(false);
                                ORGO.setEnabled(false);
                                GLGI.setEnabled(false);
                                GLGO.setEnabled(false);
                            } else {
                                instructions.setText("Instructions: Check all LEDs and Print results");
                                ok = false;
                                OK.setVisible(true);
                                while (!ok) {
                                    delay(1);
                                }
                            }
                        }
                    } else {
                        instructions.setText("Instructions: Hook device up to tester and press Test");
                        startTest.setText("Start Test");
                    }
                    delay(1);
                }
                startTest.setEnabled(false);
                delay(1);
                stage = 0;
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        FPGAfileChooser = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        instructions = new javax.swing.JLabel();
        errorStatus = new javax.swing.JLabel();
        OK = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        lserNum = new javax.swing.JLabel();
        deviceStatus = new javax.swing.JLabel();
        lgMAC = new javax.swing.JLabel();
        testerPresent = new javax.swing.JLabel();
        lmMAC = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        programFPGA = new javax.swing.JButton();
        lOper = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        FPGAfile = new javax.swing.JLabel();
        SelectFPGA = new javax.swing.JButton();
        initialize = new javax.swing.JButton();
        startTest = new javax.swing.JButton();
        printit = new javax.swing.JButton();
        operator = new javax.swing.JTextField();
        serNum = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        SNmatch = new javax.swing.JCheckBox();
        net = new javax.swing.JPanel();
        ORM2 = new javax.swing.JCheckBox();
        ORM1 = new javax.swing.JCheckBox();
        ORGO = new javax.swing.JCheckBox();
        ORGI = new javax.swing.JCheckBox();
        GLM1 = new javax.swing.JCheckBox();
        GLM2 = new javax.swing.JCheckBox();
        GLGI = new javax.swing.JCheckBox();
        GLGO = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        FPGALED = new javax.swing.JPanel();
        RedPwr = new javax.swing.JCheckBox();
        redRestore = new javax.swing.JCheckBox();
        greenPwr = new javax.swing.JCheckBox();
        redAlarm = new javax.swing.JCheckBox();
        ASILED = new javax.swing.JPanel();
        ASI1 = new javax.swing.JCheckBox();
        ASI3 = new javax.swing.JCheckBox();
        ASI4 = new javax.swing.JCheckBox();
        ASI2 = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jPanel2.border.title"))); // NOI18N

        instructions.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(instructions, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.instructions.text")); // NOI18N

        errorStatus.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(errorStatus, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.errorStatus.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(OK, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.OK.text")); // NOI18N
        OK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(instructions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(OK))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(errorStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(instructions)
                    .addComponent(OK))
                .addGap(18, 18, 18)
                .addComponent(errorStatus)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jPanel3.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lserNum, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lserNum.text")); // NOI18N

        deviceStatus.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(deviceStatus, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.deviceStatus.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lgMAC, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lgMAC.text")); // NOI18N

        testerPresent.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        testerPresent.setForeground(new java.awt.Color(255, 0, 0));
        org.openide.awt.Mnemonics.setLocalizedText(testerPresent, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.testerPresent.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lmMAC, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lmMAC.text")); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lgMAC)
                    .addComponent(lmMAC)
                    .addComponent(lserNum)
                    .addComponent(deviceStatus)
                    .addComponent(testerPresent))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deviceStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lserNum)
                .addGap(8, 8, 8)
                .addComponent(lmMAC)
                .addGap(7, 7, 7)
                .addComponent(lgMAC)
                .addGap(18, 18, 18)
                .addComponent(testerPresent)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jPanel4.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(programFPGA, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.programFPGA.text")); // NOI18N
        programFPGA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                programFPGAActionPerformed(evt);
            }
        });

        lOper.setForeground(new java.awt.Color(255, 0, 0));
        org.openide.awt.Mnemonics.setLocalizedText(lOper, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lOper.text")); // NOI18N

        jLabel1.setLabelFor(serNum);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(FPGAfile, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.FPGAfile.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(SelectFPGA, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.SelectFPGA.text")); // NOI18N
        SelectFPGA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectFPGAActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(initialize, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.initialize.text_1")); // NOI18N
        initialize.setEnabled(false);
        initialize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                initializeActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(startTest, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.startTest.text")); // NOI18N
        startTest.setEnabled(false);
        startTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startTestActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(printit, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.printit.text")); // NOI18N
        printit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printitActionPerformed(evt);
            }
        });

        operator.setText(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.operator.text")); // NOI18N
        operator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                operatorActionPerformed(evt);
            }
        });

        serNum.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                serNumStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel2.text")); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(operator, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lOper, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(printit))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(serNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(initialize)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(startTest)))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(SelectFPGA)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(programFPGA)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(FPGAfile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(operator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lOper)
                    .addComponent(printit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(initialize)
                    .addComponent(startTest))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(serNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(programFPGA)
                    .addComponent(SelectFPGA))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(FPGAfile)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jPanel5.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(SNmatch, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.SNmatch.text")); // NOI18N
        SNmatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SNmatchActionPerformed(evt);
            }
        });

        net.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.net.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(ORM2, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ORM2.text")); // NOI18N
        ORM2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ORM2ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(ORM1, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ORM1.text")); // NOI18N
        ORM1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ORM1ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(ORGO, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ORGO.text")); // NOI18N
        ORGO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ORGOActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(ORGI, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ORGI.text")); // NOI18N
        ORGI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ORGIActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(GLM1, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.GLM1.text")); // NOI18N
        GLM1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GLM1ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(GLM2, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.GLM2.text")); // NOI18N
        GLM2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GLM2ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(GLGI, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.GLGI.text")); // NOI18N
        GLGI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GLGIActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(GLGO, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.GLGO.text")); // NOI18N
        GLGO.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GLGOActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel5.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel6.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel7.text")); // NOI18N

        javax.swing.GroupLayout netLayout = new javax.swing.GroupLayout(net);
        net.setLayout(netLayout);
        netLayout.setHorizontalGroup(
            netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(netLayout.createSequentialGroup()
                .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(netLayout.createSequentialGroup()
                        .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(GLM1)
                            .addComponent(GLM2)
                            .addComponent(GLGI)
                            .addComponent(GLGO)
                            .addGroup(netLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(ORM1)
                            .addComponent(ORM2)
                            .addComponent(ORGI)
                            .addComponent(ORGO)))
                    .addGroup(netLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel7)))
                .addContainerGap())
        );
        netLayout.setVerticalGroup(
            netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(netLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ORM1)
                    .addComponent(GLM1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ORM2)
                    .addComponent(GLM2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ORGI)
                    .addComponent(GLGI))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(netLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ORGO)
                    .addComponent(GLGO))
                .addContainerGap())
        );

        FPGALED.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.FPGALED.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(RedPwr, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.RedPwr.text")); // NOI18N
        RedPwr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RedPwrActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(redRestore, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.redRestore.text")); // NOI18N
        redRestore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redRestoreActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(greenPwr, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.greenPwr.text")); // NOI18N
        greenPwr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                greenPwrActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(redAlarm, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.redAlarm.text")); // NOI18N
        redAlarm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redAlarmActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout FPGALEDLayout = new javax.swing.GroupLayout(FPGALED);
        FPGALED.setLayout(FPGALEDLayout);
        FPGALEDLayout.setHorizontalGroup(
            FPGALEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FPGALEDLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(FPGALEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(greenPwr)
                    .addComponent(RedPwr)
                    .addComponent(redAlarm)
                    .addComponent(redRestore))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        FPGALEDLayout.setVerticalGroup(
            FPGALEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FPGALEDLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(RedPwr)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(redAlarm)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(redRestore)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(greenPwr)
                .addContainerGap())
        );

        ASILED.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        org.openide.awt.Mnemonics.setLocalizedText(ASI1, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ASI1.text")); // NOI18N
        ASI1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ASI1ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(ASI3, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ASI3.text")); // NOI18N
        ASI3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ASI3ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(ASI4, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ASI4.text")); // NOI18N
        ASI4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ASI4ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(ASI2, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.ASI2.text")); // NOI18N
        ASI2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ASI2ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout ASILEDLayout = new javax.swing.GroupLayout(ASILED);
        ASILED.setLayout(ASILEDLayout);
        ASILEDLayout.setHorizontalGroup(
            ASILEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ASILEDLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ASI1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ASI2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ASI3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ASI4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ASILEDLayout.setVerticalGroup(
            ASILEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ASILEDLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ASILEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(ASI1)
                    .addComponent(ASI2)
                    .addComponent(ASI3)
                    .addComponent(ASI4))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(SNmatch)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(net, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(FPGALED, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(ASILED, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(SNmatch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(net, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(FPGALED, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ASILED, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void SelectFPGAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SelectFPGAActionPerformed
        int returnVal = FPGAfileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            FPGAfilename = FPGAfileChooser.getSelectedFile().getAbsolutePath();
            FPGAfileptr = FPGAfileChooser.getSelectedFile();
            FPGAfile.setText(FPGAfilename);
            FPGAselected = true;
            programFPGA.setEnabled(true);
            SelectFPGA.setEnabled(false);
        }
    }//GEN-LAST:event_SelectFPGAActionPerformed

    private void printitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printitActionPerformed
        appendCsv();
        print();
        cbsn = false;
        SNmatch.setSelected(false);
        FPGALED.setVisible(false);
        ASILED.setVisible(false);
        ORGI.setEnabled(false);
        ORGO.setEnabled(false);
    }//GEN-LAST:event_printitActionPerformed

    private void startTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startTestActionPerformed
        if (startTest.getText().equals("Start Test")) {
            runTest = true;
            stopTest = false;
            startTest.setText("Stop Test");
        } else {
            stopTest = true;
            runTest = false;
            startTest.setText("Start Test");
        }
    }//GEN-LAST:event_startTestActionPerformed

    private void operatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_operatorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_operatorActionPerformed

    private void initializeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_initializeActionPerformed
        setupScreen();
        startProgram = true;
    }//GEN-LAST:event_initializeActionPerformed

    private void serNumStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_serNumStateChanged
        setupScreen();
    }//GEN-LAST:event_serNumStateChanged

    private void programFPGAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_programFPGAActionPerformed
        if (connect123()) {
            if (deleteFPGA()) {
                if (programFPGA()) {
                    delay(10);
                    resetDevice(123);
                } else {
                    errorStatus.setText("Error: Failed to program FPGA");
                }
            } else {
                errorStatus.setText("Error: Failed to erase FPGA");
            }
        } else {
            if (connect50()) {
                if (deleteFPGA50()) {
                    if (programFPGA50()) {
                        delay(10);
                        resetDevice(50);
                    } else {
                        errorStatus.setText("Error: Failed to program FPGA");
                    }

                }

                errorStatus.setText("Error: Device must be at .123 IP to program FPGA");
            }
        }
    }//GEN-LAST:event_programFPGAActionPerformed

    private void SNmatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SNmatchActionPerformed
        cbsn = SNmatch.isSelected();
    }//GEN-LAST:event_SNmatchActionPerformed

    private void ORM1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ORM1ActionPerformed
        cborm1 = ORM1.isSelected();
    }//GEN-LAST:event_ORM1ActionPerformed

    private void ORM2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ORM2ActionPerformed
        cborm2 = ORM2.isSelected();
    }//GEN-LAST:event_ORM2ActionPerformed

    private void ORGIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ORGIActionPerformed
        cborgi = ORGI.isSelected();
    }//GEN-LAST:event_ORGIActionPerformed

    private void ORGOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ORGOActionPerformed
        cborgo = ORGO.isSelected();
    }//GEN-LAST:event_ORGOActionPerformed

    private void RedPwrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RedPwrActionPerformed
        cbredpwr = RedPwr.isSelected();
    }//GEN-LAST:event_RedPwrActionPerformed

    private void redAlarmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redAlarmActionPerformed
        cbredfault = redAlarm.isSelected();
    }//GEN-LAST:event_redAlarmActionPerformed

    private void redRestoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redRestoreActionPerformed
        cbredrestore = redRestore.isSelected();
    }//GEN-LAST:event_redRestoreActionPerformed

    private void greenPwrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_greenPwrActionPerformed
        cbgreen = greenPwr.isSelected();
    }//GEN-LAST:event_greenPwrActionPerformed

    private void ASI1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ASI1ActionPerformed
        cbasi1 = ASI1.isSelected();
    }//GEN-LAST:event_ASI1ActionPerformed

    private void ASI2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ASI2ActionPerformed
        cbasi2 = ASI2.isSelected();
    }//GEN-LAST:event_ASI2ActionPerformed

    private void ASI3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ASI3ActionPerformed
        cbasi3 = ASI3.isSelected();
    }//GEN-LAST:event_ASI3ActionPerformed

    private void ASI4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ASI4ActionPerformed
        cbasi4 = ASI4.isSelected();
    }//GEN-LAST:event_ASI4ActionPerformed

    private void GLM1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GLM1ActionPerformed
        cbglm1 = GLM1.isSelected();
    }//GEN-LAST:event_GLM1ActionPerformed

    private void GLM2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GLM2ActionPerformed
        cbglm2 = GLM2.isSelected();
    }//GEN-LAST:event_GLM2ActionPerformed

    private void GLGIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GLGIActionPerformed
        cbglgi = GLGI.isSelected();
    }//GEN-LAST:event_GLGIActionPerformed

    private void GLGOActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GLGOActionPerformed
        cbglgo = GLGO.isSelected();
    }//GEN-LAST:event_GLGOActionPerformed

    private void OKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OKActionPerformed
        ok = true;
        OK.setVisible(false);
    }//GEN-LAST:event_OKActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox ASI1;
    private javax.swing.JCheckBox ASI2;
    private javax.swing.JCheckBox ASI3;
    private javax.swing.JCheckBox ASI4;
    private javax.swing.JPanel ASILED;
    private javax.swing.JPanel FPGALED;
    private javax.swing.JLabel FPGAfile;
    private javax.swing.JFileChooser FPGAfileChooser;
    private javax.swing.JCheckBox GLGI;
    private javax.swing.JCheckBox GLGO;
    private javax.swing.JCheckBox GLM1;
    private javax.swing.JCheckBox GLM2;
    private javax.swing.JButton OK;
    private javax.swing.JCheckBox ORGI;
    private javax.swing.JCheckBox ORGO;
    private javax.swing.JCheckBox ORM1;
    private javax.swing.JCheckBox ORM2;
    private javax.swing.JCheckBox RedPwr;
    private javax.swing.JCheckBox SNmatch;
    private javax.swing.JButton SelectFPGA;
    private javax.swing.JLabel deviceStatus;
    private javax.swing.JLabel errorStatus;
    private javax.swing.JCheckBox greenPwr;
    private javax.swing.JButton initialize;
    private javax.swing.JLabel instructions;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JLabel lOper;
    private javax.swing.JLabel lgMAC;
    private javax.swing.JLabel lmMAC;
    private javax.swing.JLabel lserNum;
    private javax.swing.JPanel net;
    private javax.swing.JTextField operator;
    private javax.swing.JButton printit;
    private javax.swing.JButton programFPGA;
    private javax.swing.JCheckBox redAlarm;
    private javax.swing.JCheckBox redRestore;
    private javax.swing.JSpinner serNum;
    private javax.swing.JButton startTest;
    private javax.swing.JLabel testerPresent;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
