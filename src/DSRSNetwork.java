import java.io.*;
import java.net.*;

public class DSRSNetwork {

    private static final String dronename = "Relay1";

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

        try{

            String row;
            BufferedReader csvReader = new BufferedReader(new FileReader("./clients-" + dronename + ".csv"));
            while ((row = csvReader.readLine()) != null) {

                // Read the data from the .csv file
                String[] data = row.split(",");
                // Parse the data, assigning to variables
                String clientName = data[0];
                String clientType = data[1];
                String[] hostnamePort = data[2].split(":");
                String hostname = hostnamePort[0];
                int port = Integer.parseInt(hostnamePort[1]);
                int lastResponseTime = Integer.parseInt(data[3]);

                int newResponseTime = pingClient(hostname, port);

                System.out.println("" + clientName + " " + port + " " + newResponseTime);

            }
            csvReader.close();

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }



    }
}
