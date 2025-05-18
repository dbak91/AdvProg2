Build Version: v1.0-alpha

Setup
------
Database is created and then expected in the root directory with name 'flights_data.db'

CSV data is expected in 'data/' folder with the name 'flights_sample_3m.csv' i.e. 'data/flights_sample_3m.csv' from root. 

Running
--------
There are two different ways to run the app:

1: Import project and run 'FlightAnalyserApp' as Java app configuration. Note: External JARs will need to be added for JDBC & JFreeChart. "resources" folder must be added as class folder to the build path in order to enable the Java(getResources()) to find createTable.sql file on compilation.  

2: Use the FlightAnalyserApp.jar in JAR_RUNNABLE folder to launch precompiled version (not technically compiled, obj code .zip). 
   This can be achieved through Windows Explorer double-click or typical CMD launch.  '>javaw ./<dir><file>'

Links
-----
Source control: https://github.com/dbak91/AdvProg2/

Troubleshooting:
-----------------
1.
Problem:       No display on launch 
Reason:        Splash screen is a JDialog, eclipse launch can go funny and may not be brought to front of view.
Circumvention: Minimize all windows(bottom rhs), bring eclipse back by selecting it in taskbar, then minimise eclipse. 
               This should leave the splash screen still visible. 

2.             
Problem:       No data added from csv
Reason:        No such file found exception, or unexpected format
Circumvention: Ensure csv is named and in correct location(see setup)
               Ensure csv matches expected format. (see Usage_Help.txt for headers and data order)

3.             
Problem:       Search is very slow
Reason:        If number of matches doesn't quickly reach LIMIT field the search must traverse all data
Circumvention: If expecting a single entry search will be slow. 
               Ensure search term is generic enough for quickly grouping results that hit the limit returned and then sort by convenient headers(Page Sizes: Flights 50, Airline 18, AirportFull 10). 

4.             
Problem:       Loading Airport Analysis is too slow
Reason:        Multiple handles to DataBase
Circumvention: Ensure there is no connections or file handles on the databse (windows or prior run) e.g. 'taskkill /F /IM javaw.exe' or 'java.exe'. 
               Close background processes and ensure system is efficiently running. Check OneDrive as always!
6. 
Problem:       No data is loaded into the table after trying to sort or search. 
Reason:        Likely there was no match, in this case the lading would taken a while. If results are instant it means there was a problem examining the database.       
Circumvention: Confirm correct table import and examine console logs, ignoring any alpha diagnostic badly-typed messages. 

5.
Problem:       User wants to cycle backwards and display last page
Reason:        None, just unnecessary development.
Circumvention: User should note the current sorting column in effect and click the header to reverse the sort order, then forwarding pages will effectively be backwards as Desired. 

Expected enhancements and bugs:
------------------------------------
	- Delay Reason Pie date search function
	- Total results found and page "out of" display i.e. page 4 of 25. (if performance permits)
	- Improved and cleaned diagnostic messages (Log4j?). 
	- Improved JDoc
	- More DNR
	- Scatter diagrams and bar charts
	- Improved index use - i.e. wildcard and 'LIKE' prevents index use, this search type is not necessary for all searches e.g. iata code could be "EQ"
	- Stop page number overflowing/continuing past result set. 
	- Trends , e.g. departure delay causing arrival delay