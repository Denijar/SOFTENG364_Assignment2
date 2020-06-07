import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class DSRSNetwork {

    private static final int localPort = 10120;
    private static final String thisDroneName = "Relay1";
    private static final String csvName = "./clients-" + thisDroneName + ".csv";
    private static final String pingMessage = "PING\n";
    private static final String ackMessage = "ACK\n";
    private static int pingNumber = 1;

    private static int pingClient(Client client){
        try{

            Socket socket = new Socket(client.getHostname(), client.getPort());
            // 5 second timeout
            socket.setSoTimeout(5*1000);
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

            // Send ping message to the client
            dataOut.writeUTF(pingMessage);

            // Start timing
            long startTime = System.currentTimeMillis();

            // Wait for the response from the client
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            String msg = dataIn.readUTF();

            // Stop timing
            long endTime = System.currentTimeMillis();
            int timeTaken = (int)((endTime - startTime)/1000);

            client.setResponseTime(timeTaken);
            return timeTaken;

        } catch (IOException e){
            client.setResponseTime(-1);
            return -1;
        }
    }

    private static void performPingProcess(){
        try{

            System.out.println("Starting ping process #" + pingNumber);

            // Read CSV and create Client list
            String row;
            BufferedReader csvReader = new BufferedReader(new FileReader(csvName));
            List<Client> clientList = new ArrayList<>();
            // Keep track of the number of clients that have the same name as me
            // for purpose of accurate printing
            int numMyself = 0;

            System.out.println("Reading client list: starting");

            while ((row = csvReader.readLine()) != null) {
                // Read the data from the .csv file and create Client object
                Client client = new Client(row);
                if(client.getClientName().equals(thisDroneName)){
                    numMyself++;
                }
                clientList.add(client);
            }
            csvReader.close();

            System.out.println("Reading client list: finished - " + (clientList.size() - numMyself) + " clients read");
            System.out.println("Pinging all clients: starting");

            // Ping all clients
            for(Client client : clientList){
                // Skip a client if they have the same name as me
                if(client.getClientName().equals(thisDroneName)){
                    continue;
                }
                System.out.print("- Pinging " + client.getClientName() + "...");
                int timeTaken = pingClient(client);
                if(timeTaken == -1){
                    System.out.println("could not ping");
                } else {
                    System.out.println("ping received after " + client.getResponseTime() + "s");
                }
            }

            System.out.println("Pinging client list: finished - " + (clientList.size() - numMyself) + " clients pinged");
            System.out.println("Writing all clients: starting");

            // Overwrite the CSV with new data
            FileWriter csvWriter = new FileWriter(csvName);
            for(Client client : clientList){
                csvWriter.append(client.toString());
            }
            csvWriter.flush();
            csvWriter.close();

            System.out.println("Writing client list: finished - " + (clientList.size() - numMyself) + " clients written");
            System.out.println("Ping process #" + pingNumber + " finished");

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }
        return;
    }

    private static void handleIncomingConnection(Socket socket){
        System.out.println("New DVs received");
        System.out.println("Starting DV update calculation");
        try {

            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            // Receive message and process
            String message = dataIn.readUTF();
            System.out.println("message received: " + message);

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }

        System.out.println("DV update calculation finished");

    }

    public static void main(String[] args) {
        performPingProcess();

        try{
            // Open a socket and wait for incoming messages
            ServerSocket serverSocket = new ServerSocket(localPort);
            while(true){
                Socket socket = serverSocket.accept();
                handleIncomingConnection(socket);
            }


        } catch (IOException e ){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }


    }
}
