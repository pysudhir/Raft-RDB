1. clone this repo
2. resolve vertex dependencies
3. go to src/
4. To compile and run: vertx run -cp ../lib/snakeyaml-1.10.jar:../lib/voldemort-0.96.jar:../lib/jdom-1.1.jar:../lib/g4j-1.2.17.jar: MasterDriver.java
5. Similarly run Server1 and Server2
6. Make sure all the details in configuration.yaml file are correct
7. Go to the browser and enter the url in the following format:
    http://localhost:8080/put?key=value
8. To compile the client: javac -cp lib/commons-logging-1.2.jar:lib/core-0.1.4.jar:lib/httpclient-4.5.2.jar:lib/httpcore-4.4.4.jar:lib/json-20140107.jar: YcsbClient.java
9. To run the client: java -cp lib/commons-logging-1.2.jar:lib/core-0.1.4.jar:lib/httpclient-4.5.2.jar:lib/httpcore-4.4.4.jar:lib/json-20140107.jar: com.yahoo.ycsb.Client -t -db YcsbClient -P ../workloads/workloadd
