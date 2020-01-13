Lift management simulation

This application is designed for dynamic quantity of floors and lifts

Prerequisites:
Java 11, Apache Maven 3.x+

Project build

As this is a maven project, to build it run: mvn clean install;
after execution artifact lift-management-1.0-SNAPSHOT.jar will be created.
Tests is the subject for future development.

Project run

Built artifact that can be run from console as simple as
cd <projectDir>/target && java -jar lift-management-1.0-SNAPSHOT.jar
or as this is SpringBoot implementation you can run this project
from your favourite IDE via app.Runner.java class

After application start (8080 default port, can be set in application.properties though) to initiate lift request use POST request:
Example:
Path: http://localhost:8080/lift-requests
Payload: {"floorNumber":6,"direction":"UP"}

the progress can be tracked in the console.
To simulate pressing the buttons by the users who got into the lift 0-2 random buttons are pressed on each floor
that was requested.
Priority is always given to the closest lift with lowest tonnage.


Note:
This is rather conceptual implementation that needs test coverage, maybe some corner cases tuning, exception handling enhancement,
DB usage for state management, etc.
This is rather to give you shallow impression of my coding style.

Enjoy :)