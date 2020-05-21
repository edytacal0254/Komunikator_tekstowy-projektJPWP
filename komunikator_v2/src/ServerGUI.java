import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class ServerGUI {
    private JPanel panel1;
    private JLabel labelServerLogs;
    private JTextArea textAreaServerLogs;
    private JLabel labelServer;
    private JLabel labelServerIp;
    private JLabel labelPort;
    private JSpinner spinnerPort;
    private JTextField textFieldServerIp;
    private JButton startServerButton;
    private JButton turnOffServerButton;
    private JLabel labelAU;
    private JList listCU;
    private JButton kickOutUserButton;
    /////////////////////////////////////////

    private static Server server;
    private int selectedUser;

    DefaultListModel<String> defaultListModel = new DefaultListModel<>();

    int lowerLimitPort = 49152;
    int upperLimitPort = 50000;

    public ServerGUI() throws UnknownHostException, SocketException {
        server = new Server();

        Timer timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                odswiez();
            }
        });
        timer.start();


        PrintStream printStream = new PrintStream(new CustomOutputStream(textAreaServerLogs));
        System.setOut(printStream);
        //System.setErr(printStream); errors should remain in console

        textFieldServerIp.setEditable(false);
        textFieldServerIp.setText(server.getServerIP());
        textAreaServerLogs.setEditable(false);

        textAreaServerLogs.setLineWrap(true);
        textAreaServerLogs.setWrapStyleWord(true);

        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(49500,lowerLimitPort,upperLimitPort,1);
        spinnerPort.setModel(spinnerNumberModel);

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int serverPort = (int) spinnerPort.getValue();
                String serverIp = textFieldServerIp.getText();
                try {
                    server.startServer(serverPort,serverIp);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        turnOffServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    server.stopServer();
                } catch (IOException ex) {
                    System.out.println("Error while turning of server.");
                    ex.printStackTrace();
                }
            }
        });

        kickOutUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(selectedUser!=0){
                        server.kickOutUser(selectedUser);
                    }else {
                        System.out.println("You did not selected user.");
                    }
                } catch (IOException ex) {
                    System.out.println("Error while kicking out user.");
                    ex.printStackTrace();
                }
            }
        });

        listCU.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!listCU.isSelectionEmpty()){
                    StringTokenizer tmpSt = new StringTokenizer((String) listCU.getSelectedValue(), " : ");
                    selectedUser = Integer.parseInt(tmpSt.nextToken());

                }
            }
        });

    }

    public void enableComponents(){
        boolean ifServerRunning = server.isRunning();

        //enable if server is Running
        turnOffServerButton.setEnabled(ifServerRunning);
        listCU.setEnabled(ifServerRunning);
        //disable if server is Running
        textFieldServerIp.setEditable(!ifServerRunning);
        spinnerPort.setEnabled(!ifServerRunning);
        startServerButton.setEnabled(!ifServerRunning);
    }

    public void odswiez(){
        enableComponents();

        if (server.getUserListNew() >0) {
            defaultListModel.removeAllElements();
            for (int cNr : server.getClientHandlersHM().keySet()) {
                if (server.getClientNamesHM().containsKey(cNr)) {
                    defaultListModel.addElement(cNr + " : " + server.getClientNamesHM().get(cNr));
                } else {
                    defaultListModel.addElement(cNr + " :  <not_setted>");
                }
            }
            listCU.setModel(defaultListModel);
            server.setUserListNew(server.getUserListNew() - 1);
        }

        if(listCU.isSelectionEmpty()){
            kickOutUserButton.setEnabled(false);
        }else {
            kickOutUserButton.setEnabled(true);
        }


        if(textAreaServerLogs.getText().length()>0) {
            textAreaServerLogs.setCaretPosition(textAreaServerLogs.getText().length() - 1); ///auto scrolling
        }

    }

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("ServerGUI");
        frame.setContentPane(new ServerGUI().panel1);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(server.isRunning()){
                    if (JOptionPane.showConfirmDialog(frame,"Are you sure?") == JOptionPane.OK_OPTION) {
                        //serverClosing();
                        try {
                            server.stopServer();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        System.exit(0);
                    }
                }else {
                    System.exit(0);
                }
            }
        });
    }

}
