PojoSR
=======

A service registry that enables OSGi style service registry programs without using an OSGi framework. 
The idea is to create something that would make the service and parts of the life cycle layer of OSGi available in environments where it typically isn't. 

It's a service registry that you can use from normal java (including access to a bundle context so you can continue using e.g., ServiceTracker) and 
a framework that installs and starts bundles found on the classpath and still provides global visibility to the classes. However, it is not a full OSGi framework and doesn't provide all of the features of one. 

It works rather well with well-designed bundles including getting dependencymanager, scr and ipojo based services to work. 
People used it on the Google App Engine (GAE), Amazon Elastic Beanstalk, and inside JEE containers (it has a mode to run without creating threads and 
doesn't create a bundle cache -- hence, works on the GAE alas, a lot of bundles don't because they in turn create threads). 

Finally, this is not intended to be a replacement for OSGi but rather as a way to enable bundles in environments where they otherwise couldn't run and as a start to OSGi development. 


Please contribute with pull requests! 

Usage
========

See the *framework* module readme for details.


Examples
========

See the *example* module readme for details, it produces a working war using PojoSR and showing Felix Webconsole.


What's not supported
========

Using multiple bundles versions at the same time, since PojoRS works without classloaders all class versions will mixup in the same classloader.


Getting It
========

Dependencies are available from Maven Central:

```xml
<dependency>
	<groupId>com.spectray</groupId>
	<artifactId>com.spectray.pojosr.framework</artifactId>
	<version>0.4.0-SNAPSHOT</version>
</dependency>
```


## License
```
This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
