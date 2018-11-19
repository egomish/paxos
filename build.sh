rm *.class
javac -cp gson-2.8.5.jar:. SmallServer.java \
                           POJO.java \
                           \
                           Client.java \
                           POJOReqHttp.java POJOResHttp.java \
                           \
                           ContextHello.java ContextTest.java \
                           \
                           ContextKVS.java \
                           POJOReqBody.java POJOResBody.java \
                           \
                           ContextPaxosProposer.java ContextPaxosAcceptor.java \
                           POJOPaxosBody.java \
                           PaxosResponse.java \
                           \
                           ContextHistory.java \
                           POJOHistory.java \
                           \

