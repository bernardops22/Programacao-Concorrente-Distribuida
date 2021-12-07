import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @author bernardosantos
 */
public class Client {
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public Client(String addressTxt, int port) {
        System.err.println("Connecting to: " + addressTxt + ":" + port);
        try{
            InetAddress address = InetAddress.getByName(addressTxt);
            Socket socket = new Socket(address,port);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        }catch(IOException e){
            System.err.println("Problem in the arguments: Directory port and address must exist");
            e.printStackTrace();
            return;
        }
        addFrameContent();
    }

    public static void main(String[] args){
        if(args.length == 2)
            new Client(args[0], Integer.parseInt(args[1]));
        else
            throw new RuntimeException("Args must be address and port of node.");
    }

    private void centerFrame(JFrame frame) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int width = frame.getPreferredSize().width;
        int height = frame.getPreferredSize().height/2;
        int x = dim.width/2-width/2;
        int y = dim.height/2-height;
        frame.setLocation(x,y);
    }

    private void addFrameContent(){
        final JFrame frame = new JFrame("Client");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);

        JPanel information = new JPanel();

        information.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        information.setLayout(new GridLayout(0,7,10,0));

        JPanel results = new JPanel();

        information.add(Box.createHorizontalStrut(1));

        JLabel indexLabel = new JLabel("Posição a consultar:", SwingConstants.CENTER);
        information.add(indexLabel);

        JTextField indexField = new JTextField("1000");
        information.add(indexField);

        JLabel lengthLabel = new JLabel ("Comprimento:", SwingConstants.CENTER);
        information.add(lengthLabel);

        JTextField lengthField = new JTextField("10");
        information.add(lengthField);

        JTextArea resultsField = new JTextArea(10,100);
        resultsField.setEditable(false);
        resultsField.setLineWrap(true);
        resultsField.setWrapStyleWord(true);
        resultsField.setText("Respostas aparecerão aqui...");
        JScrollPane resultsFieldScroll = new JScrollPane(resultsField);
        results.add(resultsFieldScroll);

        JButton consultButton = new JButton("Consultar");
        consultButton.addActionListener(e -> {
            try {
                out.writeObject(new ByteBlockRequest(Integer.parseInt(indexField.getText()), Integer.parseInt(lengthField.getText())));
                CloudByte[] block = (CloudByte[]) in.readObject();
                String text = "";
                for (CloudByte cloudByte : block)
                    text = text.concat(cloudByte.toString());
                resultsField.setText(text);
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
        information.add(consultButton);

        information.add(Box.createHorizontalStrut(1));

        frame.add(information);
        frame.add(results, BorderLayout.SOUTH);

        centerFrame(frame);
        frame.pack();
        frame.setVisible(true);
    }
}