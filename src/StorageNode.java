import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author bernardosantos
 */
public class StorageNode {

    private static final int FILE_SIZE = 1000000;
    private static final int BLOCK_SIZE = 100;
    private static final int MAX_BLOCKS = 10000;

    private final InetAddress serverAddress;
    private final String path;
    private final int senderPort, receiverPort;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public final CloudByte[] fileContent = new CloudByte[FILE_SIZE];
    private final List<String> nodes = new ArrayList<>();
    private final BlockingQueue<CloudByte> correction = new ArrayBlockingQueue<>(2);
    public final BlockingQueue<ByteBlockRequest> queue = new ArrayBlockingQueue<>(MAX_BLOCKS);

    /**
     * Construtor da classe capaz de receber até quatro argumentos.
     * O argumento path pode ser null, mas a verificação do mesmo é feita no método runNode.
     */
    public StorageNode(InetAddress serverAddress, int senderPort, int receiverPort, String path){
        this.serverAddress = serverAddress;
        this.senderPort = senderPort;
        this.receiverPort = receiverPort;
        this.path = path;
    }

    /**
     * Assim que o código inicia temos dois casos.
     * O node tem acesso inicial ao ficheiro bin
     * O node não tem acesso inicial ao ficheiro bin
     */
    public static void main(String[] args) throws UnknownHostException {
        try {
            if (args.length == 3)
                new StorageNode(InetAddress.getByName(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), null).runNode();
            else if (args.length == 4)
                new StorageNode(InetAddress.getByName(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]).runNode();
            else throw new IllegalArgumentException("Number of arguments invalid");
        } catch (NumberFormatException e){
            throw new NumberFormatException("Inserted port(s) invalid");
        } catch (UnknownHostException e1) {
            throw new UnknownHostException("Inserted IP invalid");
        } catch (Exception e2) {
            System.err.println("Problem in the arguments: Directory port and address must be written, " +
                    "followed by the node port and the data file name.");
            e2.printStackTrace();
        }
    }

    /**
     * Através deste método são chamados todos os métodos necessários ao funcionamento da classe.
     * Algumas das verificações são também feitas neste método.
     */
    private void runNode() {
        try{
            connectToTheDirectory();
            if(!registerInTheDirectory()) {
                System.err.println("Client already enrolled. Try changing port number.");
                return;
            }
            if (path!=null)
                getFileContent();
            else {
                getNodesList();
                if(nodes.size() != 0) {
                    createQueue();
                    getContentFromNodes();
                }
                else{
                    System.err.println("No nodes available beside yours.");
                    return;
                }
            }
            new UserInput(this).start();
            System.err.println("Accepting connections...");
            startErrorAnalysis();
            waitAndSendData();
        }catch (IOException | InterruptedException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    /**
     * Neste método é criada a ligação do nó ao diretório.
     * São também criados canais de comunicação de texto.
     * Nota: Método instanciado pelo runNode().
     */
    private void connectToTheDirectory() throws IOException {
        socket = new Socket(serverAddress,senderPort);
        out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())),
                true);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
    }

    /**
     * Nesta função é feita a inscrição do nó no diretório.
     * @return Retorna true se o nó for registado com sucesso (mensagem recebida pelo diretório).
     * Nota: Função instanciada pelo runNode().
     */
    private boolean registerInTheDirectory() throws IOException {
        out.println("INSC " + socket.getLocalAddress().getHostAddress() + " " + receiverPort);
        System.err.println("Sending to directory: INSC " + socket.getLocalAddress().getHostAddress() + " " + receiverPort);
        return in.readLine().equals("true");
    }

    /**
     * Nesta função o ficheiro dado é lido e convertido num vetor de CloudByte.
     * Esta função apenas é instanciada caso o caminho para o ficheiro bin for dado e o
     * nó se tenha conseguido inscrever no diretório.
     */
    private void getFileContent() throws IOException {
        try {
            byte[] fileContentsTemp = Files.readAllBytes(new File(path).toPath());
            if (fileContentsTemp.length != FILE_SIZE) throw new IOException();
            for (int i = 0; i < fileContentsTemp.length; i++)
                fileContent[i] = new CloudByte(fileContentsTemp[i]);
            System.err.println("Loaded data from file: " + fileContent.length);
        } catch (IOException e){
            throw new IOException("File not valid. Problem in the path or in the file content.");
        }
    }

    /**
     * Neste método faz-se um pedido ao diretório dos nós atualmente inscritos.
     * É criada uma lista de nós inscritos no diretório (excluindo o próprio).
     * Este método apenas é instanciado caso o caminho para o ficheiro bin for null e o
     * nó se tenha conseguido inscrever no diretório.
     * Nota: Método instanciado pelo runNode().
     */
    private void getNodesList() throws IOException {
        nodes.clear();
        System.err.println("Querying directory for other nodes...");
        out.println("nodes");
        String line = in.readLine();
        while (!line.equals("end")) {
            System.err.println("Got answer: " + line);
            if (!line.equals("node " + socket.getLocalAddress().getHostAddress() + " " + receiverPort))
                nodes.add(line);
            line = in.readLine();
        }
    }

    /**
     * Neste método são adicionados os pedidos necessários BlockingQueue para possível receber o
     * conteúdo total do ficheiro presente nos outros nós inscritos no diretório.
     * Este método apenas é instanciado caso o caminho para o ficheiro bin for null,
     * se o nó se tenha conseguido inscrever no diretório e
     * se a lista de nós do diretório for diferente de zero.
     * Nota: Método instanciado pelo runNode().
     */
    private void createQueue() throws InterruptedException {
        for(int i = 0; i < FILE_SIZE; i += BLOCK_SIZE)
            queue.put(new ByteBlockRequest(i, BLOCK_SIZE));
    }

    /**
     * Neste método são enviados pedidos aos nós consoante o conteúdo da BlockingQueue
     * Este método apenas é instanciado caso o caminho para o ficheiro .bin for null,
     * se o nó se tenha conseguido inscrever no diretório,
     * se a lista de nós presentes no diretório for diferente de zero e
     * após ter sido atualizada a lista de pedidos a fazer aos outros nós.
     * Nota: Método instanciado pelo runNode().
     */
    private void getContentFromNodes() throws ClassNotFoundException, InterruptedException {
        final long time = System.currentTimeMillis();
        CountDownLatch cdl = new CountDownLatch(nodes.size());
        for (String node : nodes) {
            System.err.println("Launching download thread: " + node);
            new DealWithRequest(this,node.split(" ")[1], Integer.parseInt(node.split(" ")[2]), cdl).start();
        }
        cdl.await();
        System.err.println("Elapsed time: " + (System.currentTimeMillis() - time));
    }

    /**
     * A partir deste momento, os nós estão prontos a receber pedidos de outros nós e
     * procuram constantemente por erros no seu ficheiro.
     * Este método apenas é instanciado após os nós terem o conteúdo total do ficheiro, quer por via direta,
     * quer através de outros nós.
     * Nota: Método instanciado pelo runNode().
     */
    private void startErrorAnalysis() {
        Lock lock = new ReentrantLock();
        new CheckForParityErrors(this, 0,lock).start();
        new CheckForParityErrors(this, 1,lock).start();
    }

    /**
     * Neste método o nó espera por pedidos de blocos de CloudByte e envia sempre que conseguir.
     * É instanciado o método checkBlockForErrors para não serem enviados blocos com erros de paridade.
     * Nota: Método instanciado pelo runNode().
     */
    private void waitAndSendData() throws IOException {
        ServerSocket serverSocket = new ServerSocket(receiverPort);
        while(true){
            Socket getContentSocket = serverSocket.accept();
            ObjectOutputStream out = new ObjectOutputStream(getContentSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(getContentSocket.getInputStream());
            while(true) {
                try {
                    ByteBlockRequest request = (ByteBlockRequest) in.readObject();
                    int requestSize = request.getLength();
                    CloudByte[] block = new CloudByte[requestSize];
                    for (int i = 0; i < requestSize; i++)
                        block[i] = fileContent[i + request.getStartIndex()];
                    if(checkBlockForErrors(block)) out.writeObject(block);
                    else out.writeObject(null);
                }catch(Exception e) {
                    break;
                }
            }
            getContentSocket.close();
            out.close();
            in.close();
        }
    }

    /**
     * Este método verifica a existência de erros nos blocos a enviar aos nós de
     * forma a impedir o envio de bytes errados.
     * Nota: Método instanciado no método acceptingConnections.
     */
    private boolean checkBlockForErrors(CloudByte[] block){
        for(int i = 0; i != block.length; i++)
            if(!block[i].isParityOk())
                return false;
        return true;
    }

    /**
     * Método que inicia threads de pesquisa do valor real do CloudByte que contém o erro.
     * Limitação: Apenas é possível corrigir um erro de cada vez.
     * Nota: Método instanciado pela classe CheckForParityErrors caso seja detetado algum erro.
     */
    public void startErrorCorrection(int position) throws IOException, ClassNotFoundException, InterruptedException {
        try {
            System.err.println("Data Maintenance: Error was detected at " + position + ": " + fileContent[position]);
            getNodesList();
            if (nodes.size() >= 2) {
                for (int i = 0; i != nodes.size(); i++)
                    queue.add(new ByteBlockRequest(position, 1));
            } else {
                System.err.println("Cannot correct the error. Insufficient number of nodes." +
                        "\nDisconnecting node.");
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Neste método são lançadas threads usadas para receber os valores corretos do “byte” errado.
     * Caso sejam recebidos dois valores iguais, o “byte” é corrigido para o novo valor.
     * Método de correção de erros.
     * Limitação: Pressupõe-se que os “bytes” recebidos são sempre iguais.
     * Nota: Método instanciado pela thread CheckForParityErrors.
     */
    public void correctError(int position) throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(2);
        for (String node : nodes) {
            System.err.println("Launching correction thread: " + node);
            new DealWithError(this, node.split(" ")[1], Integer.parseInt(node.split(" ")[2]), cdl, correction).start();
        }
        cdl.await();
        CloudByte b1 = correction.take();
        CloudByte b2 = correction.take();
        if(b1.equals(b2)) {
            fileContent[position] = b1;
            System.err.println("Corrected to: " + fileContent[position]);
            System.err.println("Continuing error analysis...");
        }
    }

    /**
     * Injeção local do erro para posterior correção pelas threads de procura de erros.
     * Nota: Método instanciado na classe UserInput
     */
    public void injectError(int position){
        if(position >= 0 && position <= 999999) {
            fileContent[position].makeByteCorrupt();
            System.err.println("Error injected: " + fileContent[position]);
        }else System.err.println("Invalid array position.");
    }
}