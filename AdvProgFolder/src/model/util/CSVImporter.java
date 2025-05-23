package model.util;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JLabel;

import model.DatabaseManager;
import model.sql.InsertStatements;
/**
 * Dedicated to importing of CSV function only
 *
 *
 * <p>Suggested enhancement: use Map for fields and indexes instead of manual if branches.
 * Don't want to break. Wasn't comfortable with Maps at early stage (used in later stages)</p>
 * 
 * @author 23751662
 *
 */

public class CSVImporter
{
	private static int	FL_DATE_ind;
	private static int	AIRLINE_ind;
	private static int	AIRLINE_CODE_ind;
	private static int	FL_NUMBER_ind;
	private static int	ORIGIN_ind;
	private static int	ORIGIN_CITY_ind;
	private static int	DEST_ind;
	private static int	DEST_CITY_ind;
	private static int	CRS_DEP_TIME_ind;
	private static int	DEP_TIME_ind;
	private static int	CRS_ARR_TIME_ind;
	private static int	ARR_TIME_ind;
	private static int	ARR_DELAY_ind;
	private static int	DELAY_DUE_CARRIER_ind;
	private static int	DELAY_DUE_WEATHER_ind;
	private static int	DELAY_DUE_NAS_ind;
	private static int	DELAY_DUE_SECURITY_ind;
	private static int	DELAY_DUE_LATE_AIRCRAFT_ind;
	private static long rowsSkipped = 0;
	private static StringBuilder skippedDiags = new StringBuilder();
	
	private static int	BATCH_SIZE	= 100000;
	
	

	/**
	 * Gets a connection to the database and csv input file, scans the input file line by line
	 * and prepares insert statements in batches to execute on the db.
	 *
	 * @param loadingLabel any potential label to update with progress
	 * @throws Exception something went wrong with the db interaction
	 */
	public static void importCSV(JLabel loadingLabel) throws Exception
	{
		String csvDir = "data/flights_sample_3m.csv";
		Path csvPath = Paths.get(csvDir);

		try (BufferedReader reader = Files.newBufferedReader(csvPath);
				Connection conn = DatabaseManager.connect(false);
				PreparedStatement insertAirlineStmt = conn.prepareStatement(InsertStatements.insertAirlineSQL);
				PreparedStatement insertAirportStmt = conn.prepareStatement(InsertStatements.insertAirportSQL);
				PreparedStatement insertDelayStmnt = conn.prepareStatement(InsertStatements.insertDelaySQL);)
		{
			PreparedStatement insertFlightStmnt = conn.prepareStatement(InsertStatements.insertFlightSQL,
					Statement.RETURN_GENERATED_KEYS); //need access to gen keys

			// read header line (should be first) and assign indexes for mapping
			String[] header = reader.readLine().split(",", -1);
			assignIndexes(header);

			// turn off auto-commit for transactions (big data set)
			conn.setAutoCommit(false);
			long lineCount = Files.lines(csvPath).count();
			String line;
			int batchCount = 0;
			int indexReached = 1;

			// get time of start
			final long[] startMillis = {System.currentTimeMillis()};
			int eta = 0;
			/*
			 * for each line, get fields[matching_index] into the '?' params in insert sql statements
			 * then add to sql batch, at every batch size execute and commit to db(partial ready commit good in case user 
			 * want to cancel with*  atleast a subset of data to analyse and especially on big data sets
			 *
			 *  params must match order of ?'s!! 
			 */
			while ((line = reader.readLine()) != null)
			{
				//Timer()
				/*
				 * split by comma need to cater for comma inside text comma inside text
				 */
				String regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"; //

				String[] fields = line.split(regex, -1); // -1 for trailing empty values

				// set params

				insertAirlineStmt.setString(1, fields[AIRLINE_CODE_ind]);

				insertAirlineStmt.setString(2, fields[AIRLINE_ind]);
				insertAirlineStmt.addBatch();
				batchCount++;

				insertAirportStmt.setString(1, fields[ORIGIN_ind]);
				insertAirportStmt.setString(2, fields[ORIGIN_CITY_ind]);
				insertAirportStmt.addBatch();

				insertAirportStmt.setString(1, fields[DEST_ind]);
				insertAirportStmt.setString(2, fields[DEST_CITY_ind]);
				insertAirportStmt.addBatch();
				//
				insertFlightStmnt.setInt(1, indexReached);
				insertFlightStmnt.setString(2, fields[FL_DATE_ind].replace("-", "")); // Example: "2019-01-09"
				insertFlightStmnt.setString(3, fields[AIRLINE_CODE_ind]);
				insertFlightStmnt.setInt(4,
						fields[FL_NUMBER_ind].isEmpty() ? 0 : Integer.parseInt(fields[FL_NUMBER_ind]));
				insertFlightStmnt.setString(5, fields[ORIGIN_ind]);
				insertFlightStmnt.setString(6, fields[DEST_ind]);
				insertFlightStmnt.setInt(7,
						fields[CRS_DEP_TIME_ind].isEmpty() ? 0 : Integer.parseInt(fields[CRS_DEP_TIME_ind]));

				insertFlightStmnt.setInt(8,
						fields[DEP_TIME_ind].isEmpty() ? 0 : Integer.parseInt(fields[DEP_TIME_ind].split("\\.")[0]));
				insertFlightStmnt.setInt(9,
						fields[CRS_ARR_TIME_ind].isEmpty() ? 0 : Integer.parseInt(fields[CRS_ARR_TIME_ind]));
				insertFlightStmnt.setInt(10,
						fields[ARR_TIME_ind].isEmpty() ? 0 : Integer.parseInt(fields[ARR_TIME_ind].split("\\.")[0]));


				//  skip rows that are invalid
				if (fields[FL_NUMBER_ind].isEmpty() ||
						fields[FL_DATE_ind].isEmpty() ||
							fields[ORIGIN_ind].isEmpty() || 
								fields[DEP_TIME_ind].isEmpty() ||
									fields[ARR_TIME_ind].isEmpty())
				{
					String msg = "Not added "+fields[AIRLINE_ind] + " No. " + fields[FL_NUMBER_ind] + " missing critical fields";
					if(fields[DEP_TIME_ind].isEmpty())
					{
						msg += " departure time empty";
					}
					if(fields[ARR_TIME_ind].isEmpty())
					{
						msg += " arrival time empty";
					}
					// do not add, invalid data
					//System.err.println(msg);
					skippedDiags.append("\n" + msg);
					rowsSkipped++;
				}
				else //add batches and execute
				{
					insertFlightStmnt.addBatch();

					insertDelayStmnt.setInt(1, indexReached);
					String reason = "";
					int delay = 0;
					
					// this is shocking design, all just doing the same check. Use Maps!!!
					if (!fields[DELAY_DUE_CARRIER_ind].isEmpty())
					{
						if (Integer.parseInt(fields[DELAY_DUE_CARRIER_ind].split("\\.")[0]) != 0)
						{
							reason += "CARRIER ";
							// System.out.println(Integer.parseInt(fields[DELAY_DUE_CARRIER_ind].split("\\.")[0]));
							delay = +Integer.parseInt(fields[DELAY_DUE_CARRIER_ind].split("\\.")[0]);
						}
					}
					if (!fields[DELAY_DUE_WEATHER_ind].isEmpty())
					{
						if (Integer.parseInt(fields[DELAY_DUE_WEATHER_ind].split("\\.")[0]) != 0)
						{
							reason += "WEATHER ";
							delay = +Integer.parseInt(fields[DELAY_DUE_WEATHER_ind].split("\\.")[0]);
						}
					}
					if (!fields[DELAY_DUE_NAS_ind].isEmpty())
					{
						if (Integer.parseInt(fields[DELAY_DUE_NAS_ind].split("\\.")[0]) != 0)
						{
							reason += "AIR TRAFFIC  ";

							delay = +Integer.parseInt(fields[DELAY_DUE_NAS_ind].split("\\.")[0]);
						}
					}
					if (!fields[DELAY_DUE_SECURITY_ind].isEmpty())
					{
						if (Integer.parseInt(fields[DELAY_DUE_SECURITY_ind].split("\\.")[0]) != 0)
						{
							reason += "SECURITY ";

							delay = +Integer.parseInt(fields[DELAY_DUE_SECURITY_ind].split("\\.")[0]);
						}
					}

					if (!fields[DELAY_DUE_LATE_AIRCRAFT_ind].isEmpty())
					{
						if (Integer.parseInt(fields[DELAY_DUE_LATE_AIRCRAFT_ind].split("\\.")[0]) != 0)
						{
							reason += "LATE AIRCRAFT ";

						}
					}

					if (fields[ARR_DELAY_ind] != null && !fields[ARR_DELAY_ind].isEmpty())
					{
						delay = Integer.parseInt(fields[ARR_DELAY_ind].split("\\.")[0]);

					}
					else
					{
						delay = 0;
					}

					if (reason.isEmpty())
					{
						reason = "NONE";
					}

					insertDelayStmnt.setInt(1, indexReached);
					insertDelayStmnt.setString(2, reason);
					insertDelayStmnt.setInt(3, delay);

					insertDelayStmnt.addBatch();

					//
					if (batchCount >= BATCH_SIZE)
					{

						batchCount = 0;
						
						long elapsedMillis = System.currentTimeMillis() - startMillis[0];

						// this was a right faff with types and division
						if (elapsedMillis > 0) 
						{
							double elapsedSeconds = elapsedMillis / 1000.0;
							
							long linesRemaining = lineCount - indexReached;
							
							double linesPerSecond = indexReached / elapsedSeconds;
							eta =(int) linesPerSecond > 0 ?(int) linesRemaining / (int)linesPerSecond : 9999; // default If error or 0
							//diags
							//System.out.println("eta: " + eta + " seconds" + "linecount"+lineCount +" index"+indexReached+"seconds"+seconds[0] +"linesPersec"+linesPerSecond);
						}
						
						loadingLabel.setText(loadingLabel.getText().split("Import")[0] + "Import [lines: "  + indexReached + " of " + lineCount+"]" + " ETA: "+eta + "secs");
						
						// need to catch silent sql exception when csv has been partially updated
						//(i.e unique constraint violated for first rows already imported) so it does not propagate and break while loop
						// also useful for other batch processing errors.
						try {
							insertAirlineStmt.executeBatch();
							insertAirportStmt.executeBatch();
							insertFlightStmnt.executeBatch();

							insertDelayStmnt.executeBatch();


							conn.commit();
						} catch (SQLException e) {
							System.err.println("Batch insert error at index: " + indexReached);
							e.printStackTrace();
							conn.rollback(); // important: rollback on failure
							//    throw e; // or handle gracefully
						}

						// delayQueue=new ArrayList<>();
					} // batch reached
				}

				indexReached++;
			}
			if (batchCount > 0)
			{
				insertAirlineStmt.executeBatch();
				insertAirportStmt.executeBatch();
				insertFlightStmnt.executeBatch();
				insertDelayStmnt.executeBatch();
			}

			conn.commit();
			
			System.err.println(skippedDiags);
			
			loadingLabel.setText("CSV imported into database (rows skipped:"+rowsSkipped+")"
					+ ". Creating indexes");
			
			Thread.sleep(3000); // pause so user can see rows skipped.
			
			System.out.println("CSV imported into db, creating indexes");
			
			DatabaseManager dbManager = new DatabaseManager();
			dbManager.createIndexes();
			
			System.out.println("indexes finished");

		}
	}



	// -----------------------------------------------------
	// ------------Util-------------------------------------
	// -----------------------------------------------------


	/**
	 * Scans through an array and maps the static index variables to the corresponding displacement
	 *
	 * The indexes are used to retrieve elements from the parsed csv data (e.g. a,b,c,d will reuslt in a_ind=0,b_ind=1...
	 * 
	 * verbose
	 *
	 * @param header the csv header array
	 */
	private static void assignIndexes(String[] header)
	{
		for (int i = 0; i < header.length; i++)
		{
			switch (header[i])
			{
			case "FL_DATE":
				FL_DATE_ind = i;
				break;
			case "AIRLINE":
				AIRLINE_ind = i;
				break;
			case "AIRLINE_DOT":
				break;
			case "AIRLINE_CODE":
				AIRLINE_CODE_ind = i;
				break;
			case "DOT_CODE":
				break;
			case "FL_NUMBER":
				FL_NUMBER_ind = i;
				break;
			case "ORIGIN":
				ORIGIN_ind = i;
				break;
			case "ORIGIN_CITY":
				ORIGIN_CITY_ind = i;
				break;
			case "DEST":
				DEST_ind = i;
				break;
			case "DEST_CITY":
				DEST_CITY_ind = i;
				break;
			case "CRS_DEP_TIME":
				CRS_DEP_TIME_ind = i;
				break;
			case "DEP_TIME":
				DEP_TIME_ind = i;
				break;
			case "DEP_DELAY":
				break;
			case "TAXI_OUT":
				break;
			case "WHEELS_OFF":
				break;
			case "WHEELS_ON":
				break;
			case "TAXI_IN":
				break;
			case "CRS_ARR_TIME":
				CRS_ARR_TIME_ind = i;
				break;
			case "ARR_TIME":
				ARR_TIME_ind = i;
				break;
			case "ARR_DELAY":
				ARR_DELAY_ind = i;
				break;
			case "CANCELLED":
				break;
			case "CANCELLATION_CODE":
				break;
			case "DIVERTED":
				break;
			case "CRS_ELAPSED_TIME":
				break;
			case "ELAPSED_TIME":
				break;
			case "AIR_TIME":
				break;
			case "DISTANCE":
				break;
			case "DELAY_DUE_CARRIER":
				DELAY_DUE_CARRIER_ind = i;
				break;
			case "DELAY_DUE_WEATHER":
				DELAY_DUE_WEATHER_ind = i;
				break;
			case "DELAY_DUE_NAS":
				DELAY_DUE_NAS_ind = i;
				break;
			case "DELAY_DUE_SECURITY":
				DELAY_DUE_SECURITY_ind = i;
				break;
			case "DELAY_DUE_LATE_AIRCRAFT":
				DELAY_DUE_LATE_AIRCRAFT_ind = i;
				break;
			default:
				System.err.println("Error, unknown header entry found in csv:" + header[i]);
			}
		}
	}

}