public class Client {

    private String _clientName;
    private String _clientType;
    private String _hostname;
    private int _port;
    private int _responseTime;

    public Client(String[] data){
        // Parse the data, assigning to fields
        _clientName = data[0];
        _clientType = data[1];
        String[] hostnamePort = data[2].split(":");
        _hostname = hostnamePort[0];
        _port = Integer.parseInt(hostnamePort[1]);
        _responseTime = Integer.parseInt(data[3]);
    }

    public void setResponseTime(int responseTime){
        _responseTime = responseTime;
    }

    public String getClientName(){
        return _clientName;
    }

    public String getHostname(){
        return _hostname;
    }

    public int getPort(){
        return _port;
    }

    public int getResponseTime(){
        return _responseTime;
    }

    @Override
    public String toString(){
        String output = "" + _clientName + "," + _clientType + "," + _hostname + ":" + _port + "," + _responseTime + System.lineSeparator();
        return output;
    }

}
