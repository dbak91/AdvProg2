package model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;
/**
 * For getting a connection to, and examining the state of the database as a whole.
 *
 * <p>No constructor. Static methods</p>
 *
 * @author 23751662
 */
public class DatabaseManager
{
	private static final String		DATABASE_NAME	= "flight_data.db";
	private static final String		FLIGHTS_URL		= "jdbc:sqlite:" + DATABASE_NAME;
	private static final String		SQL_PATH_STR	= "/createTables.sql"; // in compiled resources folder

	/**
	 * Retrieves a Connection to the database hard coded in FLIGHTS_URL.
	 *
	 * <p>Redundant param</p>
	 *
	 * @param initial Redundant.  Was intended for the different between creator and interrogator of the db.
	 * @return A Connection object to the database representing live and controlled data access protocol.
	 * @throws SQLException If a database access error occurs.
	 */

	public static Connection connect(boolean initial) throws Exception
	{

		return DriverManager.getConnection(FLIGHTS_URL);
	}

	/**
	 * Reads file in 'SQL_PATH_STR' as sql string then performs an executeUpdate on the database.
	 * Will delete existing db if requested.
	 * @param deleteDb Delete existing database before create / update
	 */
	public static void createNewDataBase(boolean deleteDb)
	{
		Path dbPath = Paths.get(DATABASE_NAME);

		if (deleteDb)
		{
			try
			{
				Files.deleteIfExists(dbPath);
			} catch (IOException e)
			{
				JOptionPane.showMessageDialog(null,
						"Delete DB if exists error:/n"+e.getClass().getName() +":/n"+e.getLocalizedMessage(),
						"Data Error",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();

				// continue not critical
			}
		}

		try (Connection dbConn = connect(true); Statement sql = dbConn.createStatement())
		{
			// Use getResourceAsStream since creatTables.sql is in 'resource' project folder included / built into jar, not
			// separate on file system (like the csv data is)
			try (InputStream inputFile = DatabaseManager.class.getResourceAsStream(SQL_PATH_STR))
			{
				if (inputFile == null)
				{
					throw new FileNotFoundException("Resource not found: "+SQL_PATH_STR);
				}

				String fullSqlStr = new String(inputFile.readAllBytes(), StandardCharsets.UTF_8);

				// Use the `fullSqlStr` for executing SQL
				sql.executeUpdate(fullSqlStr);

			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"No createTable.sql found within jar/n"+e.getClass().getName() +":/n"+e.getLocalizedMessage(),
						"Data Error",
						JOptionPane.ERROR_MESSAGE);

				e.printStackTrace();
				//fallthrough
			}
		} catch (Exception e)
		{
			JOptionPane.showMessageDialog(null,
					"Error Create New Database /n" + e.getClass().getName() +":/n"+e.getLocalizedMessage(),
					"Data Error",
					JOptionPane.ERROR_MESSAGE);

			e.printStackTrace();
		}
	}

	/**
	 * Simply reuturns existence state of db ('DATABASE_NAME')
	 * @return true: exists, false: does not exist
	 */
	public static boolean exists()
	{
		Path dbPath = Paths.get(DATABASE_NAME);
		// Path sqlPath = Paths.get(_sqlPath);

		return Files.exists(dbPath);
	}

	/**
	 * Returns true if the db both exists and has at least 1 valid row of data
	 *
	 * @return true: db exists and populate
	 */
	public static boolean existsAndPopulated()
	{

		return exists() && hasData();
	}

	/**
	 * Private utility for handling the check of data being present in the db
	 * @return true: db has data, false no data
	 */
	private static boolean hasData()
	{
		boolean result = false;
		try (Connection conn = connect(true);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT 1 FROM Flight LIMIT 1"))
		{
			if (rs.next())
			{
				result = true;
			}
			else
			{
				JOptionPane.showMessageDialog(null,
						"No data found in the 'Flight' table.",
						"Data Error",
						JOptionPane.ERROR_MESSAGE);
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Creates indexes in the db. (informed note, most querys use LIKE %% so indexes wont be used anyway)
	 * <p>Intended to be used after data inserts.</p>
	 *
	 * Most search terms use 'LIKE' so not sure this even achieves anything.
	 *
	 * @throws Exception executeUpdate failed
	 */
	public void createIndexes() throws Exception
	{
		try (Connection conn = connect(false); Statement stmt = conn.createStatement())
		{
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_flight_origin ON flight(flight_origin);");
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_flight_destination ON flight(flight_destination);");
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_delay_flight_id ON delay_reason(flight_id);");
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_flight_date ON flight(date);");
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_flight_airline ON flight(airline_code);");
			stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_airport_iata ON airport(iata_code);");
		} catch (SQLException e)
		{
			// popup???
			System.err.println("Index creation failed: " + e.getMessage());
		}
	}

}
