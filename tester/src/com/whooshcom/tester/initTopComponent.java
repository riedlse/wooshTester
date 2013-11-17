/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.whooshcom.tester;

import java.awt.Font;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.text.MessageFormat;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
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
    public boolean prReady = false;
    public String errorMessage = "";
    public String oper = "";
    public long errorDispStart = 0L;
    public boolean printDialog = false;
    public boolean printInteractive = false;
    public static JTextArea ptext;
    private static boolean device123 = false;
    private static boolean device124 = false;
    private static boolean device50 = false;
    private static boolean startProgram = false;
    private static boolean runTest = false;
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
        MessageFormat header = new MessageFormat(" ");
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
            //message(!complete, message);
        }
    }

    private void error(String msg) {
        errorMessage = msg;
        errorDispStart = System.currentTimeMillis();
//        message(true, msg);
    }

    private class ExecTest implements Runnable {

        @Override
        public void run() {
            Document doc;
            while (true) {
                oper = operator.getText();
                lOper.setText("Operator=" + oper);
                
                if (device123) {
                    initialize.setEnabled(true);
                    startTest.setEnabled(false);
                } else {
                    initialize.setEnabled(false);
                    if (device50) {
                        startTest.setEnabled(true);
                    } else {
                        startTest.setEnabled(false);
                    }
                }
               
                // try to connect to device    
                try {
                    doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123").timeout(3000).get();
                    device123 = true;
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
                            lmMAC.setText(String.format("Maintenance MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, m3, m2, m1));
                        }
                        if (key.equals("M1")) {
                            lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=" + value, e3, e2, e1));
                        }
                    }

                    if (startProgram) {
                        startProgram = false;
                        // Save Serial Number
                        try {
                            doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                                    .data("r", "Save-S/N")
                                    .data("M2", String.format("%05d", serialNumber))
                                    .timeout(3000)
                                    .get();
                        } catch (IOException ex) {
                            System.out.println("Error Setting Serial Number");
                            //Exceptions.printStackTrace(ex);
                        }

                        // Save Maintenance MAC
                        try {
                            doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                                    .data("r", "Save-MAC-(maintenance)")
                                    .data("M0", String.format("40-D8-55-%02X-%02X-%02X", m3, m2, m1))
                                    .timeout(3000)
                                    .get();
                        } catch (IOException ex) {
                            System.out.println("Error Setting maintenance MAC");
                            //Exceptions.printStackTrace(ex);
                        }

                        // Save gigE MAC
                        try {
                            doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                                    .data("r", "Save-MAC-(GIG-E)")
                                    .data("M1", String.format("40-D8-55-%02X-%02X-%02X", e3, e2, e1))
                                    .timeout(3000)
                                    .get();
                        } catch (IOException ex) {
                            System.out.println("Error Setting Gig E MAC");
                            //Exceptions.printStackTrace(ex);
                        }

                        // delete FPGA code
                        try {
                            doc = Jsoup.connect("http://ADMIN:admin@192.168.34.123/webpage.html")
                                    .data("d", " DeleteFPGA code")
                                    .timeout(3000)
                                    .get();
                        } catch (IOException ex) {
                            System.out.println("Error deleting FPGA code");
                            //Exceptions.printStackTrace(ex);
                        }

                    } else {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ex6) {
                            //Exceptions.printStackTrace(ex);
                        }
                    }

                } catch (IOException ex) {
                    System.out.println(" No device at http://192.168.34.123");
                    device123 = false;

                    // try to read back device status
                    try {
                        doc = Jsoup.connect("http://ADMIN:admin@192.168.34.124").timeout(3000).get();
                        device124 = true;
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
                        // try to reset device
                        try {
                            doc = Jsoup.connect("http://ADMIN:admin@192.168.34.124/0")
                                    .data("b", "")
                                    .timeout(3000)
                                    .get();
                        } catch (IOException ex1) {
                            System.out.println(" No device at http://ADMIN:admin@192.168.34.124");
                            //Exceptions.printStackTrace(ex);
                        }

                    } catch (IOException ex2) {
                        device124 = false;
                        System.out.println("Error reading 124 back box status");
                        //Exceptions.printStackTrace(ex);

                        try {
                            doc = Jsoup.connect("http://ADMIN:admin@192.168.34.50").timeout(3000).get();
                            device50 = true;
                            deviceStatus.setText("Device found at 192.168.34.50 - Ready to test");
                            Elements inputElements = doc.getElementsByTag("input");
                            for (Element inputElement : inputElements) {
                                String key = inputElement.attr("name");
                                String value = inputElement.attr("value");

                            }
                            
                            if (runTest) {
                                instructions.setText("Instructions: Running device tests");
                            } else {
                                instructions.setText("Instructions: Hook device up to tester and press Test");                                
                            }
                            
                        } catch (IOException ex3) {
                            device50 = true;
                            System.out.println("Error reading 50 back box status");
                            //Exceptions.printStackTrace(ex);
                        }
                        //Exceptions.printStackTrace(ex);
                    }

                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex6) {
                    //Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    /**
         * This method is called from within the constructor to initialize the
         * form. WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the Form Editor.
         */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

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

        jLabel1.setLabelFor(serNum);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(initialize, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.initialize.text")); // NOI18N
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

        org.openide.awt.Mnemonics.setLocalizedText(lOper, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.lOper.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(deviceStatus, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.deviceStatus.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(instructions, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.instructions.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(errorStatus, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.errorStatus.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(startTest, org.openide.util.NbBundle.getMessage(initTopComponent.class, "initTopComponent.startTest.text")); // NOI18N
        startTest.setEnabled(false);
        startTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startTestActionPerformed(evt);
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
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(serNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(initialize)
                        .addGap(39, 39, 39)
                        .addComponent(startTest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 187, Short.MAX_VALUE)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(operator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(lOper)
                        .addGap(49, 49, 49))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lmMAC)
                            .addComponent(lserNum))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                .addComponent(lOper)
                .addGap(18, 18, 18)
                .addComponent(deviceStatus)
                .addGap(38, 38, 38)
                .addComponent(lserNum)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lmMAC)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lgMAC)
                .addGap(29, 29, 29)
                .addComponent(instructions)
                .addGap(18, 18, 18)
                .addComponent(errorStatus)
                .addContainerGap(129, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(143, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(226, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void operatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_operatorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_operatorActionPerformed

    private void initializeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_initializeActionPerformed
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
        lmMAC.setText(String.format("Maintenance MAC set=40-D8-55-%02X-%02X-%02X Read=xx-xx-xx-xx-xx-xx", m3, m2, m1));
        lgMAC.setText(String.format("GigE MAC set=40-D8-55-%02X-%02X-%02X Read=xx-xx-xx-xx-xx-xx", e3, e2, e1));
        lserNum.setText("Serial Number set=" + serialNumber + " Read=xxxx");
        
        if (device123) {
            instructions.setText("Instructions: Programming Check LED sequence");
            startProgram = true;
        } else {
            errorStatus.setText("Error: No device to Program");
        }
    }//GEN-LAST:event_initializeActionPerformed

    private void startTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startTestActionPerformed
        runTest = true;
    }//GEN-LAST:event_startTestActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
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
    private javax.swing.JSpinner serNum;
    private javax.swing.JButton startTest;
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
