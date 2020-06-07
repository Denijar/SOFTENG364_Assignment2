import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class DSRSNetwork {

    private static final String thisDroneName = "Relay1";
    private static final String csvName = "./clients-" + thisDroneName + ".csv";
    private static final String message = "PING";
    private static int pingNumber = 1;

    private static int pingClient(Client client){
        try{

            Socket socket = new Socket(client.getHostname(), client.getPort());
            // 5 second timeout
            socket.setSoTimeout(5*1000);
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

            // Send ping message to the client
            dataOut.writeUTF(message);

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
                String[] data = row.split(",");
                Client client = new Client(data);

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

    public static void main(String[] args) {
        performPingProcess();
    }
}
