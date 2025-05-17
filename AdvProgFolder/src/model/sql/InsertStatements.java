package model.sql;
/**
 * [Brief description of what this class does.]
 *
 * <p>[More detailed explanation of the class if needed, including purpose, usage context, etc.]</p>
 *
 * @author 23751662
 */

/**
 * Insert sql statements for adding to database.
 *
 * <p>For tables:</p>
 * <ul>
 *   <li><b>airline</b>
 *     <ul>
 *       <li>iata_code</li>
 *       <li>name</li>
 *     </ul>
 *   </li>
 *   <li><b>airport</b>
 *     <ul>
 *       <li>iata_code</li>
 *       <li>name</li>
 *     </ul>
 *   </li>
 *   <li><b>flight</b>
 *     <ul>
 *       <li>flight_id</li>
 *       <li>date</li>
 *       <li>airline_code</li>
 *       <li>flight_number</li>
 *       <li>flight_origin</li>
 *       <li>flight_destination</li>
 *       <li>scheduled_departure</li>
 *       <li>actual_departure</li>
 *       <li>scheduled_arrival</li>
 *       <li>actual_arrival</li>
 *     </ul>
 *   </li>
 *   <li><b>delay_reason</b>
 *     <ul>
 *       <li>flight_id</li>
 *       <li>reason</li>
 *       <li>delay_length</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class InsertStatements
{
	public static String	insertAirlineSQL	= "INSERT OR IGNORE INTO Airline (iata_code,name) VALUES (?,?)";
	public static String	insertAirportSQL	= "INSERT OR IGNORE INTO Airport (iata_code, name) VALUES (?, ?)";

	public static String	insertFlightSQL		= "INSERT INTO flight (flight_id,date,airline_code,"
			+ "flight_number,flight_origin,flight_destination,scheduled_departure,"
			+ "actual_departure,scheduled_arrival,actual_arrival" + ") VALUES (?,?,?,?,?, ?, ?, ?, ?, ?)";

	public static String	insertDelaySQL		= "INSERT INTO delay_reason ( flight_id,reason,delay_length"
			+ ") VALUES (?,?,?)";

}
