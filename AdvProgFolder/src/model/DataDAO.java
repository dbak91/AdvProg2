package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.sql.SearchStatements;
/**
 * DAO for the database.
 *
 * <p>A bit of an amalgam in that it is capable of returning FlightWithDelay, AirlineWithData, Airport, or DelayReason,
 * representing Flight, Airport, Airline, entries from the database tables. </p>
 *
 * @author 23751662
 */
public class DataDAO implements AutoCloseable
{
	/**
	 * Class representing Airline carrier
	 */
	public class AirlineWithData
	{
		public String	iata_code;
		public String	name;
		public int		totalDelay;
		public float	avgDelay;
		public int		numberFlights;
	}


	/**
	 * Class representing Airport
	 */
	public class Airport
	{
		public String	iata_code;
		public String	name;
		public float	avgDelayOrig;
		public int		totalDelayOrig;

		public float	avgDelayDest;
		public int		totalDelayDest;
		public String	mostAirline;
		public String	leastAirline;
		public int		totalFlights;
		public float	avgDelay;
	}


	/**
	 * Class representing Flight
	 */
	public class FlightWithDelay
	{
		public int		flightId;
		public String	date;
		public String	airlineCode;
		public int		flightNumber;
		public String	origin;
		public String	destination;
		public Integer	scheduledDeparture;
		public Integer	actualDeparture;
		public Integer	scheduledArrival;
		public Integer	actualArrival;
		public String	delayReason;
		public Integer	delayLength;
	}
	/**
	 * Class representing Delay Reason
	 */
	public class DelayReason
	{
		public int flightId=0;
		public String reason = "";
		public int reasonCount =0;
	}

	// for connection interaction with database
	private final Connection conn;


	/**
	 * Constructor. Initialises Connection variable.
	 *
	 * @throws Exception db access failed
	 */
	public DataDAO() throws Exception
	{
		this.conn = DatabaseManager.connect(false);
	}

	/**
	 * Override: Autoclose, shut down db connection
	 */
	@Override
	public void close() throws Exception
	{
		//sclean up.
		this.conn.close();
	}



	/*
	 * -------------------------------------------------------
	 * ------------Interrogation functions--------------------
	 * -------------------------------------------------------
	 */


	/**
	 * Returns all flights in db (limited by page_size(LIMIT) and offset(page_size*current page)
	 *
	 * @param page Current displayed page for SQL offset calculation
	 * @param pageSize Intended page size, which represents the LIMIT size in SQL
	 * @param sortBy sql sortB column
	 * @param ascending true or false, converted to ASC or DESc in sql statement
	 * @return List of all FlightWithDelays found
	 * @throws SQLException
	 */
	public List<FlightWithDelay> getAllFlights(int page, int pageSize, String sortBy, boolean ascending)
			throws SQLException
	{
		List<FlightWithDelay> flights = new ArrayList<>();
		String order = ascending ? "ASC" : "DESC";

		// Validate and sanitise sortBy
		Set<String> validSortColumns = Set.of("name",
				"date",
				"airline_code",
				"flight_origin",
				"flight_destination",
				"flight_id",
				"flight_number",
				"scheduled_departure",
				"actual_departure",
				"scheduled_arrival",
				"actual_arrival",
				"reason",
				"delay_length");
		if (!validSortColumns.contains(sortBy))
		{
			System.err.println("invalid sort by passed to getflights():" + sortBy);
			sortBy = "date";

		}

		// append f to all flight fields, exclude delays (from delay table)
		else if (!sortBy.equals("reason") && !sortBy.equals("delay_length"))
		{
			sortBy = "f." + sortBy;
		}
		System.out.println("getallflights");
		try (PreparedStatement stmt = conn.prepareStatement(
				SearchStatements.selectAllFlightsWithDelay + " ORDER BY " + sortBy + " " + order + " LIMIT ? OFFSET ?"))
		{

			stmt.setInt(1, pageSize);
			stmt.setInt(2, page * pageSize);
			System.out.println(stmt.toString());
			ResultSet rs = stmt.executeQuery();
			System.out.println("results:" + rs.getFetchSize());
			while (rs.next())
			{
				FlightWithDelay flight = populateFlightWithDelay(rs);
				// System.out.println("al:"+flight.airlineCode);
				flights.add(flight);
			}
		}

		return flights;
	}


	/**
	 * Returns a List of Airports in the db
	 *
	 * <p>Appends SearchStatements.selectAirportMinimum with 'order' and 'sort' then passes to executeQuery
	 * in order to receive all airports</p>
	 *
	 * @param page Current displayed page for SQL offset calculation
	 * @param pageSize Intended page size, which represents the LIMIT size in SQL
	 * @param sortBy sql sortB column
	 * @param ascending true or false, converted to ASC or DESc in sql statement
	 * @return List of all airports found
	 * @throws SQLException
	 */
	public List<Airport> getAllAirports(int page, int pageSize, String sortBy, boolean ascending) throws SQLException
	{
		List<Airport> airports = new ArrayList<>();
		String order = ascending ? "ASC" : "DESC";

		String sql = SearchStatements.selectAirportMinimum + " ORDER BY " + sortBy + " " + order + " LIMIT ? OFFSET ?";
		System.out.println("sqlString" + sql);
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setInt(1, pageSize);
		stmt.setInt(2, page * pageSize);

		System.out.println("getAirports()-" + stmt.toString());
		ResultSet rs = stmt.executeQuery();
		while (rs.next())
		{
			Airport airport = new Airport();
			airport.iata_code = rs.getString("iata_code");
			airport.name = rs.getString("name");
			// airport.avgDelayOrig = rs.getInt("avg_delay_orig");
			airport.totalFlights = rs.getInt("flight_count");
			// airport.avgDelay = rs.getFloat("avg_delay");
			airports.add(airport);
		}

		System.out.println("airport size " + airports.size());
		return airports;
	}

	/**
	 * Takes an iata_code and returns the matching AirlineWithData from Airline db table
	 *
	 * @param airlineCode the iata code to search
	 * @return airline matching iata
	 * @throws SQLException
	 */
	public AirlineWithData getAirlineFromIata(String airlineCode) throws SQLException
	{
		// TODO Auto-generated method stub
		AirlineWithData airline = new AirlineWithData();
		//airline.iata_code

		String sql = SearchStatements.buildAirlineIataQuery(airlineCode);

		PreparedStatement stmt = conn.prepareStatement(sql);

		ResultSet rs = stmt.executeQuery();

		while (rs.next())
		{
			airline.iata_code = rs.getString("iata_code");
			airline.name = rs.getString("name");//        	}
		}
		return airline;
	}

	/**
	 * Takes a iata_code and returns Airport
	 *
	 * <p>Uses SearchStatements.buildSearchAirportIata and exceuteQuery() to match iata code to Airport </p>
	 *
	 * @param iataCode the iata code to match Airport iata
	 * @return An Airport object witht he matching iata
	 * @throws SQLException
	 */
	public Airport getAirportByIata(String iataCode) throws SQLException
	{
		String sql = SearchStatements.buildSearchAirportIata(iataCode);
		Airport airport = new Airport();
		// change to prepared
		PreparedStatement stmt = conn.prepareStatement(sql);

		ResultSet rs = stmt.executeQuery();

		while (rs.next())
		{

			airport.iata_code = rs.getString("iata_code");
			airport.name = rs.getString("name");

		}
		return airport;
	}
	/**
	 * Returns a List of AirlineWithDatas that match the searchMap passed
	 *
	 * @param searchMap MAp showing key->search term to be selected from db.
	 * @return All Airline matching searchMap condition
	 * @throws SQLException
	 */
	public List<AirlineWithData> getAirlines(Map<String, String> searchMap) throws SQLException
	{
		List<AirlineWithData> airlines = new ArrayList<>();

		String sql = "";
		if (searchMap == null || searchMap.isEmpty())
		{
			sql = SearchStatements.selectAirlinesWithAvg;
		}
		else
		{
			if(searchMap.containsKey("date"))
			{
				String date = searchMap.get("date");
				date=date.replace("-","");
				searchMap.put("date", date);
			}
			sql = SearchStatements.buildAirlineQuery(searchMap);
		}

		// change to prepared
		PreparedStatement stmt = conn.prepareStatement(sql);

		System.out.println("getaurlines sql:" + stmt.toString());
		ResultSet rs = stmt.executeQuery();

		// Loop through the result set and add each airline code to the list
		while (rs.next())
		{
			AirlineWithData airline = new AirlineWithData();
			airline.iata_code = rs.getString("iata_code");
			airline.name = rs.getString("name");
			airline.avgDelay = rs.getFloat("avg_delay");
			airline.totalDelay = rs.getInt("total_delay");
			airline.numberFlights = rs.getInt("flight_count");
			rs.getInt("flight_count");
			airlines.add(airline);
		}
		return airlines;
	}



	/**
	 * Takes a searchMap and returns all flights matching the search map conditions
	 *
	 * @param searchMap key->term mapping of search terms
	 * @param page Current displayed page for SQL offset calculation
	 * @param pageSize Intended page size, which represents the LIMIT size in SQL
	 * @param sortBy sql sortB column
	 * @param ascending true or false, converted to ASC or DESc in sql statement
	 * @return List of all FlightWithDelay found
	 * @throws SQLException
	 */
	public List<FlightWithDelay> getFlightBySearch(
			Map<String, String> searchMap,
			int page,
			int pageSize,
			String sortBy,
			boolean ascending) throws SQLException
	{
		List<FlightWithDelay> flights = new ArrayList<>();

		String order = ascending ? "ASC" : "DESC";

		// auto sort by flight id if that search type.
		// lazy programming didnt check which values are valid from this side, underscore or no,
		// so test for both
		if(searchMap.containsKey("flight id") || searchMap.containsKey("flight_id"))
		{
			sortBy = "f.flight_id";
		}
		
		// database is 8char without breaks for date, as spec. I would have had it has char10 and kept db inline with csv. 
		
		for(String map: searchMap.keySet())
		{
			System.out.println("key"+map + "val"+searchMap.get(map));
		}
		if(searchMap.containsKey("date")||searchMap.containsKey("all"))
		{
			
			String date = searchMap.get("date");
			if(date != null && !date.isEmpty())
			{
				date=date.replace("-","");
			
				searchMap.put("date", date);
			}
			
			String all = searchMap.get("all");
			if(all != null && !all.isEmpty())
			{
				all=all.replace("-","");
			
				searchMap.put("all", all);
			}
			
		}
		try (PreparedStatement stmt = conn.prepareStatement(SearchStatements.buildFlightSearchQuery(searchMap)
				+ " ORDER BY " + sortBy + " " + order + " LIMIT ? OFFSET ?"))
		{
			/*
			 * this must be same order as buildFlightSearchQuery to set params
			 */

			// stmt.setString(1,"%2022%");
			stmt.setInt(1, pageSize);
			stmt.setInt(2, page * pageSize);

			System.out.println(stmt.toString());
			ResultSet rs = stmt.executeQuery();

			while (rs.next())
			{
				FlightWithDelay flight = populateFlightWithDelay(rs);
				flights.add(flight);
			}
			for (String key : searchMap.keySet())
			{
				System.out.println("key:" + key);
			}
			System.out.println("getflightsbyserarhc22 statment " + stmt.toString());
		} catch (SQLException gg)
		{
			gg.printStackTrace();
		}

		return flights;
	}



	/**
	 * Takes a search map and gets the airports where the search map is min/max number of flights
	 *
	 * @param searchMap expected 'min'->term1,'max'->term2
	 * @param page Current displayed page for SQL offset calculation
	 * @param pageSize Intended page size, which represents the LIMIT size in SQL
	 * @param sortBy sql sortB column
	 * @param ascending true or false, converted to ASC or DESc in sql statement
	 * @return
	 * @throws SQLException
	 */
	public List<Airport> getBasicAirports(
			Map<String, String> searchMap,
			int page,
			int pageSize,
			String sortBy,
			boolean ascending) throws SQLException
	{
		List<Airport> airports = new ArrayList<>();

		
		//convert to sql
		String order = ascending ? "ASC" : "DESC";

		if(searchMap.containsKey("date"))
		{
			String date = searchMap.get("date");
			date=date.replace("-","");
			searchMap.put("date", date);
		}
		try (PreparedStatement stmt = conn.prepareStatement(SearchStatements.buildAirportSearchQuery(searchMap)
				+ " ORDER BY " + sortBy + " " + order + " LIMIT ? OFFSET ?"))
		{
			/*
			 * this must be same order as buildFlightSearchQuery to set params
			 */
			stmt.setInt(1, pageSize);
			stmt.setInt(2, page * pageSize);

			System.out.println(stmt.toString());

			ResultSet rs = stmt.executeQuery();

			while (rs.next())
			{
				Airport airport = new Airport();
				airport.iata_code = rs.getString("iata_code");
				airport.name = rs.getString("name");
				airport.totalFlights = rs.getInt("flight_count");
				airports.add(airport);
			}
			for (String key : searchMap.keySet())
			{
				System.out.println("key:" + key);
			}
			System.out.println("getminMAxAirport " + stmt.toString());
		}

		return airports;
	}

	/**
	 * @param airportSearchMap
	 * @param page Current displayed page for SQL offset calculation
	 * @param pageSize Intended page size, which represents the LIMIT size in SQL
	 * @param sortBy sql sortB column
	 * @param ascending true or false, converted to ASC or DESc in sql statement
	 * @return
	 * @throws SQLException
	 */
	public List<Airport> searchAirports(
			Map<String, String> airportSearchMap,
			int page,
			int pageSize,
			String sortBy,
			boolean ascending) throws SQLException
	{

		List<Airport> airportsWithFlights = new ArrayList<>();
		String order = ascending ? "ASC" : "DESC";

		if(airportSearchMap.containsKey("date"))
		{
			String date = airportSearchMap.get("date");
			if(date!=null)
			{
				date=date.replace("-","");
				airportSearchMap.put("date", date);
			}
			
		}
		String sql = SearchStatements.buildAirportFullQuery(airportSearchMap);

		// no such date field for airport , only date search
		if (sortBy.toLowerCase().equals("date"))
		{
			sortBy = "a.iata_code";
		}
		
		if (sortBy.toLowerCase().equals("name"))
		{
			sortBy = "a.name";
		}

		String suffix = " ORDER BY " + sortBy + " " + order + " LIMIT ? OFFSET ?";

		sql += suffix;

		System.out.println("sAirports: " + sql);
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setInt(1, pageSize);
		stmt.setInt(2, page * pageSize);
		// System.out.println("pagesz:"+pageSize);

		System.out.println("sAirports: " + stmt.toString());

		ResultSet rs = stmt.executeQuery();

		Map<String, Integer> iataDelayMap = new HashMap<>();

		while (rs.next())
		{
			Airport airport = new Airport();
			airport.iata_code = rs.getString("iata_code");
			airport.name = rs.getString("name");
			airport.avgDelayOrig = rs.getFloat("avg_delay_orig");
			airport.avgDelayDest = rs.getFloat("avg_delay_dest");
			airport.totalDelayOrig = rs.getInt("total_delay_orig");
			airport.totalDelayDest = rs.getInt("total_delay_dest");

			AirlineWithData airline = getAirlineFromIata(rs.getString("most_frequent"));
			airport.mostAirline = airline.name + "("+rs.getString("most_frequent")+")";
			airline = getAirlineFromIata(rs.getString("least_frequent"));
			airport.leastAirline =  airline.name + "("+rs.getString("least_frequent")+")";
			airport.totalFlights = rs.getInt("flight_count");
			iataDelayMap.put(airport.iata_code, airport.totalDelayOrig);

			airportsWithFlights.add(airport);
			//        	}
		}

		System.out.println("SIZE OF AIRPORTS :" + airportsWithFlights.size());
		return airportsWithFlights;
	}
	/*
	 * -------------------------------------------------------
	 * ------------Utility-------------------------------------
	 * -------------------------------------------------------
	 */
	/**
	 * returns a FlightWithDelay populated from the passed database query result set
	 * @param rs Result set from PreparedStatement execute query
	 * @return
	 * @throws SQLException
	 */
	public FlightWithDelay populateFlightWithDelay(ResultSet rs) throws SQLException
	{
		FlightWithDelay flight = new FlightWithDelay();
		flight.flightId = rs.getInt("flight_id");
		flight.date = rs.getString("date");
		flight.airlineCode = rs.getString("airline_code");
		flight.flightNumber = rs.getInt("flight_number");
		flight.origin = rs.getString("flight_origin");
		flight.destination = rs.getString("flight_destination");
		flight.scheduledDeparture = rs.getInt("scheduled_departure");
		flight.actualDeparture = rs.getInt("actual_departure");
		flight.scheduledArrival = rs.getInt("scheduled_arrival");
		flight.actualArrival = rs.getInt("actual_arrival");
		flight.delayReason = rs.getString("reason");
		flight.delayLength = rs.getInt("delay_length");
		// flight.delayLength = rs.getInt("delay_length");

		return flight;
	}
	/*
	 * -------------------------------------------------------
	 * ------------UNUSED-------------------------------------
	 * -------------------------------------------------------
	 */



	/**
	 * Takes an airport iata code and returns all flights with that airport as origin departure
	 * @param airport
	 * @param sortBy sql SORTBY column
	 * @param ascending true or false, converted to ASC or DESC in sql statement
	 * @return List of all airports found
	 * @throws SQLException
	 *
	 * */

	public List<FlightWithDelay> getFlightsByAirportAsOrig(String airport, String sortBy, boolean ascending)
			throws SQLException
	{
		List<FlightWithDelay> flights = new ArrayList<>();
		String order = ascending ? "ASC" : "DESC";

		PreparedStatement stmt = conn.prepareStatement(	SearchStatements.searchForAirportOrigInFlight + " ORDER BY "
				+ sortBy + " " + order);
		stmt.setString(1, airport);
		ResultSet rs = stmt.executeQuery();

		// Loop through the result set and add each airline code to the list
		while (rs.next())
		{
			FlightWithDelay flight = populateFlightWithDelay(rs);
			flights.add(flight);
		}
		return flights;
	}

	public Map<String,Integer> getAllDelaysWithCount()
	{
		// TODO Auto-generated method stub
		Map<String,Integer> delayCountMap = new HashMap<>();
		//String order = ascending ? "ASC" : "DESC";

		try
		{
			PreparedStatement stmt = conn.prepareStatement(	SearchStatements.searchForAllDelays);
			//stmt.setString(1, airport);
			ResultSet rs;

			rs = stmt.executeQuery();

			String securityKey = "security";
			int secVal =0;
			delayCountMap.put(securityKey,secVal);
			String nasKey ="air traffic";
			int nasVal =0;
			delayCountMap.put(nasKey,nasVal);
			String carrierKey = "carrier";
			int carVal =0;
			delayCountMap.put(carrierKey,carVal);
			String aircraftKey = "late aircraft";
			int craftVal =0;
			delayCountMap.put(aircraftKey,craftVal);
			String weatherKey = "weather";
			int weathVal=0;
			delayCountMap.put(weatherKey,weathVal);
			// Loop through the result set and add each airline code to the list
			while (rs.next())
			{
				String reason = rs.getString("reason").toLowerCase();

				if(reason.contains(securityKey)) {
					secVal++;
					delayCountMap.put(securityKey,secVal);
				}
				if(reason.contains(nasKey)) {
					nasVal++;
					delayCountMap.put(nasKey,nasVal);
				}
				if(reason.contains(carrierKey)) {
					carVal++;
					delayCountMap.put(carrierKey,carVal);

				}
				if(reason.contains(aircraftKey)) {
					craftVal++;
					delayCountMap.put(aircraftKey,craftVal);
				}


				if(reason.contains(weatherKey)) {
					weathVal++;
					delayCountMap.put(weatherKey,weathVal);
				}
			}
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




		return delayCountMap;
		//eturn null;
	}


}
