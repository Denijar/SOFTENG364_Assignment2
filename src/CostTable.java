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
    // List of neighbours' names to this drone (ones that were found to have a non -1 distance from the initial pings)
    List<String> neighbours = new ArrayList<>();
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
        // Record the list of neighbours by name
        int index = 0;
        nameToIndex.put(_thisDroneName, index);
        costTable[0][0] = 0;
        for(Client client : clientList){
            index++;
            nameToClientObject.put(client.getClientName(), client);
            nameToIndex.put(client.getClientName(), index);
            costTable[0][index] = client.getResponseTime();
            if(client.getResponseTime() != -1){
                neighbours.add(client.getClientName());
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

        // Add the new value to the table

        // Calculate the new min values

        // Send the updates, if necessary

    }

    // This method assumes that the forwarding table has already been made
    private void updateForwardingTable(){

        // Iterate through each possible destination
        for(String destination : nameToIndex.keySet()){
            int currentCost = costTable[0][nameToIndex.get(destination)]; // Note the current cost doesn't mean much. It might need to increase because we just found out we can't get there as efficiently as we used to
            // Iterate through neighbours, finding cost of reaching destination from each neighbour
            for(String neighbour : neighbours){
                int costFromMeToNeighbour = costTable[0][nameToIndex.get(neighbour)];
                int costFromNeighbourToDestination = costTable[nameToIndex.get(neighbour)][nameToIndex.get(destination)];
            }
        }

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
