public class Client {

    private String _clientName;
    private String _clientType;
    private String _hostname;
    private int _port;
    private int _responseTime;

    public Client(String row){
        // Parse the data, assigning to fields
        String[] data = row.split(",");
        _clientName = data[0];
        _clientType = data[1];
        String[] hostnamePort = data[2].split(":");
        _hostname = hostnamePort[0];
        _port = Integer.parseInt(hostnamePort[1]);
        _responseTime = Integer.parseInt(data[3]);
    }

    void setResponseTime(int responseTime){
        _responseTime = responseTime;
    }

    String getClientName(){
        return _clientName;
    }

    String getHostname(){
        return _hostname;
    }

    int getPort(){
        return _port;
    }

    int getResponseTime(){
        return _responseTime;
    }

    @Override
    public String toString(){
        return "" + _clientName + "," + _clientType + "," + _hostname + ":" + _port + "," + _responseTime + System.lineSeparator();
    }

}
