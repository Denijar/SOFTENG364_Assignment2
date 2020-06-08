import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostTable {

    private String _thisDroneName;
    private List<Client> _clientList;
    // Map: client name to the client object (this will not include an object for this drone)
    private Map<String, Client> nameToClientObject = new HashMap<>();
    // Map: client name to their index in the cost table (this drone will be indexed at 0)
    private Map<String, Integer> nameToIndex = new HashMap<>();
    // Map: store of forwarding table data. Maps destination node to the best choice node for forwarding. No entry means it is unreachable
    private Map<String, String> destinationToForwarder = new HashMap<>();
    // Map of neighbours and the cost to get to that neighbour
    private Map<String, Integer> neighboursCost = new HashMap<>();
    // Cost table - ROW is FROM, COLUMN is DESTINATION
    private int[][] costTable;

    public CostTable(String thisDroneName, List<Client> clientList){
        _thisDroneName = thisDroneName;
        _clientList = clientList;

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
        costTable = new int[clientList.size() + 1][clientList.size() + 1];

        // Initialise the hash maps and first row of cost table

        // Initialise for this drone
        int index = 0;
        nameToIndex.put(_thisDroneName, index);
        costTable[0][0] = 0;

        // Initialise for the rest of the clients
        for(Client client : clientList){
            index++;
            nameToClientObject.put(client.getClientName(), client);
            nameToIndex.put(client.getClientName(), index);
            costTable[0][index] = client.getResponseTime();
            if(client.getResponseTime() != -1){
                neighboursCost.put(client.getClientName(), client.getResponseTime());
                // Neighbours can be reached directly
                destinationToForwarder.put(client.getClientName(), client.getClientName());
            }
        }

        // Initialise rest of the cost table
        for(int i = 1; i < index + 1; i++){
            for(int j = 0; j < index + 1; j++){
                if(i == j){
                    costTable[i][j] = 0;
                } else {
                    costTable[i][j] = -1;
                }
            }
        }

        // FIXME: remove this print
        for(int i = 0; i < index + 1; i++){
            for(int j = 0; j < index + 1; j++){
                System.out.print(costTable[i][j]);
            }
            System.out.println("");
        }

        createForwardingTable();
    }

    public void processUpdates(String headerOriginatingDrone, String updates){
        String[] splitUpdates = updates.split(",");

        // Add the new cost to the table for each update
        for(int i = 0; i < splitUpdates.length; i++){
            String[] destinationAndCost = splitUpdates[i].split("=");
            String destinationDrone = destinationAndCost[0];
            int cost = Integer.parseInt(destinationAndCost[1]);

            costTable[nameToIndex.get(headerOriginatingDrone)][nameToIndex.get(destinationDrone)] = cost;
        }

        // TODO: Where to call?
        createForwardingTable();
    }

    private void createForwardingTable(){

        // Updates to send out
        List<String> updatesToSend = new ArrayList<>();

        // Iterate through each possible destination
        for(String destination : nameToIndex.keySet()){
            // Skip me
            if(destination.equals(_thisDroneName)){
                continue;
            }

            System.out.println(destination);

            int previousCost = costTable[0][nameToIndex.get(destination)];
            int newMin = -1;
            // Keep track of the neighbour that caused the new min to be generated
            String neighbourCausingChange = "";

            // Iterate through neighbours, finding cost of reaching destination from each neighbour
            for(String neighbour : neighboursCost.keySet()){
                int costFromMeToNeighbour = neighboursCost.get(neighbour); // This should never equal -1, but check for consistency
                int costFromNeighbourToDestination = costTable[nameToIndex.get(neighbour)][nameToIndex.get(destination)];
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
            costTable[0][nameToIndex.get(destination)] = newMin;

            System.out.println(destinationToForwarder);
            System.out.println(destination);

            if(newMin == -1){ // This destination is unreachable
                if(!(previousCost == -1)){ // It was previously reachable, update
                    destinationToForwarder.remove(destination);
                    updatesToSend.add(destination);
                } else {
                    continue; // Skip past this node entirely - nothing has changed
                }
            } else { // This destination is reachable
                if(newMin == previousCost && destinationToForwarder.get(destination).equals(neighbourCausingChange)){ // The only case to not update is if the cost and best choice neighbour has not changed
                    continue; // Skip past this destination node entirely - nothing has changed
                } else {
                    destinationToForwarder.put(destination, neighbourCausingChange);
                    updatesToSend.add(destination);
                }
            }
        }

        // FIXME: remove these prints print
        // Hypothesis - if we run this the first time, this table should be empty. All minimum costs should be the same
        for(int i = 0; i < costTable.length; i++){
            for(int j = 0; j < costTable.length; j++){
                System.out.print(costTable[i][j]);
            }
            System.out.println("");
        }
        System.out.println(neighboursCost);
        System.out.println(updatesToSend.size());
        System.out.println(updatesToSend);
        System.out.println(destinationToForwarder);

        // IDEA: create a new minimum from scratch. Start minimum at -1
        // Iterate through the costs per each neighbour.
        // For each, check if the current min is -1. If it is, then replace. Or, if the current is less than the min, replace
        // If we replace, record the neighbour drone that caused the change to take place
        // When done, update the cost for reaching the destination node (in the first row), and update the destinationToFowarder drone (either overriding or updating)
        //      If this change actually changed the forwarder node, then record this as needed for an update send (check what info is needed for DV update)
        // If the current minimum is still -1, then remove the entry from the map completely.

        // Account for: updates meaning that a neighbour node is no longer a neighbour node anymore (see Piazza posts for if this is possible).

        // When done, can just completely make the forwarding table from our map.

        // Check, can this exact code be run right from the beginning??


    }
}
