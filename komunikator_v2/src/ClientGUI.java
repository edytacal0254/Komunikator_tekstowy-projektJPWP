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

public class ClientGUI {
    private JPanel panel1;

    private JPanel panelConnection;
    private JTextField textFieldServerIp;
    private JSpinner spinnerServerPort;
    private JSpinner spinnerYourPort;
    private JTextField textFieldYourName;
    private JButton connectToServerButton;
    private JButton logoutButton;

    private JPanel panelChat;
    private JTextArea textAreaChat;
    private JTextArea textAreaMsg;
    private JTextField textFieldRecipent;
    private JButton buttonSend;

    private JPanel panelAvUsers;
    private JList listAvUsers;
    private JTextField textFieldClientIp;

    private static Client client;

    int lowerLimitPort = 49152;
    int upperLimitPort = 50000;

    int waitForResponse = 180;
    int howLong = 0;
    boolean waiting = false;


    DefaultListModel<String> defaultListModel = new DefaultListModel<>();
    String selectedRecipent = "ALL";

    public ClientGUI() {
        client = new Client();

        Timer timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    odswiez();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        timer.start();

        ClientGUI clientGUI_this = this;

        PrintStream out = new PrintStream( new CustomOutputStream( textAreaChat ) );
        System.setOut( out );
        //System.setErr( out ); errors should remain in console
        System.out.println( "Welcome" );

        SpinnerNumberModel spinnerNM1 = new SpinnerNumberModel(49500,lowerLimitPort,upperLimitPort,1);
        spinnerServerPort.setModel(spinnerNM1);

        SpinnerNumberModel spinnerNM2 = new SpinnerNumberModel(49501,lowerLimitPort,upperLimitPort,1);
        spinnerYourPort.setModel(spinnerNM2);

        textAreaMsg.setLineWrap(true);
        textAreaChat.setLineWrap(true);
        textAreaMsg.setWrapStyleWord(true);
        textAreaChat.setWrapStyleWord(true);

        //setEditable();

        connectToServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearGUI();
                client.setServerIp(textFieldServerIp.getText());
                client.setServerPort((int) spinnerServerPort.getValue());
                client.setClientIp(textFieldClientIp.getText());
                client.setClientPort((int) spinnerYourPort.getValue());
                client.setClientName(textFieldYourName.getText());

                if (client.checkIfNameIsValid(client.getClientName()).equals("true")) {
                    try {
                        client.startClient();
                        waiting = true;
                    } catch (IOException ex) {
                        System.out.println("Are you sure that you provided correct parameters?");
                        ex.printStackTrace();
                    }
                }else{
                    System.out.println(client.checkIfNameIsValid(client.getClientName()));
                }
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(JOptionPane.showConfirmDialog(null,"Are you sure?") == JOptionPane.OK_OPTION) {
                    try {
                        client.loggingOut();
                    } catch (IOException ex) {
                        System.out.println("Error while logging out");
                        ex.printStackTrace();
                    }
                }
            }
        });

        buttonSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.setNewMsg(textAreaMsg.getText());
                client.setRecipentName(textFieldRecipent.getText());
                client.setToSend(true);
                textAreaMsg.setText("");
            }
        });

        listAvUsers.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(!listAvUsers.isSelectionEmpty()){
                    selectedRecipent = (String) listAvUsers.getSelectedValue();
                }
            }
        });
    }

    public void odswiez() throws IOException {
        setEditable("client");

        if(waiting){
            howLong++;
            if(howLong==waitForResponse){
                try {
                    client.getClientConnection().close();
                }catch (NullPointerException e){

                }
                waiting = false;
                howLong = 0;
                System.out.println("<CLIENT> Server is not responding. Are you sure you provided correct parameters?");
            }
        }

        if(client.isIfServerAcceptedName() && waiting){
            waiting = false;
            howLong = 0;
        }


        textFieldRecipent.setText(selectedRecipent);

        //if(textAreaMsg.getText().length() > 1){
        //    textAreaMsg.setCaretPosition(textAreaMsg.getText().length() - 1);
        //}

        if(textAreaChat.getText().length() > 1) {
            textAreaChat.setCaretPosition(textAreaChat.getText().length() - 1); ///auto scrolling
        }

        if(client.getUserListNew() >0) {
            defaultListModel.removeAllElements();
            for (String user : client.getAvUsers().keySet()) {
                if (!user.equals(client.getClientName())) {
                    defaultListModel.addElement(user);
                }
            }
            listAvUsers.setModel(defaultListModel);
            client.setUserListNew(client.getUserListNew() - 1);
        }
    }

    public void clearGUI(){
        textAreaChat.setText("");
        textFieldRecipent.setText("");
        listAvUsers.clearSelection();
        listAvUsers.removeAll();
        textFieldRecipent.setText("ALL");
    }

    public void setEditable(String true_false_client){
        boolean connected = false;
        if(true_false_client.equals("true")){
            connected = true;
        }else if(true_false_client.equals("false")){
            connected = false;
        }else if (true_false_client.equals("client")){
            try {
                connected = client.getClientConnection().isConnected();
            }catch (NullPointerException e){
                connected = false;
            }
        }


        //if connected==true enable:
        panelAvUsers.setVisible(connected);
        textAreaMsg.setEditable(connected);
        buttonSend.setEnabled(connected);  //?
        logoutButton.setEnabled(connected);

        //if connected==true disable
        textFieldClientIp.setEditable((!connected));
        connectToServerButton.setEnabled(!connected);
        textFieldServerIp.setEditable(!connected);
        spinnerServerPort.setEnabled(!connected);
        spinnerYourPort.setEnabled(!connected);
        textFieldYourName.setEditable(!connected);

    }

    public static void main(String[] args) throws UnknownHostException, SocketException {

        JFrame frame = new JFrame("ClientGUI");
        frame.setContentPane(new ClientGUI().panel1);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);


        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (client.getClientConnection().isConnected()) {
                        if (JOptionPane.showConfirmDialog(null, "Are you sure?") == JOptionPane.OK_OPTION) {
                            try {
                                client.loggingOut();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            System.exit(0);
                        }
                    } else {
                        System.exit(0);
                    }
                }catch (NullPointerException e2){
                    System.exit(0);
                }
            }
        });
    }
}
