import java.util.NoSuchElementException;
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
                    try {

                        node.injectError(Integer.parseInt(line.split(" ")[1]));
                    }
                    catch (NoSuchElementException | NumberFormatException e){
                        System.err.println("Command not recognized.");
                    }
                line = scanner.nextLine();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}