package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import model.DataDAO;
import model.DataDAO.AirlineWithData;
import model.DataDAO.Airport;
import model.DataDAO.FlightWithDelay;

/**
 * Extends JTable with populate row and set listener methods.  
 */
public class CustomJTable extends JTable
{
	private static final long serialVersionUID = 1L;
	MainDisplayFrame parent;
	CustomTableModel model;
	
	public final Color                         highlightColour         = new Color(245,245,240);
	public final int							PAGE_SIZE				= 50;
	public int									airportFullPageSize		= 20;
	public String[]								flightColumns			= new String[]
			{
					"Flight ID",
					"Date",
					"Airline Code",
					"Flight Number",
					"Origin",
					"Destination",
					"Scheduled Departure",
					"Actual Departure",
					"Scheduled Arrival",
					"Actual Arrival",
					"Delay Reason",
					"Delay Time(Total)"

			};

	public String[]								airportColumns			=
		{
				"iata_code",
				"name",
				"Avg Delay Orig",
				"Total Delay Orig",
				"Avg Delay Dest",
				"Total Delay Dest",
				"Most Airline",
				"Least Airline",
				"Total Flights"
		};
	public String[]								airlineColumns			=
		{
				"iata_code",
				"name",
				"Avg Delay",
				"Total Delay",
				"Num Flights"
		};

	public String[]								airportMinColumns		=
		{
				"iata_code",
				"name",
				"Flights"
		};


	/**
	 * conmstrucotr, sets parenrt
	 */
	public CustomJTable(CustomTableModel model,MainDisplayFrame parent)
	{
		super(model);

		this.model = model;

		this.parent=parent;
	}
	/*
	 * -------------------------------------------------------
	 * -----------Set listeners-------------------------------
	 * -------------------------------------------------------
	 */




	public void setTableHeaderListener()
	{
		getTableHeader().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				System.err.println("mouse clicked on header");
				int columnIndex = columnAtPoint(e.getPoint());
				if (columnIndex >= 0)
				{
					String columnName = getColumnName(columnIndex);
					// currentSortColumn = columnName.toLowerCase().replace("
					// ", "_");
					// Convert display name to database field name (e.g., Flight
					// ID -> flight_id)
					String selectedColumn = columnName.toLowerCase().replace(" ", "_");
					String order = parent.ascending ? "ASC" : "DESC";
					System.out.println("was " + order + "now opposite");

					parent.ascending = !parent.ascending;
					// Toggle order if same column clicked

					parent.currentSortColumn = selectedColumn;

					parent.currentPage = 0;
					JTableHeader header = getTableHeader();
					TableColumnModel colModel = header.getColumnModel();
					/*
					 * FUTURE OVERSITE would be missing re populate call by airlines returning >50,
					 * currently okay only 18 in db
					 *
					 */

					//restricting to non-airline selected
					if (!parent.viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("line"))
					{
						for (int i = 0; i < colModel.getColumnCount(); i++)
						{
							String baseName = flightColumns[i];
							String displayName = baseName;

							String columnId = baseName.toLowerCase().replace(" ", "_");
							if (columnId.equals(parent.currentSortColumn))
							{
								displayName += parent.ascending ? " ▲" : " ▼";
								System.out.println(displayName);
							}

							colModel.getColumn(i).setHeaderValue(displayName);
						}
						// Call once after all updates
						getTableHeader().revalidate();
						getTableHeader().repaint();
						header.repaint();

						// tableModel.setRowCount(0);

						// cater for difference in headers vs sql table/columns
						if (selectedColumn.toLowerCase().contains("origin"))
						{
							parent.currentSortColumn = "flight_origin";
						}
						if (selectedColumn.toLowerCase().contains("flights"))
						{
							parent.currentSortColumn = "flight_count";
						}
						if (selectedColumn.toLowerCase().contains("delay_reason"))
						{
							parent.currentSortColumn = "reason";
						}
						if (selectedColumn.toLowerCase().contains("time"))
						{
							parent.currentSortColumn = "delay_length";
						}
						if (selectedColumn.toLowerCase().contains("destination"))
						{
							parent.currentSortColumn = "flight_destination";
						}
						Runnable task = () -> {

							if (parent.viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("flights"))
							{
								if (parent.searchActive)
								{

									populateTableWithFlightSearch(parent.currentSortColumn, parent.ascending);
								}
								else
								{
									populateTableWithAllFlights(parent.currentSortColumn, parent.ascending);
								}

							}
							else if (parent.viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains(
									"analysis"))
							{
								/*
								 * cater for difference ebteween column identifiers and db fields
								 */
								if (parent.currentSortColumn.toLowerCase().contains("delay orig"))
								{
									parent.currentSortColumn = "avg_delay_orig";
								}
								else if (parent.currentSortColumn.toLowerCase().contains("delay dest"))
								{
									parent.currentSortColumn = "avg_delay_dest";
								}
								else if (parent.currentSortColumn.toLowerCase().contains("total delay dest"))
								{
									parent.currentSortColumn = "total_delay_dest";
								}
								else if (parent.currentSortColumn.toLowerCase().contains("total delay orig"))
								{
									parent.currentSortColumn = "total_delay_orig";
								}
								else if (parent.currentSortColumn.toLowerCase().contains("most_airline"))
								{
									parent.currentSortColumn = "most_frequent";
								}
								else if (parent.currentSortColumn.toLowerCase().contains("least_airline"))
								{
									parent.currentSortColumn = "least_frequent";
								}

								populateTableWithAirport(true);
							}
							else if (!parent.searchActive)
							{
								populateTableWithAirport(false);
							}
							else
							{
								populateBasicTableWithAirport();
							}

						}; // runnable task

						parent.runWithLoadingLabel(task, null, "Reording...");

					}// selected contains 'line'

				} // colukmn index > 0
			}// mouse event

		});// add mouse listener
	}// set header listener


	public void setTableRowListener()
	{

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if(parent.viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("flight"))
				{

					int rowClicked = rowAtPoint(e.getPoint());

					if (rowClicked >= 0 && e.getClickCount() == 1) // row count for?
					{
						// Example: get column data from the model

						int scheduled = (int) getValueAt(rowClicked,6);
						int actual = (int) getValueAt(rowClicked,7);

						int departureDelay = getDelay(scheduled,actual);

						scheduled = (int) getValueAt(rowClicked,8);
						actual = (int) getValueAt(rowClicked,9);

						int arrivalDelay = getDelay(scheduled,actual);
						//String cellData1 = (String) table.getValueAt(rowClicked, 0); // column 0
						//String cellData2 = (String) table.getValueAt(rowClicked, 1); // column 1

						int dueDelay = arrivalDelay-departureDelay;
						String message = "Airline: " + getValueAt(rowClicked,2)
						+ "\n Departure delay: " + (departureDelay > 0? "+":"") + departureDelay + " minutes"
						+ "\n Arrival delay: " + (arrivalDelay > 0? "+":"") + arrivalDelay + " minutes"
						+ "\n Reason: " + getValueAt(rowClicked,10)
						+ "\n In flight adjustment: " + (dueDelay > 0? "+":"") + dueDelay + " minutes";



						JOptionPane.showMessageDialog(CustomJTable.this, message, "Flight Information", JOptionPane.INFORMATION_MESSAGE);
					}
				}// if flight

			}//mouse clciked override

			private int getDelay(int scheduled, int actual)
			{
				// TODO Auto-generated method stub
				int scheduledHours = scheduled / 100; // 12
				int scheduledMinutes = scheduled % 100; // 45

				int actualHours = actual / 100; // 13
				int actualMinutes = actual % 100; // 55

				int scheduledTotalMinutes = scheduledHours * 60 + scheduledMinutes; // 12*60 + 45 = 765
				int actualTotalMinutes = actualHours * 60 + actualMinutes; // 13*60 + 55 = 835

				return actualTotalMinutes - scheduledTotalMinutes;
			}//get delay
		});// add listener
	}//setTableRowListener

	/*
	 * -------------------------------------------------------
	 * -----------Populate tables with------------------------
	 * -------------------------------------------------------
	 *  - Flights
	 *  - Airline
	 *  - Airport
	 *  -------------------------------------------------------
	 */

	/**
	 * Takes a single search term and a type and populates the Flight table based on
	 * results
	 *
	 * @param type the database Flight field to search (column) e.g. "flight_id"
	 * @param term the term to match with LIKE
	 *
	 */
	public void populateTableWithSingleSearchTerm(String type, String term, String sortBy, boolean ascending)
	{
		System.err.println("pop with single search " + type + ":" + term);
		//	parent.searchType = type;
		//this.searchTerm = term;

		parent.searchMap = new HashMap<>();

		parent.searchMap.put(type, term);

		populateTableWithFlightSearch(sortBy, ascending);
	}


	/**
	 * Set the table to contain airline data
	 *
	 *  retrieves all airlines matching searchMap criteria and applied to table
	 */
	public void populateTableWithAirline()
	{
		try
		{
			System.out.println("popuylatewithairlines");

			DataDAO dataAccess = new DataDAO();

			List<AirlineWithData> airlines = dataAccess.getAirlines(parent.searchMap);

			// setColumnsFromAirLines(airlines);



			model.setRowCount(0); // This will remove all existing rows


			model.setColumnIdentifiers(airlineColumns, this);

			getColumnModel().getColumn(4).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // total flights
			getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // avg delay
			model.setRowsFromAirlines(airlines);
			// Create the JTable with the table model

			dataAccess.close();
		} catch (SQLException e)
		{
			e.printStackTrace();

		} catch (Exception f)
		{
			f.printStackTrace();
		}
	}//populate airlines


	/**
	 * Populate the table based off all flights in the database
	 *
	 * @param sortBy    database field to sort returned results from e.g. "date"
	 * @param ascending true or false, converted to ASC/DESC in SQL.
	 */
	public void populateTableWithAllFlights(String sortBy, boolean ascending)
	{
		parent.searchActive = false;
		model.setColumnIdentifiers(flightColumns, this);

		//total time column highlight

		getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour));

		getColumnModel().getColumn(4).setCellRenderer(new CustomColumnColorRenderer(highlightColour));

		getColumnModel().getColumn(5).setCellRenderer(new CustomColumnColorRenderer(highlightColour));
		getColumnModel().getColumn(10).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); //reason
		getColumnModel().getColumn(11).setCellRenderer(new CustomColumnColorRenderer(highlightColour));// total delay
		try (DataDAO dataAccess = new DataDAO();)
		{
			// flightAccess = new FlightDAO();
			try
			{
				if (parent.searchMap.containsKey("flight id"))
				{
					System.err.println("should go hgere change to fsort by flight id");
				}
				// flightAccess = new FlightDAO();
				List<FlightWithDelay> flights = dataAccess.getAllFlights(parent.currentPage, PAGE_SIZE, sortBy, ascending);

				model.setRowsFromFlights(flights, parent.rowFlightMap);
			} catch (SQLException e)
			{
				System.err.println("Error populating tablke in populatweTableWithMain");
				e.printStackTrace();
			}
		} catch (Exception e)
		{
			JOptionPane.showMessageDialog(null,
					"Database connection failed:\n" + e.getMessage(),
					"Connection Error",
					JOptionPane.ERROR_MESSAGE);
			System.err.println("Error creating flight dao, (connecting to db)");
			e.printStackTrace();
		}
	}//populate with all flights


	/**
	 * Populate table with all flights matching searchMap contents
	 *
	 * @param sortBy    database field to sort returned results from e.g. "date"
	 * @param ascending true or false, converted to ASC/DESC in SQL.
	 */
	public void populateTableWithFlightSearch(String sortBy, boolean ascending)
	{
		System.out.println("pop with search");
		parent.searchActive = true;
		try (DataDAO dataAccess = new DataDAO())
		{
			// Map<String,String> search = new HashMap<>();
			List<FlightWithDelay> flights = dataAccess.getFlightBySearch(
					parent.searchMap,
					parent.currentPage,
					PAGE_SIZE,
					sortBy,
					ascending);

			model.setRowsFromFlights(flights, parent.rowFlightMap);
		} catch (SQLException e)
		{
			System.err.println("Error populating ");
			e.printStackTrace();

		} catch (Exception f)
		{
			System.err.println("Error from creating flight dao, populatWithSearch");
			f.printStackTrace();
		}
	}//populate with flight search


	/**
	 * Populates table with all airlines matching searchMap contents
	 */
	public void populatePieChartWithDelays(){
		DataDAO dataAccess;
		try
		{
			dataAccess = new DataDAO();
			Map<String,Integer> delayList = dataAccess.getAllDelaysWithCount();

			//System.err.println("count of sec"+ delayList.get("carrier"));


			DefaultPieDataset dataset = new DefaultPieDataset();
			for (Map.Entry<String, Integer> entry :delayList.entrySet())
			{
				dataset.setValue(entry.getKey(), entry.getValue());
			}


			JFreeChart pieChart = ChartFactory.createPieChart(
					"Delay Distribution",
					dataset,
					true, true, false  );

			parent.fullPanel.remove(parent.scrollPane);
			parent.delayPiePanel = new ChartPanel(pieChart);
			parent.fullPanel.add(parent.delayPiePanel);

		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}//populate delay pie chart

	/**
	 * Populates the basic only table with airport matching the searchMap content
	 * (can be made redundant by using the other airport populate function)
	 *
	 * <p>
	 * No need for handling of which panel is selected, only called on
	 * </p>
	 *
	 * <p>
	 * Handling of min/max fields is left to DAO/SQL layer
	 * </p>
	 */
	public void populateBasicTableWithAirport()

	{

		model.setRowCount(0); // This will remove all existing rows
		try (DataDAO dataAccess = new DataDAO())
		{
			List<Airport> airports;
			// if previously defaulted, setcto an airport specfic column
			if (parent.currentSortColumn.equals("date"))
			{
				parent.currentSortColumn = "iata_code";
			}
			System.out.println("fulldisplayM<inMax");
			airports = dataAccess.getBasicAirports(parent.searchMap, parent.currentPage, PAGE_SIZE, parent.currentSortColumn, parent.ascending);

			System.out.println("basic");
			model.setColumnIdentifiers(airportMinColumns, this);
			getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // flight count
			model.setRowsFromAirport(airports);
			// flightAccess.close();

		} catch (SQLException e)
		{
			e.printStackTrace();

		} catch (Exception f)
		{
			f.printStackTrace();
		}

	}// populate basic table with airport



	/**
	 * Populates either the full analysis table or basic flight count table with all
	 * airports matching the searchMap content, will also set the correct basic/full
	 * column headers
	 *
	 * @param fullDisplay true for full anlysis, false for basic flight count
	 */
	public void populateTableWithAirport(boolean fullDisplay)
	{

		try (DataDAO dataAccess = new DataDAO())
		{
			List<Airport> airports;
			if (fullDisplay)
			{
				Map<String,String> searchMap = parent.searchMap;
				System.out.println("fulldisplay");
				searchMap = new HashMap<>();
				searchMap.put("iata", parent.iataToSearch);
				searchMap.put("date", parent.dateToSearch);
				searchMap.put("airline", parent.airlineToSearch);
				model.setColumnIdentifiers(airportColumns, this);
				getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // delay orig
				getColumnModel().getColumn(4).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // delay dest
				getColumnModel().getColumn(8).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // total flights
				airports = dataAccess.searchAirports(searchMap,
						parent.currentPage,
						airportFullPageSize,
						parent.currentSortColumn,
						parent.ascending);
				model.setRowCount(0); // This will remove all existing rows, maynot be needed now, setcolumnIdent
				// now resets rows

				model.setRowsFromAirport(airports);
			}
			else
			{
				System.out.println("basic");
				if (parent.currentSortColumn.equals("date"))
				{
					parent.currentSortColumn = "a.iata_code";
				}
				System.out.println("poplatewithairpors, sortby: " + parent.currentSortColumn);
				model.setColumnIdentifiers(airportMinColumns, this);
				getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour));
				airports = dataAccess.getAllAirports(parent.currentPage, PAGE_SIZE, parent.currentSortColumn, parent.ascending);
				model.setRowCount(0); // This will remove all existing rows, may not be needed here, column clears
				// row

				model.setRowsFromAirport(airports);

			}

		} catch (SQLException e)
		{
			e.printStackTrace();

		} catch (Exception f)
		{
			f.printStackTrace();
		}

	}// populate with airport
	
	/**
	 * Extends cell renderer intended to highlight a column with the supplied colour
	 */
	public static class CustomColumnColorRenderer extends DefaultTableCellRenderer
	{
		/**
		 *
		 */
		private static final long serialVersionUID = -5707093970149887588L;
		Color colour; //(dev note: bad effect of being a dev is now i often misspell colour with no 'u' !!! do one)


		/**
		 * Constructor
		 * @param colour color to set the associated column to
		 */
		public CustomColumnColorRenderer(Color colour) {
			this.colour=colour;
		}

		/**
		 * Custom graphical object (cell/column from table), with a colour to supply.
		 *
		 * Sets the column/component to the supplied colour
		 *
		 *
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column)
		{

			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (!isSelected)
			{

				c.setBackground(colour);
				// text readability? should it be white or black to match other columns?
			}

			return c;
		}
	}

}
