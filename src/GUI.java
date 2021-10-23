import javax.swing.*;
import java.awt.*;

public class GUI {

    private final JFrame frame;

    public GUI(){
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

        JButton consultButton = new JButton("Consultar");
        information.add(consultButton);

        information.add(Box.createHorizontalStrut(1));

        JTextArea resultsField = new JTextArea(10,100);
        resultsField.setText("Respostas aparecerão aqui...");
        results.add(resultsField);

        frame.add(information);
        frame.add(results, BorderLayout.SOUTH);
    }

    public static void main(String[] args){
        GUI window = new GUI();
        window.open();
    }

}