package client;

import javax.swing.*;
import java.awt.*;

public class Client {

    private final JFrame frame;

    public Client(){
        frame = new JFrame("Client");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        addFrameContent();
        centerFrame();
        frame.pack();
    }

    private void centerFrame() {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int width = frame.getPreferredSize().width;
        int height = frame.getPreferredSize().height/2;
        int x = dim.width/2-width/2;
        int y = dim.height/2-height;
        frame.setLocation(x,y);
    }

    public void open() {
        frame.setVisible(true);
    }

    private void addFrameContent() {

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
        consultButton.addActionListener(e -> resultsField.setText(indexField.getText()+":"+lengthField.getText()));
        information.add(consultButton);

        information.add(Box.createHorizontalStrut(1));

        frame.add(information);
        frame.add(results, BorderLayout.SOUTH);
    }

    public static void main(String[] args){
        Client window = new Client();
        window.open();
    }
}