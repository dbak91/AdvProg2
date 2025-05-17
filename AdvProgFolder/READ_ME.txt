Build Version: v1.0

Setup
------
Database is expected in the root directory (or created there on initial run) with name 'flights_data.db'

CSV data is expected in 'data/' folder with the name 'flights_sample_3m.csv' i.e. 'data/flights_sample_3m.csv' from root. 

Running
--------
There are two different ways to run the app:

1: Import project and run 'FlightAnalyserApp' as Java app configuration. Note: External JARs will need to be added for JDBC & JFreeChart. "resources" folder must be added as class folder to build path in order to enable getResources() to find createTable.sql on compilation. Eclipse Project and git settings were not exported, source only. 

2: Use the FlyghtAnalyserApp.jar in JAR_RUNNABLE folder to launch precompiled version (not technically compiled, obj code). 
   This can be achieved through Windows Explorer double-click or typical CMD launch.  "javaw ./<dir><file>"

Links
-----
Source control: https://github.com/dbak91/AdvProg2/tree/master/AdvProgFolder

Troubleshooting:
-----------------
1.
Problem:       No display on launch 
Reason:        Splash screen is a JDialog, eclipse launch can go funny and may not be brought to front of view.
Circumvention: Minimize all windows(bottom rhs), bring eclipse back by selecting in taskbar, then minimise eclipse. 
               This should leave the splash screen still visible

2.             
Problem:       No data added from csv
Reason:        ????,or no such file found exception
Circumvention: Ensure csv is named and in correct location(see setup)
               Ensure csv matches expected format. (see Usage_Help.txt for headers and data order)

3.             
Problem:       Search is very slow
Reason:        If number of matches doesn't quickly reach LIMIT field the search must traverse all data
Circumvention: If expecting single entry search will be slow. 
               Ensure search term is generic enough for quickly grouping result that hit the limit returned and sort by convenient headers(Page Size: Flights 50, Airline 18,AirportF 10. 

4.             
Problem:       Loading Airport Analysis is too slow
Reason:        Multiple handles to DB. 
Circumvention: Ensure there is no connections or file handles on the db (windows or prior run) e.g. 'taskkill /F /IM javaw.exe' or 'java.exe'. 
               Close background processes and ensure system is efficiently running. Check OneDrive as always!
5.
Problem:       User wants to cycle backwards and display last page
Reason:        None, just unnecessary development.
Circumvention: User should note the current sorting column in effect and click the header to reverse the sort order, then forwarding pages will effectively be backwards as desired. 
