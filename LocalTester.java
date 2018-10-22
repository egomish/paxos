public class LocalTester {


public static void
main (String[] args)
{
    HttpResponse response;

//hello world
    response = ClientRequest.sendRequest("localhost:8080",
                                        "GET", "/hello", null, 
                                        null);
    System.out.println("8080: " + response.getResponseBody());

//put new key "foo"="bart"
    response = ClientRequest.sendRequest("localhost:8080",
                                         "PUT", "/kvs", null, 
                                         "{'value':'bart', 'key':'foo'}");
    System.out.println("8080: " + response.getResponseBody());

//get key "foo"
    response = ClientRequest.sendRequest("localhost:8080",
                                         "GET", "/kvs", "key=foo", 
                                         null);
    System.out.println("8080: " + response.getResponseBody());

//get non-existent key "faa"
    response = ClientRequest.sendRequest("localhost:8080",
                                         "GET", "/kvs", "key=faa", 
                                         null);
    System.out.println("8080: " + response.getResponseBody());

}


}
