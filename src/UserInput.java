import java.util.Scanner;

/**
 * @author bernardosantos
 */
public class UserInput extends Thread {

    private final StorageNode node;

    public UserInput(StorageNode node){
        this.node = node;
    }

    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            while (true) {
                if (line.split(" ")[0].equals("ERROR"))
                    node.injectError(line.split(" ")[1]);
                else System.err.println("Command not recognized.");
                line = scanner.nextLine();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}