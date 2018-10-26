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

//on nonprimary, hello world
    response = ClientRequest.sendRequest("localhost:4001",
                                        "GET", "/hello", null, 
                                        null);
    System.out.println("4001: " + response.getResponseBody());

//on primary, put new key "key"="value"
    response = ClientRequest.sendRequest("localhost:4000",
                                         "PUT", "/keyValue-store/key", null, 
                                         "{'val':'value'}");
    System.out.println("4000: " + response.getResponseBody());

//on nonprimary, put new key "foo"="Distributed System"
    response = ClientRequest.sendRequest("localhost:4001",
                                         "PUT", "/keyValue-store/foo", null, 
                                         "{'val':'Distributed System'}");
    System.out.println("4001: " + response.getResponseBody());

//on primary, get key "foo"
    response = ClientRequest.sendRequest("localhost:4000",
                                         "GET", "/keyValue-store/foo", null, 
                                         null);
    System.out.println("4000: " + response.getResponseBody());

//on nonprimary, get key "foo"
    response = ClientRequest.sendRequest("localhost:4001",
                                         "GET", "/keyValue-store/foo", null, 
                                         null);
    System.out.println("4001: " + response.getResponseBody());

//on primary, get non-existent key "faa"
    response = ClientRequest.sendRequest("localhost:4000",
                                         "GET", "/keyValue-store/faa", null, 
                                         null);
    System.out.println("4000: " + response.getResponseBody());

//on nonprimary, get non-existent key "faa"
    response = ClientRequest.sendRequest("localhost:4001",
                                         "GET", "/keyValue-store/faa", null, 
                                         null);
    System.out.println("4001: " + response.getResponseBody());

//on primary, check if key "key" exists
    response = ClientRequest.sendRequest("localhost:4000",
                                         "GET", "/keyValue-store/search/key", null, 
                                         null);
    System.out.println("4000: " + response.getResponseBody());

    try {
        System.out.println("sleeping for 10 seconds...");
        Thread.sleep(10 * 1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }

//on nonprimary, check if non-existent key "faa" exists
    response = ClientRequest.sendRequest("localhost:4001",
                                         "GET", "/keyValue-store/search/faa", null, 
                                         null);
    System.out.println("4001: " + response.getResponseBody());
}


}
