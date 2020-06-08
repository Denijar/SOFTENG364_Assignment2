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

    private static CostTable costTable;

    private static void handleIncomingConnection(Socket socket){
        System.out.println("New DVs received");
        try {

            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

            // Receive message and acknowledge
            String message = dataIn.readUTF();
            dataOut.writeUTF(ackMessage);
            dataOut.flush();
            dataOut.close();
            dataIn.close();

            // Process message
            String[] splitMessage = message.split(":");
            String headerType = splitMessage[0];
            String headerOriginatingDrone = splitMessage[1];
            // TODO: check for this being an update message
            String updates = splitMessage[2];

            // Process data
            System.out.println("Starting DV update calculation");
            costTable.processUpdates(headerOriginatingDrone, updates);
            System.out.println("DV update calculation finished");

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }
    }

    private static int pingClient(Client client){
        try{

            Socket socket = new Socket(client.getHostname(), client.getPort());
            // 10 second timeout
            socket.setSoTimeout(10*1000);
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

            // Send ping message to the client
            dataOut.writeUTF(pingMessage);

            // Start timing
            long startTime = System.currentTimeMillis();

            // Wait for the response from the client
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            dataIn.readUTF();

            // Stop timing
            long endTime = System.currentTimeMillis();
            int timeTaken = (int)((endTime - startTime)/1000);

            client.setResponseTime(timeTaken);
            dataOut.flush();
            dataOut.close();
            dataIn.close();
            socket.close();
            return timeTaken;

        } catch (IOException e){
            client.setResponseTime(-1);
            return -1;
        }
    }

    private static List<Client> performPingProcess(){
        List<Client> clientList = new ArrayList<>();
        try{

            System.out.println("Starting ping process #1");

            // Read CSV
            String row;
            BufferedReader csvReader = new BufferedReader(new FileReader(csvName));
            // Keep track of the number of clients that have the same name as me
            // for purpose of accurate printing
            int numMyself = 0;

            System.out.println("Reading client list: starting");

            while ((row = csvReader.readLine()) != null) {
                // Read the data from the .csv file and create Client object
                Client client = new Client(row);
                if (client.getClientName().equals(thisDroneName)) {
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
            System.out.println("Ping process #1 finished");

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }
        return clientList;
    }

    public static void main(String[] args) {
        List<Client> clientList = performPingProcess();
        costTable = new CostTable(thisDroneName, clientList);

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
