import java.io.*;
import java.net.*;

public class DSRSNetwork {

    private static final String dronename = "Relay1";
    private static final String csvName = "./clients-" + dronename + ".csv";

    private static int pingClient(String hostname, int port){
        try{

            Socket socket = new Socket(hostname, port);
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

            // Send ping message to the client
            dataOut.writeUTF("PING\n");

            // Start timing
            long startTime = System.currentTimeMillis();

            // Wait for the response from the client
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            String msg = dataIn.readUTF();

            // Stop timing
            long endTime = System.currentTimeMillis();

            int timeTaken = (int)((endTime - startTime)/1000);

            return timeTaken;

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public static void main(String[] args) {

        // Keep track of the content to overwrite the CSV with
        StringBuilder newCSVContents = new StringBuilder();

        try{
            
            // Read CSV and ping clients
            String row;
            BufferedReader csvReader = new BufferedReader(new FileReader(csvName));
            while ((row = csvReader.readLine()) != null) {

                // Read the data from the .csv file
                String[] data = row.split(",");
                // Parse the data, assigning to variables
                String clientName = data[0];
                String clientType = data[1];
                String[] hostnamePort = data[2].split(":");
                String hostname = hostnamePort[0];
                int port = Integer.parseInt(hostnamePort[1]);
                int responseTime = Integer.parseInt(data[3]);

                if(!clientName.equals(dronename)){ // Skip this ping if the currently read client corresponds to this client
                    responseTime = pingClient(hostname, port);
                }

                String output = "" + clientName + "," + clientType + "," + hostname + ":" + port + "," + responseTime + System.lineSeparator();
                System.out.println(output);
                newCSVContents.append(output);

            }
            csvReader.close();

            // Overwrite the CSV with new data
            FileWriter csvWriter = new FileWriter(csvName);
            csvWriter.append(newCSVContents.toString());
            csvWriter.flush();
            csvWriter.close();

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }



    }
}
