public class LocalTester {


public static void
main (String[] args)
{
    HttpResponse response;

//put bad key "foo"=null
    System.out.println("foo=NULL: ");
    String service = "/keyValue-store/";
    System.out.println("service: " + service);
    response = ClientRequest.sendRequest("localhost:8080",
                                         "PUT", service, null, 
                                         "{'val':'no key'}");
    System.out.println("8080: " + response.getResponseBody());

/*
//hello world
    System.out.println("hello world: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                        "GET", "/hello", null, 
                                        null);
    System.out.println("8080: " + response.getResponseBody());

//put new key "foo"="bart"
    System.out.println("foo=bart: ");
    String service = "/keyValue-store/foo";
    System.out.println("service: " + service);
    response = ClientRequest.sendRequest("localhost:8080",
                                         "PUT", service, null, 
                                         "{'val':'bart'}");
    System.out.println("8080: " + response.getResponseBody());

//put new key "subject"="Distributed System"
    System.out.println("subject=Distributed System: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                         "PUT", "/keyValue-store/subject", 
                                         null,
                                         "{'val': 'Distributed System'}");
    System.out.println("8080: " + response.getResponseBody());

//get key "subject"
    System.out.println("subject=?: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                         "GET", "/keyValue-store/subject", 
                                         null,
                                         null);
    System.out.println("8080: " + response.getResponseBody());

//check for key "foo"
    System.out.println("foo?: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                         "GET", "/keyValue-store/search/foo", null,
                                         null);
    System.out.println("8080: " + response.getResponseBody());

//get key "foo"
    System.out.println("foo=?: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                         "GET", "/keyValue-store/foo", null,
                                         null);
    System.out.println("8080: " + response.getResponseBody());

//get non-existent key "faa"
    System.out.println("faa=?: ");
    response = ClientRequest.sendRequest("localhost:8080",
                                         "GET", "/keyValue-store/faa", null,
                                         null);
    System.out.println("8080: " + response.getResponseBody());
*/

}


}
