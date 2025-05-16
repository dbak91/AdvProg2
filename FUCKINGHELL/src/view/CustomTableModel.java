package view;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import model.DataDAO;
import model.DataDAO.AirlineWithData;
import model.DataDAO.Airport;
import model.DataDAO.FlightWithDelay;

/**
 * Extends TableModel to avoid repeated set column ident. red tape and allow set rows from data dedication.
 *
 *
 * @author 23751662
 */
public class CustomTableModel extends DefaultTableModel
{
	private static final long serialVersionUID = 1L;

	public CustomTableModel()
	{

		super();
	}

	// stop edit
	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
	}

	public void setColumnIdentifiers(String[] columns, JTable table)
	{
		System.out.println("setmaincolumn identifyer");

		setRowCount(0); // This will remove all existing rows

		setColumnIdentifiers(columns);

		table.revalidate();
		table.repaint();
	}

	public void setRowsFromAirlines(List<AirlineWithData> airlines)
	{

		setRowCount(0); // clear previous rows
		// tableModel.setColumnIdentifiers(new String[] { "IATA Code", "Airline
		// Name", "Country" });

		System.out.println("setrowdfromairline number=" + airlines.size());
		int i = 0;
		while (i < airlines.size() && airlines.get(i) != null)
		{
			AirlineWithData airline = airlines.get(i);
			Vector<Object> row = new Vector<>();
			row.add(airline.iata_code);
			row.add(airline.name);
			row.add(airline.avgDelay);

			row.add(airline.totalDelay);
			row.add(airline.numberFlights);
			// row.add(airline.totalDelay);
			addRow(row);
			i++;
		}
	}

	public void setRowsFromAirport(List<Airport> airports)
	{

		setRowCount(0);
		for (Airport airport : airports)
		{

			Vector<Object> row = new Vector<>();
			row.add(airport.iata_code);
			row.add(airport.name);

			if (airport.totalDelayOrig != 0)
			{
				row.add(airport.avgDelayOrig);
				row.add(airport.totalDelayOrig);
				row.add(airport.avgDelayDest);
				row.add(airport.totalDelayDest);
				row.add(airport.mostAirline);
				row.add(airport.leastAirline);

				//
			} // System.out.println("total:"+airport.totalDelayOrig);
			row.add(airport.totalFlights);
			// row.add(airport.avgDelay);
			addRow(row);

		}
	}

	public void setRowsFromFlights(List<FlightWithDelay> flights, Map<Integer, FlightWithDelay> rowFlightMap)
			throws Exception
	{
		// setMainColumnHeader();
		setRowCount(0);
		int i = 0;
		while (i < flights.size() && flights.get(i) != null)
		{
			FlightWithDelay flight = flights.get(i);
			Vector<Object> row = new Vector<>();
			row.add(flight.flightId);
			row.add(flight.date);
			String airlineCode = flight.airlineCode;
			DataDAO dataAccess = new DataDAO();
			AirlineWithData airline = dataAccess.getAirlineFromIata(airlineCode);

			row.add(airline.name + "(" + flight.airlineCode + ")");
			row.add(flight.flightNumber);
			// row.add(flight.origin);

			Airport airport = dataAccess.getAirportByIata(flight.origin);
			row.add(airport.name + "(" + flight.origin + ")");
			// row.add(flight.destination);
			// flightAccess = new FlightDAO();
			airport = dataAccess.getAirportByIata(flight.destination);

			// finally {
			dataAccess.close();

			row.add(airport.name + "(" + flight.destination + ")");
			row.add(flight.scheduledDeparture);
			row.add(flight.actualDeparture);
			row.add(flight.scheduledArrival);
			row.add(flight.actualArrival);
			row.add(flight.delayReason);
			row.add(flight.delayLength);
			addRow(row);
			rowFlightMap.put(i, flight);
			i++;
		}

	}
}
