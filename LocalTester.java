public class LocalTester {


public static void
main (String[] args)
{
    HttpResponse response;

//hello world
    System.out.println("hello world: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                        "GET", "/hello", null, 
                                        null);
    System.out.println("8080: " + response.getResponseBody());
//paxos
    System.out.println("paxos: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                        "GET", "/paxos", null, 
                                        null);
    System.out.println("8080: " + response.getResponseCode() + ", " + response.getResponseBody());
}


}
