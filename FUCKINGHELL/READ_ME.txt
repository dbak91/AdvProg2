Setup
------
Database is expected in the root directory (or created there on initial run) with name 'flights_data.db'

CSV data is expected in 'data/' folder with the name 'flights_sample_3m.csv' i.e 'data/flights_sample_3m.csv' from root. 

Running
--------
There are two ways to run the app:

1: Import project and run 'RunMain' as Java app
2: Use the JAR in JAR folder to launch precompiled version(not tehcniically compiled, obj code). 
   This can be achieved through Windows Explorer double-click or typical CMD launch.  

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
               Ensure search term is generic enough for quickly grouping result that hit the limit returned (Flights 50, Airline 18,AirportF 10. 

4.             
Problem:       Loading Airport Analysis is too slow
Reason:        Multiple handles to DB. 
Circumvention: Ensure there is no connections or file handles on the db (windows or prior run) e.g 'taskkill /F /IM javaw.exe' or 'java.exe'. 
               Close background processes and ensure system is efficiently running. 
5.
Problem:       User wants to cycle backwards and display last page
Reason:        None, just unnecessary development.
Circumvention: User should note the current sorting column and click the header to reverse the sort order, then forward pages will effectively be backwards. 
