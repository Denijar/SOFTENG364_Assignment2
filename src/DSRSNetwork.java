import java.io.*;
import java.net.*;

public class DSRSNetwork {

    public static void main(String[] args) {

        // Currently, hard code in the data that we will later read from csv
        String clientName = "Relay2";
        String clientType = "Relay";
        String hostname = "127.0.0.1";
        int port = 10128;
        // int currentResponseTime = 6;

        // Ping a neighbour
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

            long timeTaken = endTime - startTime;

            // Print out the message
            System.out.println(msg);
            System.out.println(timeTaken);

        } catch (IOException e){
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }



    }
}
