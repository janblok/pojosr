PojoSR Example
=======

This example uses Pojosr to load bundles from lib directory inside the war and starts them.
As a result, it shows the Apache Felix System Console working.
This is useful for running OSGi bundles from a .war file on GAE and AWS Beanstalk.

To extend this example just add your bundles as dependencies in the pom file. 

Startup
=======

Run with:  
mvn jetty:run

Open a browser to:
http://localhost:8080/system/console/  

For login credentials default values see:  
http://felix.apache.org/documentation/subprojects/apache-felix-web-console.html

