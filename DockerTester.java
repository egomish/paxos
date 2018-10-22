public class DockerTester {


public static void
main (String[] args)
{
    HttpResponse response;

//on primary, hello world
    response = ClientRequest.sendRequest("localhost:4000",
                                        "GET", "/hello", null, 
                                        null);
    System.out.println("4000: " + response.getResponseBody());

//on primary, put new key "foo"="bart"
    response = ClientRequest.sendRequest("localhost:4000",
                                         "PUT", "/kvs", null, 
                                         "{'value':'bart', 'key':'foo'}");
    System.out.println("4000: " + response.getResponseBody());

//on primary, get key "foo"
    response = ClientRequest.sendRequest("localhost:4000",
                                         "GET", "/kvs", "key=foo", 
                                         null);
    System.out.println("4000: " + response.getResponseBody());

//on primary, get non-existent key "faa"
    response = ClientRequest.sendRequest("localhost:4000",
                                         "GET", "/kvs", "key=faa", 
                                         null);
    System.out.println("4000: " + response.getResponseBody());
}


}
