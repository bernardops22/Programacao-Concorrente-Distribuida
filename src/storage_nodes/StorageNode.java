package storage_nodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

import messages.CloudByte;

public class StorageNode {

    private String address, path;
    private int senderPort, receiverPort;
    private static byte[] fileContents;

    public StorageNode(String address, int senderPort, int receiverPort, String path){
        this.address = address;
        this.path = path;
        this.senderPort = senderPort;
        this.receiverPort = receiverPort;
    }

    public StorageNode(String address,int senderPort, int receiverPort){
        this.address = address;
        this.senderPort = senderPort;
        this.receiverPort = receiverPort;
    }
    
    private void getFileContent(String path) {
        try {
            fileContents = Files.readAllBytes(new File(path).toPath());
            if(fileContents.length != 1000000) throw new IOException();
            for (int i = 0; i < fileContents.length; i++)
                fileContents[i] = new CloudByte(fileContents[i]).getValue();
        }catch (IOException e) {
            /*e.printStackTrace();*/
            getFromOtherNodes();
        }
    }

    public void getFromOtherNodes() {
        System.err.println("Got file info from other nodes");
    }

    private void waitForErrorInjection(){
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String position = scanner.nextLine();
            if (position.split("\\s")[0].equals("ERROR")) {
                position = position.split("\\s+")[1];
                injectError(position);
            } else System.err.println("Command not recognized");
        }
    }

    private void injectError(String position){
        CloudByte cb = new CloudByte(position.getBytes()[0]);
        cb.makeByteCorrupt();
        fileContents[Integer.parseInt(position)] = cb.getValue();
        System.err.println("Error injected: " + cb);
    }

    public static void main(String[] args) {
        StorageNode sn = null;
        if (args.length == 3)
            sn = new StorageNode(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]));
        else if (args.length == 4)
            sn = new StorageNode(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]),args[3]);
        sn.getFileContent(sn.path);
        sn.waitForErrorInjection();
    }
}
