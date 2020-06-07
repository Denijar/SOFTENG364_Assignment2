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
    // Cost table
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
        int index = 0;
        nameToIndex.put(_thisDroneName, index);
        costTable[0][0] = 0;
        for(Client client : clientList){
            index++;
            nameToClientObject.put(client.getClientName(), client);
            nameToIndex.put(client.getClientName(), index);
            costTable[0][index] = client.getResponseTime();
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

        for(int i = 0; i < index + 1; i++){
            for(int j = 0; j < index + 1; j++){
                System.out.print(costTable[i][j]);
            }
            System.out.println("");
        }
    }

    public void processUpdate(String headerOriginatingDrone, String info){
        // Add the new value to the table

        // Calculate the new min values

        // Send the updates, if necessary

    }
}
