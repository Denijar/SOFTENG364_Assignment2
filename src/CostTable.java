import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostTable {

    private String _thisDroneName;
    private String _forwardingTableFileName;
    // Map: client name to the client object (this will not include an object for this drone)
    private Map<String, Client> _nameToClientObject = new HashMap<>();
    // Map: client name to their index in the cost table (this drone will be indexed at 0)
    private Map<String, Integer> _nameToIndex = new HashMap<>();
    // Map: store of forwarding table data. Maps destination node to the best choice node for forwarding. No entry means it is unreachable
    private Map<String, String> _destinationToForwarder = new HashMap<>();
    // Map of neighbours and the cost to get to that neighbour
    private Map<String, Integer> _neighboursCost = new HashMap<>();
    // Cost table - ROW is FROM, COLUMN is DESTINATION
    private int[][] _costTable;

    public CostTable(String thisDroneName, List<Client> clientList){
        _thisDroneName = thisDroneName;
        _forwardingTableFileName = "./forwarding-" + _thisDroneName + ".csv";

        // Remove this drone if it appears in the client list
        // We want to control the fact that this drone appears at index 0 of the cost table, so easiest to just remove it
        List<Client> found = new ArrayList<>();
        for(Client client : clientList){
            if(client.getClientName().equals(_thisDroneName)){
                found.add(client);
            }
        }
        clientList.removeAll(found);

        // Instantiate cost table (+1 because we will put this client at index 0)
        _costTable = new int[clientList.size() + 1][clientList.size() + 1];

        // Initialise the hash maps and first row of cost table

        // Initialise for this drone
        int index = 0;
        _nameToIndex.put(_thisDroneName, index);
        _costTable[0][0] = 0;

        // Initialise for the rest of the clients
        for(Client client : clientList){
            index++;
            _nameToClientObject.put(client.getClientName(), client);
            _nameToIndex.put(client.getClientName(), index);
            _costTable[0][index] = client.getResponseTime();
            if(client.getResponseTime() != -1){
                _neighboursCost.put(client.getClientName(), client.getResponseTime());
                // Neighbours can be reached directly
                _destinationToForwarder.put(client.getClientName(), client.getClientName());
            }
        }

        // Initialise rest of the cost table
        for(int i = 1; i < index + 1; i++){
            for(int j = 0; j < index + 1; j++){
                if(i == j){
                    _costTable[i][j] = 0;
                } else {
                    _costTable[i][j] = -1;
                }
            }
        }
        createForwardingTable();
    }

    public void processUpdates(String headerOriginatingDrone, String updates){
        String[] splitUpdates = updates.split(",");

        // Add the new cost to the table for each update
        for (String splitUpdate : splitUpdates) {
            String[] destinationAndCost = splitUpdate.split("=");
            String destinationDrone = destinationAndCost[0];
            int cost = Integer.parseInt(destinationAndCost[1]);

            _costTable[_nameToIndex.get(headerOriginatingDrone)][_nameToIndex.get(destinationDrone)] = cost;
        }
        updateCostTable();
        createForwardingTable();
    }

    private void updateCostTable(){

        // Updates to send out
        List<String> updatesToSend = new ArrayList<>();

        // Iterate through each possible destination
        for(String destination : _nameToIndex.keySet()){
            // Skip me
            if(destination.equals(_thisDroneName)){
                continue;
            }

            System.out.print("- Calculating cost for " + destination + "...");

            int previousCost = _costTable[0][_nameToIndex.get(destination)];
            int newMin = -1;
            // Keep track of the neighbour that caused the new min to be generated
            String neighbourCausingChange = "";

            // Iterate through neighbours, finding cost of reaching destination from each neighbour
            for(String neighbour : _neighboursCost.keySet()){
                int costFromMeToNeighbour = _neighboursCost.get(neighbour); // This should never equal -1, but check for consistency
                int costFromNeighbourToDestination = _costTable[_nameToIndex.get(neighbour)][_nameToIndex.get(destination)];
                // Find the min cost. Remember any appearance of -1 means this connection should not be considered
                if(!(costFromMeToNeighbour == -1 || costFromNeighbourToDestination == -1)){
                    int thisCost = costFromMeToNeighbour + costFromNeighbourToDestination;
                    if(newMin == -1 || thisCost < newMin){
                        newMin = thisCost;
                        neighbourCausingChange = neighbour;
                    }
                }
            }

            // No matter what, we are updating the table
            _costTable[0][_nameToIndex.get(destination)] = newMin;

            if(newMin == -1){ // This destination is unreachable
                if(!(previousCost == -1)){ // It was previously reachable, update
                    _destinationToForwarder.remove(destination);
                    updatesToSend.add(destination);
                    System.out.println("cost updated to -1");
                } else { // Skip past this node entirely - nothing has changed
                    System.out.println("no change");
                }

            } else { // This destination is reachable
                if(!(newMin == previousCost && _destinationToForwarder.get(destination).equals(neighbourCausingChange))){ // The only case to not update is if the cost and best choice neighbour has not changed
                    _destinationToForwarder.put(destination, neighbourCausingChange);
                    updatesToSend.add(destination);
                    System.out.println("cost updated to " + newMin + " via " + neighbourCausingChange);
                } else {
                    System.out.println("no change");

                }
            }
        }

        sendUpdates(updatesToSend);
    }

    private void createForwardingTable(){
        // Create the new forwarding table
        try {
            FileWriter csvWriter = new FileWriter(_forwardingTableFileName);
            for(String destination : _destinationToForwarder.keySet()){
                csvWriter.append(destination + "," + _destinationToForwarder.get(destination) + System.lineSeparator());
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            System.err.format("Something went wrong: '%s'%n", e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendUpdates(List<String> updatesToSend){
        if(updatesToSend.isEmpty()){
            System.out.println("Skipping DV update send");
            return;
        }
        System.out.println("Sending updated DVs");
        // Construct message to send
        StringBuilder updateMessage = new StringBuilder("UPDATE:" + _thisDroneName + ":");
        for(int i = 0; i < updatesToSend.size() - 1; i++){
            updateMessage.append(updatesToSend.get(i) + "=" + _costTable[0][_nameToIndex.get(updatesToSend.get(i))] + ",");
        }
        updateMessage.append(updatesToSend.get(updatesToSend.size() - 1) + "=" + _costTable[0][_nameToIndex.get(updatesToSend.get(updatesToSend.size() - 1))]);
        updateMessage.append(":" + updatesToSend.size() + "\n");
        // Send an update to each neighbour regarding these new costs
        for(String neighbour : _neighboursCost.keySet()){
            try{
                System.out.print("Sending to " + neighbour + "...");
                Client client = _nameToClientObject.get(neighbour);
                Socket socket = new Socket(client.getHostname(), client.getPort());
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                dataOut.writeUTF(updateMessage.toString());
                // Wait for the client's acknowledgement
                dataIn.readUTF();
                dataOut.flush();
                dataOut.close();
                dataIn.close();
                socket.close();
                System.out.println("done");
            } catch (IOException e){
                System.err.format("Something went wrong: '%s'%n", e.getMessage());
                e.printStackTrace();
            }


        }


    }
}
