package model.sql;

import java.util.Map;
/**
 * Collection of strings to be passed to jdbc sql Statement/PreparedStatement for selecting and searching the db.
 *
 * <p>
 * Design is inconsistent in that some strings and it's users use sql parameters '?' some use str concatenation later and others
 * use java parameters (builders). Future enh. is to have one consistent method. i.e. could use sql params everywhere but
 * would require in-code separation and use of db column identifiers in java e.g.
 *  </p>
 *
 * @author 23751662
 */
public class SearchStatements
{

	public static String		searchAllAirlines	= "SELECT * FROM Airline";

	public static String	searchAllAirports						= "SELECT * FROM Airport";

	public static String	serachFlightsFromFromAirportDestination	= "WITH LimitedAirports AS ( "
			+ "SELECT DISTINCT a.iata_code, a.name " + "FROM Airport a "
			+ "INNER JOIN Flight f ON a.iata_code = f.flight_destination " + "ORDER BY a.iata_code "
			+ "LIMIT ? OFFSET ? " + ") " + "SELECT " + "la.iata_code, la.name, "
			+ "f.flight_id,f.date,f.scheduled_arrival, f.actual_arrival, d.delay_length " + "FROM LimitedAirports la "
			+ "LEFT JOIN Flight f ON la.iata_code = f.flight_destination "
			+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "ORDER BY la.iata_code, f.date";

	public static String	searchForAirlineInFlight				= "SELECT f.flight_id, date, airline_code, flight_number, "
			+ "flight_origin,flight_destination, scheduled_departure, " + "actual_departure,"
			+ "scheduled_arrival, actual_arrival, d.reason, d.delay_length FROM "
			+ "Flight f LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "WHERE airline_code = ?";

	public static String	searchForAirportOrigInFlight				= "SELECT f.flight_id, date, airline_code, flight_number, "
			+ "flight_origin,flight_destination, scheduled_departure, " + "actual_departure,"
			+ "scheduled_arrival, actual_arrival, d.reason, d.delay_length FROM "
			+ "Flight f LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "WHERE flight_origin = ?";

	public static String	searchForDelayByFlight					= "SELECT delay_id FROM Delay_Reason WHERE flight_id = ?";

	public static String	selectAllFlightsWithDelay				= "SELECT f.flight_id, f.date, f.airline_code, f.flight_number, "
			+ "f.flight_origin, f.flight_destination, f.scheduled_departure, " + "f.actual_departure, "
			+ "f.scheduled_arrival, f.actual_arrival, d.reason,   COALESCE(d.delay_length, 0) AS delay_length "
			+ "FROM Flight f LEFT JOIN Delay_Reason d ON f.flight_id = " + "d.flight_id ";

	// backup
	//	public static String selectAirlinesWithAvg = "SELECT a.iata_code,a.name," + "COUNT(f.flight_id) AS flight_count,"
	//			+ "SUM(CASE WHEN d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay,"
	//			+ "AVG(CASE WHEN d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS avg_delay "
	//			+ "FROM airline a " + "LEFT JOIN Flight f ON a.iata_code = f.airline_code "
	//			+ "LEFT JOIN delay_reason d ON f.flight_id = d.flight_id " + "GROUP BY a.iata_code, a.name "
	//			+ "ORDER BY a.iata_code";
	public static String	selectAirlinesWithAvg					= "SELECT a.iata_code, a.name, "
			+ "COUNT(DISTINCT f.flight_id) AS flight_count, "
			+ "SUM(CASE WHEN d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay, "
			+ "AVG(d.delay_length) AS avg_delay " + "FROM Airline a "
			+ "LEFT JOIN Flight f ON a.iata_code = f.airline_code "
			+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "GROUP BY a.iata_code, a.name "
			+ "ORDER BY a.iata_code";
	public static String	selectAirportsWithData					= "SELECT a.iata_code,a.name,"
			+ "COUNT(f.flight_id) AS flight_count,"
			+ "SUM(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay_orig,"
			+ "AVG(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS avg_delay_orig,"
			+ "SUM(CASE WHEN f.flight_destination = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay_dest, "
			+ "AVG(CASE WHEN f.flight_destination = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE NULL END) AS avg_delay_dest, "
			+ "(SELECT f2.airline_code FROM Flight f2 " + "WHERE f2.flight_origin = a.iata_code "
			+ "GROUP BY f2.airline_code " + "ORDER BY COUNT(f2.airline_code) DESC LIMIT 1) AS most_frequent, "
			+ "(SELECT f2.airline_code FROM Flight f2 " + "WHERE f2.flight_origin = a.iata_code "
			+ "GROUP BY f2.airline_code " + "ORDER BY COUNT(f2.airline_code) ASC LIMIT 1) AS least_frequent "
			+ "FROM Airport a " + "LEFT JOIN  Flight f ON a.iata_code = f.flight_origin "
			+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "GROUP BY a.iata_code, a.name ";
	//orig
	public static String selectAirportMinimum = "SELECT a.iata_code,a.name," + "COUNT(f.flight_id) AS flight_count "

			+ "FROM Airport a "
			+ "LEFT JOIN Flight f ON a.iata_code = f.flight_destination OR  a.iata_code = f.flight_origin "
			+ "GROUP BY a.iata_code, a.name";

	public static String searchForAllDelays = "SELECT * FROM Delay_Reason";

	public static String buildAirlineIataQuery(String iata) {
		String stmt = "SELECT name,iata_code FROM Airline WHERE iata_code LIKE '%"+iata+"%'";

		return stmt;

	}
	public static String buildAirlineQuery(Map<String, String> searchMap)
	{
		StringBuilder sql = new StringBuilder(
				"SELECT a.iata_code,a.name, "

						+ "COUNT(f.flight_id) AS flight_count,"
						+ "SUM(CASE WHEN d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay,"
						+ "AVG(CASE WHEN d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS avg_delay "
						+ "FROM Airline a " + "LEFT JOIN Flight f ON a.iata_code = f.airline_code "
						+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id ");

		String suffix = " GROUP BY a.iata_code, a.name " + "ORDER BY a.iata_code";

		if (searchMap.containsKey("date") && !searchMap.get("date").isEmpty())
		{
			sql.append(" WHERE date LIKE '%" + searchMap.get("date") + "%'");
		}

		sql.append(suffix);
		return sql.toString();
	}

	public static String buildAirportDateQuery(String date)
	{
		String stmt = "SELECT a.iata_code,a.name," + "COUNT(f.flight_id) AS flight_count,"
				+ "SUM(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay_orig,"
				+ "AVG(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS avg_delay_orig,"
				+ "SUM(CASE WHEN f.flight_destination = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay_dest, "
				+ "AVG(CASE WHEN f.flight_destination = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE NULL END) AS avg_delay_dest, "
				+ "(SELECT f2.airline_code FROM Flight f2 " + "WHERE f2.flight_origin = a.iata_code "
				+ "GROUP BY f2.airline_code " + "ORDER BY COUNT(f2.airline_code) DESC LIMIT 1) AS most_frequent, "
				+ "(SELECT f2.airline_code FROM Flight f2 " + "WHERE f2.flight_origin = a.iata_code "
				+ "GROUP BY f2.airline_code " + "ORDER BY COUNT(f2.airline_code) ASC LIMIT 1) AS least_frequent "
				+ "FROM Airport a " + "LEFT JOIN Flight f ON a.iata_code = f.flight_origin "
				+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "WHERE f.date LIKE '%" + date + "%' "
				+ "GROUP BY a.iata_code, a.name ";

		return stmt;
	}

	public static String buildAirportFullQuery(Map<String, String> searchMap)
	{

		String stmt = "SELECT a.iata_code, a.name, " + "COUNT(f.flight_id) AS flight_count, "
				+ "SUM(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay_orig, "
				+ "AVG(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE NULL END) AS avg_delay_orig, "
				+ "(SELECT SUM(d2.delay_length) FROM Flight f2 "
				+ "LEFT JOIN Delay_Reason d2 ON f2.flight_id = d2.flight_id "
				+ "WHERE f2.flight_destination = a.iata_code AND d2.delay_length IS NOT NULL) AS total_delay_dest, "
				+ "(SELECT AVG(d2.delay_length) FROM Flight f2 "
				+ "LEFT JOIN Delay_Reason d2 ON f2.flight_id = d2.flight_id "
				+ "WHERE f2.flight_destination = a.iata_code AND d2.delay_length IS NOT NULL) AS avg_delay_dest, "
				+ "(SELECT f3.airline_code FROM Flight f3 " + "WHERE f3.flight_origin = a.iata_code "
				+ "GROUP BY f3.airline_code " + "ORDER BY COUNT(*) DESC LIMIT 1) AS most_frequent, "
				+ "(SELECT f4.airline_code FROM Flight f4 " + "WHERE f4.flight_origin = a.iata_code "
				+ "GROUP BY f4.airline_code " + "ORDER BY COUNT(*) ASC LIMIT 1) AS least_frequent "
				+ "FROM Airport a "
				+ "LEFT JOIN Flight f ON a.iata_code = f.flight_origin "
				+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id "
				+ "LEFT JOIN Airline aln ON f.airline_code = aln.iata_code ";

		boolean hasCondition = false;

		if (searchMap.get("iata") != null && !searchMap.get("iata").isEmpty())
		{
			stmt += "WHERE a.iata_code LIKE '%" + searchMap.get("iata") + "%' ";
			hasCondition = true;
		}

		if (searchMap.get("date") != null && !searchMap.get("date").isEmpty())
		{
			stmt += (hasCondition ? "AND " : "WHERE ") + "f.date LIKE '%" + searchMap.get("date") + "%' ";
			hasCondition = true;
		}

		if (searchMap.get("airline") != null && !searchMap.get("airline").isEmpty())
		{
			stmt += (hasCondition ? "AND " : "WHERE ") + "aln.iata_code LIKE '%" + searchMap.get("airline") + "%' OR aln.name LIKE '%"
					+ searchMap.get("airline")+"%' ";
		}

		stmt += "GROUP BY a.iata_code, a.name";
		return stmt;
	}

	public static String buildAirportIataQuery(String iata)
	{
		String stmt = "SELECT a.iata_code,a.name," + "COUNT(f.flight_id) AS flight_count,"
				+ "SUM(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay_orig,"
				+ "AVG(CASE WHEN f.flight_origin = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS avg_delay_orig,"
				+ "SUM(CASE WHEN f.flight_destination = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE 0 END) AS total_delay_dest, "
				+ "AVG(CASE WHEN f.flight_destination = a.iata_code AND d.delay_length IS NOT NULL THEN d.delay_length ELSE NULL END) AS avg_delay_dest, "
				+ "(SELECT f2.airline_code FROM Flight f2 " + "WHERE f2.flight_origin = a.iata_code "
				+ "GROUP BY f2.airline_code " + "ORDER BY COUNT(f2.airline_code) DESC LIMIT 1) AS most_frequent, "
				+ "(SELECT f2.airline_code FROM Flight f2 " + "WHERE f2.flight_origin = a.iata_code "
				+ "GROUP BY f2.airline_code " + "ORDER BY COUNT(f2.airline_code) ASC LIMIT 1) AS least_frequent "
				+ "FROM airport a " + "LEFT JOIN Flight f ON a.iata_code = f.flight_origin "
				+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "WHERE a.iata_code LIKE '%" + iata + "%'";

		return stmt;
	}

	// orig
	//	public static String selectAirportMinimum = "SELECT a.iata_code,a.name," + "COUNT(f.flight_id) AS flight_count, AVG(f.scheduled_arrival - f.actual_arrival) AS avg_delay, "
	//			+ "FROM airport a " + "LEFT JOIN Flight f ON a.iata_code = f.flight_destination "
	//			+ "GROUP BY a.iata_code, a.name";

	public static String buildAirportSearchQuery(Map<String, String> searchTerms)
	{
		StringBuilder baseQuery = new StringBuilder(
				"SELECT a.iata_code,a.name," + "COUNT(f.flight_id) AS flight_count " + "FROM airport a "
						+ "LEFT JOIN Flight f ON a.iata_code = f.flight_origin "
						+ "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id " + "WHERE 1=1 ");
		String suffix = "GROUP BY a.iata_code, a.name ";
		// boolean hasWhere = false;

		if (searchTerms.containsKey("origin") && !searchTerms.get("origin").isEmpty())
		{
			// baseQuery.replace(84,85," LEFT JOIN airport air ON air.iata_code")
			baseQuery.append(" AND f.flight_origin LIKE '%").append(searchTerms.get("origin")).append("%' ");
			// hasWhere = true;
		}

		if (searchTerms.containsKey("destination") && !searchTerms.get("destination").isEmpty())
		{

			baseQuery.append(" AND f.flight_destination LIKE '%").append(searchTerms.get("destination")).append("%' ");
		}

		if (searchTerms.containsKey("airline code") && !searchTerms.get("airline code").isEmpty())
		{

			baseQuery.append(" AND f.airline_code LIKE '%").append(searchTerms.get("airline code")).append("%' ");
		}

		if (searchTerms.containsKey("date") && !searchTerms.get("date").isEmpty())
		{

			baseQuery.append(" AND f.date LIKE '%").append(searchTerms.get("date")).append("%' ");
		}
		baseQuery.append(suffix);

		boolean containsMin = searchTerms.containsKey("min") && !searchTerms.get("min").isEmpty();
		boolean hasMax = searchTerms.containsKey("max") && !searchTerms.get("max").isEmpty();

		if (containsMin || hasMax)
		{
			baseQuery.append(" HAVING ");
			boolean conditionAdded = false;

			if (containsMin)
			{
				baseQuery.append("flight_count >= ").append(searchTerms.get("min"));
				conditionAdded = true;
			}

			if (hasMax)
			{
				try
				{
					int max = Integer.parseInt(searchTerms.get("max"));
					if (!containsMin || max >= Integer.parseInt(searchTerms.get("min")))
					{
						if (conditionAdded)
						{
							baseQuery.append(" AND ");
						}
						baseQuery.append("flight_count <= ").append(searchTerms.get("max"));
					}
				} catch (NumberFormatException e)
				{
					System.err.println("Error: min/max not a valid integer.");
				}
			}
		}

		System.out.println("baseQ:" + baseQuery.toString());
		return baseQuery.toString();
	}

	//
	public static String buildFlightSearchQuery(Map<String, String> searchTerms)
	{
		StringBuilder baseQuery = new StringBuilder(
				"SELECT * FROM Flight f LEFT JOIN Delay_Reason d " + "ON f.flight_id = d.flight_id "
						+ "LEFT JOIN Airline air ON air.iata_code = f.airline_code "
						+ "LEFT JOIN Airport ao ON ao.iata_code = f.flight_origin "
						+ "LEFT JOIN Airport ad ON ad.iata_code = f.flight_destination "
						+" WHERE 1=1");

		for (Map.Entry<String, String> entry : searchTerms.entrySet())
		{
			System.out.println("  " + entry.getKey() + " = " + entry.getValue());
		}

		// reset base query if contains dest or orig to include left joins/orig
		String key = "origin";
		if ((searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
				|| (searchTerms.containsKey("destination") && !searchTerms.get("destination").isEmpty()))
		{
			baseQuery = new StringBuilder(
					"SELECT * FROM Flight f " + "LEFT JOIN Delay_Reason d ON f.flight_id = d.flight_id "
							+ "LEFT JOIN Airport ao ON ao.iata_code = f.flight_origin "
							+ "LEFT JOIN Airport ad ON ad.iata_code = f.flight_destination "
							+ "LEFT JOIN Airline air ON air.iata_code = f.airline_code " +"WHERE 1=1");
		}


		/*
		 * origin and dest muist go first since they reset the abse query
		 */

		key = "origin";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			String origin = searchTerms.get(key);
			baseQuery.append(" AND (f.flight_origin LIKE '%").append(origin).append("%'").append(
					" OR ao.name LIKE '%").append(origin).append("%')");
		}

		key = "destination";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			String dest = searchTerms.get(key);
			baseQuery.append(" AND (f.flight_destination LIKE '%").append(dest).append("%'").append(
					" OR ad.name LIKE '%").append(dest).append("%')");
		}
		//	key
		key = "flight id";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND f.flight_id LIKE '%" + searchTerms.get(key) + "%'");

		}
		key = "date";
		// Add conditions for each search term if it's not empty
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND date LIKE '%" + searchTerms.get(key) + "%'");

		}

		key = "arrival from";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND actual_arrival >= " + searchTerms.get(key));
		}
		key = "arrival to";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND actual_arrival <= " + searchTerms.get(key));
		}

		key = "scheduled arrival from";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND scheduled_arrival >= " + searchTerms.get(key));
		}
		key = "scheduled arrival to";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND scheduled_arrival <= " + searchTerms.get(key));
		}

		key = "scheduled departure from";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND scheduled_departure >= " + searchTerms.get(key));
		}
		key = "scheduled departure to";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND scheduled_departure <= " + searchTerms.get(key));
		}
		key = "departure from";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND actual_departure >= " + searchTerms.get(key));
		}
		key = "departure to";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND actual_departure <= " + searchTerms.get(key));
		}
		key = "airline code";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			// System.out.println("get form searc=stataments
			// airlinecode :" + searchTerms.get("airline"));
			baseQuery.append(" AND (airline_code LIKE '%" + searchTerms.get(key) + "%'").append(" OR air.name LIKE '%").append(searchTerms.get(key)).append("%')");
		}

		key = "flight number";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND flight_number LIKE '%" + searchTerms.get(key) + "%'");
		}

		key = "delay reason";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND reason LIKE '%" + searchTerms.get(key) + "%'");
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}

		key = "reason";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			System.err.println("triggered!");
			baseQuery.append(" AND reason LIKE '%" + searchTerms.get(key) + "%'");
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}

		key = "delay from";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			System.err.println("delay key not empty");
			baseQuery.append(" AND delay_length >= " + searchTerms.get(key));
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}

		key = "delay to";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			System.err.println("delay key not empty");
			baseQuery.append(" AND delay_length <= " + searchTerms.get(key));
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}
		key = "scheduled departure";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND scheduled_departure LIKE '%" + searchTerms.get(key) + "%'");
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}

		key = "actual departure";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND actual_departure LIKE '%" + searchTerms.get(key) + "%'");
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}

		key = "scheduled arrival";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND scheduled_arrival LIKE '%" + searchTerms.get(key) + "%'");
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}

		key = "actual arrival";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND actual_arrival LIKE '%" + searchTerms.get(key) + "%'");
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}

		key = "delay time(total)";
		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			baseQuery.append(" AND delay_length = " + searchTerms.get(key)

					);
			// parameters.add(Integer.parseInt(searchTerms.get("flightNumber")));
			// // Add the Flight number
		}
		// Add other search terms as needed...

		System.out.println("base query: " + baseQuery.toString());
		key = "all";

		if (searchTerms.containsKey(key) && !searchTerms.get(key).isEmpty())
		{
			System.err.println("'any' key not empty");
			String anyTerm = searchTerms.get(key);

			 
			baseQuery.append(" AND (CAST(f.flight_id AS TEXT) LIKE '%" + anyTerm + "%' OR date LIKE '%" + anyTerm
					+ "%' OR airline_code LIKE '%" + anyTerm + "%' OR CAST(f.flight_number AS TEXT) LIKE '%" + anyTerm
					+ "%' OR air.name LIKE '%" + anyTerm
					+ "%' OR  flight_origin LIKE '%" + anyTerm + "%' OR flight_destination LIKE '%" + anyTerm
					+ "%' OR   CAST(f.scheduled_departure AS TEXT) LIKE '%" + anyTerm + "%' OR   CAST(f.actual_departure AS TEXT) LIKE '%" + anyTerm
					+ "%' OR   CAST(f.scheduled_arrival AS TEXT) LIKE '%" + anyTerm + "%' OR   CAST(f.actual_arrival AS TEXT) LIKE '%" + anyTerm
					+ "%' OR reason LIKE '%" + anyTerm + "%' OR CAST(delay_length AS TEXT) LIKE '%" + anyTerm + "%')");

			//   parameters.add(wildcardTerm);
			// parameters.add(wildcardTerm);
			// parameters.add(wildcardTerm);
			// parameters.add(wildcardTerm);
		}
		return baseQuery.toString(); // Return the dynamically constructed SQL query
	}

	public static String buildSearchAirportIata(String iata)
	{
		String stmt = "SELECT * FROM airport WHERE iata_code = '" + iata + "'";

		return stmt;
	}
}
