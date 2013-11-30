/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.whooshcom.tester;

import java.awt.Color;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
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
    "CTL_initTopComponent=init Window",
    "HINT_initTopComponent=This is a init window"
})
public final class initTopComponent extends TopComponent {

    public static String printText = " ";
    public static String FPGAfilename = " ";
    public File FPGAfileptr;
    public static String maintMAC = "";
    public static String gigeMAC = "";
    public static String smaintMAC = "";
    public static String sgigeMAC = "";
    public static String sserialNumber = "";
    public boolean FPGAselected = false;
    public boolean prReady = false;
    public String errorMessage = "";
    public String oper = "";
    public long errorDispStart = 0L;
    public boolean printDialog = true;
    public boolean printInteractive = true;
    public static JTextArea ptext;
    private static boolean startProgram = false;
    private static boolean runTest = false;
    private static boolean stopTest = false;
    private final String useFont = "Consolas";
    private final int baseMAC = 0x0A5000;
    private int serialNumber;
    private int maintMac;
    private int gigeMac;
    private int m3;
    private int m2;
    private int m1;
    private int e3;
    private int e2;
    private int e1;

    public initTopComponent() {
               
        initComponents();
        setName(Bundle.CTL_initTopComponent());
        setToolTipText(Bundle.HINT_initTopComponent());
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);

        // create a blind text area for the print function to use.
        ptext = new javax.swing.JTextArea();
        ptext.setLineWrap(true);
        ptext.setFont(new java.awt.Font(useFont, Font.PLAIN, 10));
        ptext.setColumns(130);
        ptext.setRows(100);
        
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(1201, 1001, 2000, 1);
        serNum.setModel(spinnerNumberModel);
        
        Thread t = new Thread(new ExecTest());
        t.start();  
    }
    
    private void message(boolean error, String msg) {
        int type = (error ? JOptionPane.ERROR_MESSAGE
                : JOptionPane.INFORMATION_MESSAGE);
        //JOptionPane.showMessageDialog(this.getComponent(), msg, "Printing", type);
    }

    public void print() {
        
        printText = lserNum.getText() + "\n";
        printText += lmMAC.getText() + "\n";
        printText += lgMAC.getText() + "\n";
             
        MessageFormat header = new MessageFormat(" Whooshcom Production Test Follower");
        MessageFormat footer = new MessageFormat("Whooshcom Test Automation v" + 1 + "." + 0 + "." + 0 + "          Page - {0}");
        ptext.setText(printText);
        PrintingTask task = new PrintingTask(header, footer);
        task.execute();
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
                //aset.add(new Copies(5));
                //aset.add(MediaSize.A4);
                //aset.add(Sides.DUPLEX);
               
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
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123").timeout(3000).get();
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
            deviceStatus.setText("No Device at http://192.168.34.123");
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
                    .timeout(3000)
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
                        .timeout(3000)
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
                    .timeout(3000)
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
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm").timeout(3000).get();
            deviceStatus.setText("Tester found at 192.168.34.121");
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
            errorStatus.setText("ERROR: No Tester Present");
            return (false);
        }
        return (true);
    }

    private boolean checkFail() {
        System.out.println("Trying to connect to tester at http://192.168.34.121/XML_state.htm");
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/XML_state.htm").timeout(3000).get();
            //Element link = doc.select("state").first();
            String state = doc.body().text();
            System.out.println("State=" + state);
            if (state.equalsIgnoreCase("failed")) {
                errorStatus.setText("ERROR: Device Failed Test");
                testerPresent.setForeground(Color.red);
                testerPresent.setText("Tester Present: FAILED");
                return (false);
            } else {
                testerPresent.setForeground(Color.green);
                testerPresent.setText("Tester Present: TESTING");
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

    private boolean saveMaint() {
        boolean rc = true;
        System.out.println("Saving Maintenance MAC=" + smaintMAC);
        // Save Maintenance MAC
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                    .data("r", "Save-MAC-(maintenance)")
                    .data("M0", smaintMAC)
                    .timeout(3000)
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
                    .timeout(3000)
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
                    .timeout(3000)
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
                    .timeout(3000)
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
                    .timeout(3000)
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
                    .timeout(3000)
                    .get();
        } catch (IOException ex10) {
            //Exceptions.printStackTrace(ex);
            errorStatus.setText("ERROR: tester save and apply changes");
            return(false);
        }

        // Verify mac set correctly
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .timeout(3000)
                    .get();
            deviceStatus.setText("Tester found at 192.168.34.121");
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
        System.out.println("Sleeping " + seconds + " seconds");
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
                    .timeout(3000)
                    .get();
        } catch (IOException ex1) {
            errorStatus.setText("Error resetting device at http://ADMIN:admin@192.168.34.123");
            //Exceptions.printStackTrace(ex);
        }
    }

    private boolean connect124() {
        // try to read back device status
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.124").timeout(3000).get();
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
            Document doc = Jsoup.connect("http://192.168.34.50/device.htm").timeout(3000).get();
            deviceStatus.setText("Device found at 192.168.34.50 - Ready to test / Device");
            //String Serial = Jsoup.parse(doc).select("tr:matchesOwn(Serial Number)").first().nextSibling().toString());
            Elements inputElements = doc.getElementsByTag("tr");
            for (Element inputElement : inputElements) {
                String key = inputElement.attr("name");
                String value = inputElement.attr("value");
                String text = inputElement.attr("text");
                System.out.println("name=" + key + " value=" + value + " text=" + text);

            }
            Document doc1 = Jsoup.connect("http://192.168.34.50/mNetwork.htm").timeout(3000).get();
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
            Document doc2 = Jsoup.connect("http://192.168.34.50/tStream.htm").timeout(3000).get();
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
            errorStatus.setText("Error reading back box status ip 192.168.34.50");
            //Exceptions.printStackTrace(ex);
            return(false);
        }
        return(true);
    }

    private boolean testerOff() {
        // VInsure tester is not running
        try {
            Document doc = Jsoup.connect("http://ADMIN:admin@192.168.34.121/Tester.htm")
                    .timeout(3000)
                    .get();
            deviceStatus.setText("Tester found at 192.168.34.121");
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
            boolean setupDone = false;
            while (!setupDone) {
                oper = operator.getText();
                if (oper.equals("abc")) {
                    instructions.setText("Instructions: Please enter Operator initials");
                } else {
                    lOper.setText("Operator=" + oper);
                    lOper.setForeground(Color.green);
                    if (!FPGAselected) {
                        instructions.setText("Instructions: Please select FPGA file");
                    } else {
                        if (connectTester()) {
                            setupDone = true;
                        } else {
                            instructions.setText("Instructions: Please connect Tester");
                        }
                    }
                }
                delay(1);
            }
            
            operator.setEnabled(false);
            SelectFPGA.setEnabled(false);

            while (true) {
                instructions.setText("Instructions: Searching for Device");
                while (connect123()) {
                    initialize.setEnabled(true);
                    startTest.setEnabled(false);
                    instructions.setText("Instructions: Set serial number and initialize");
                    if (startProgram) {
                        startProgram = false;
                        if (saveGige()) {
                            if (deleteFPGA()) {
                                if (programFPGA()) {
                                    delay(10);
                                    if (saveMaint()) {
                                        if (!saveSerial()) {
                                            errorStatus.setText("Error: Failed to program Serial Number");
                                        }
                                        resetDevice(123);
                                    } else {
                                        errorStatus.setText("Error: Failed to program Maintenance MAC");
                                    }
                                } else {
                                    errorStatus.setText("Error: Failed to program FPGA");
                                }
                            } else {
                                errorStatus.setText("Error: Failed to erase FPGA");
                            }
                        } else {
                            errorStatus.setText("Error: Failed to program GigE MAC");
                        }
                    }
                    delay(3);
                }
                initialize.setEnabled(false);
                while (connect124()) {
                    resetDevice(124);
                    delay(3);
                }
                while (connect50()) {
                    instructions.setText("Instructions: Connect tester and start test");
                    startTest.setEnabled(true);
                    if (runTest) {
                        runTest = false;
                        startTest.setText("Stop Test");
                        instructions.setText("Instructions: Running device tests");
                        testerOff();
                        setTesterMAC();
                        startTester();
                        boolean failed = false;
                        while (!failed || !stopTest) {
                            failed = checkFail();
                            delay(3);
                        }
                        stopTest = false;
                        testerOff();
                        instructions.setText("Instructions: Hook device up to tester and press Test");
                        startTest.setText("Start Test");
                    } else {
                        instructions.setText("Instructions: Hook device up to tester and press Test");
                        startTest.setText("Start Test");
                    }
                    delay(3);
                }
                startTest.setEnabled(false);
                delay(3);
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
        serNum = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        initialize = new javax.swing.JButton();
        operator = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        lmMAC = new javax.swing.JLabel();
        lgMAC = new javax.swing.JLabel();
        lserNum = new javax.swing.JLabel();
        lOper = new javax.swing.JLabel();
        deviceStatus = new javax.swing.JLabel();
        instructions = new javax.swing.JLabel();
        errorStatus = new javax.swing.JLabel();
        startTest = new javax.swing.JButton();
        printit = new javax.swing.JButton();
        testerPresent = new javax.swing.JLabel();
        SelectFPGA = new javax.swing.JButton();
        FPGAfile = new javax.swing.JLabel();
        programFPGA = new javax.swing.JButton();

        serNum.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                serNumStateChanged(evt);
            }
        });

        jLabel1.setLabelFor(serNum);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(initialize, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.initialize.text_1")); // NOI18N
        initialize.setEnabled(false);
        initialize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                initializeActionPerformed(evt);
            }
        });

        operator.setText(org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.operator.text")); // NOI18N
        operator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                operatorActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lmMAC, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lmMAC.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lgMAC, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lgMAC.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lserNum, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lserNum.text")); // NOI18N

        lOper.setForeground(new java.awt.Color(255, 0, 0));
        org.openide.awt.Mnemonics.setLocalizedText(lOper, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lOper.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(deviceStatus, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.deviceStatus.text")); // NOI18N

        instructions.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(instructions, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.instructions.text")); // NOI18N

        errorStatus.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(errorStatus, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.errorStatus.text")); // NOI18N

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

        testerPresent.setForeground(new java.awt.Color(255, 0, 0));
        org.openide.awt.Mnemonics.setLocalizedText(testerPresent, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.testerPresent.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(SelectFPGA, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.SelectFPGA.text")); // NOI18N
        SelectFPGA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectFPGAActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(FPGAfile, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.FPGAfile.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(programFPGA, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.programFPGA.text")); // NOI18N
        programFPGA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                programFPGAActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(SelectFPGA)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(FPGAfile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(printit)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lmMAC)
                            .addComponent(lserNum))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(testerPresent)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(initialize)
                                .addGap(39, 39, 39)
                                .addComponent(startTest)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 159, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(operator, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lOper, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(41, 41, 41))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(programFPGA)
                            .addComponent(errorStatus)
                            .addComponent(instructions)
                            .addComponent(deviceStatus)
                            .addComponent(lgMAC))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(initialize)
                    .addComponent(operator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(startTest))
                .addGap(1, 1, 1)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lOper)
                    .addComponent(testerPresent))
                .addGap(18, 18, 18)
                .addComponent(deviceStatus)
                .addGap(38, 38, 38)
                .addComponent(lserNum)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lmMAC)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lgMAC)
                .addGap(37, 37, 37)
                .addComponent(instructions)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(errorStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(programFPGA)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(printit)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(SelectFPGA)
                            .addComponent(FPGAfile))
                        .addGap(16, 16, 16))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void SelectFPGAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SelectFPGAActionPerformed
        int returnVal = FPGAfileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            FPGAfilename = FPGAfileChooser.getSelectedFile().getAbsolutePath();
            FPGAfileptr = FPGAfileChooser.getSelectedFile();
            FPGAfile.setText(FPGAfilename);
            FPGAselected = true;
        }
    }//GEN-LAST:event_SelectFPGAActionPerformed

    private void printitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printitActionPerformed
        print();
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
            errorStatus.setText("Error: Device must be at .123 IP to program FPGA");
        }
    }//GEN-LAST:event_programFPGAActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FPGAfile;
    private javax.swing.JFileChooser FPGAfileChooser;
    private javax.swing.JButton SelectFPGA;
    private javax.swing.JLabel deviceStatus;
    private javax.swing.JLabel errorStatus;
    private javax.swing.JButton initialize;
    private javax.swing.JLabel instructions;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lOper;
    private javax.swing.JLabel lgMAC;
    private javax.swing.JLabel lmMAC;
    private javax.swing.JLabel lserNum;
    private javax.swing.JTextField operator;
    private javax.swing.JButton printit;
    private javax.swing.JButton programFPGA;
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
