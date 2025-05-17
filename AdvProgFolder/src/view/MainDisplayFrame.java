package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import model.DataDAO;
import model.DataDAO.AirlineWithData;
import model.DataDAO.Airport;
import model.DataDAO.FlightWithDelay;

/**
 * The main Frame containing the data table(analysis) and user function panels
 *
 * Monolithic, future enh. to modularise
 */
public class MainDisplayFrame extends JFrame
{

	/*
	 * -------------------------------------------------------
	 * -----------Members-------------------------------------
	 * -------------------------------------------------------
	 */

	private static final long					serialVersionUID		= -5786420551398119288L;
	private final String						APP_TITLE				= "Flight Analyser";
	private final int							PAGE_SIZE				= 50;
	private int									airportFullPageSize		= 20;
	private final int							AIRPORT_FULL_FIELD_SIZE	= 8;
	private int									currentPage				= 0;
	private CustomTableModel					tableModel;
	private JLabel								pageLabel;
	private JTable								table;
	private TableRowSorter<DefaultTableModel>	sorter;
	private JPanel fullPanel ;
	private JButton								prevBtn					= new JButton("Previous");
	private ChartPanel delayPiePanel;
	private JButton								nextBtn					= new JButton("Next");
	private Map<Integer, FlightWithDelay>				rowFlightMap			= new HashMap<>();
	private String[]									viewOptions				= new String[]
			{
					"Flights",
					"Airline (~10sec)",
					"Airport - Basic Flight Count (~5sec)",
					"Airport - Analysis (10-45sec)",
					"Delay Reason Pie"
			};


	private JPanel										userFuncPanel;

	private Comparator<Object>					numericComparator		= (o1, o2) ->
	{
		try
		{
			int i1 = Integer.parseInt(
					o1.toString().split("\\.")[0]);
			int i2 = Integer.parseInt(
					o2.toString().split("\\.")[0]);
			return Integer.compare(i1, i2);
		} catch (NumberFormatException e)
		{
			return o1.toString().compareTo(
					o2.toString());
		}
	};

	private String[]								flightColumns			= new String[]
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

	private String[]								airportColumns			=
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
	private String[]								airlineColumns			=
		{
				"iata_code",
				"name",
				"Avg Delay",
				"Total Delay",
				"Num Flights"
		};

	private String[]								airportMinColumns		=
		{
				"iata_code",
				"name",
				"Flights"
		};

	private String								searchType				= "";
	private String								searchTerm				= "";


	// tested for none null to activate search filters
	private String								dateToSearch			= null;
	private String								iataToSearch			= null;
	private String								airlineToSearch			= null;


	/*
	 * ------------------------------------
	 * ----------Public--------------------
	 * ------------------------------------
	 */
	public JPanel								mainPanel;

	public JComboBox<String>					viewSelectorComboBox	= new JComboBox<>(viewOptions);
	public String								currentSortColumn		= "flight id";									// Default
	// sort
	public boolean								ascending				= true;

	public boolean										searchActive			= false;
	public Map<String, String>							searchMap				= new HashMap<>();
	private JScrollPane scrollPane;

	public CardLayout							cardLayout;

	/*
	 * --------------------------------------------------------
	 * -----------Constructor----------------------------------
	 * ---------------------------------------------------------
	 */
	public MainDisplayFrame()
	{
		initialise();
	}

	public MainDisplayFrame(String name)
	{
		super(name);
		initialise();
	}

	/*
	 * -------------------------------------------------------
	 * -----------Methods-------------------------------------
	 * -------------------------------------------------------
	 */

	/**
	 * -----------------------------------------
	 * Set up of the frame:
	 * ------------------------------------------
	 * - close option
	 * - size
	 * - table model
	 * - main panel and layout
	 * - subpanels (twoRow,userFunc,controlPanel)
	 * - set selector and header listeners - add content to self
	 *
	 * set visible left to parent
	 */
	public void initialise()
	{
		setTitle(APP_TITLE);
		// setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// scale the size on screen-size, originally 1600x1000 hard coded tested
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int) (screenSize.width * 0.9); // 90% width
		int height = (int) (screenSize.height * 0.9); // 90% height
		setSize(width, height);

		// prevent editing
		tableModel = new CustomTableModel();
		table = new JTable(tableModel);

		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Get the row and column where the mouse was pressed
				int row = table.rowAtPoint(e.getPoint());

				if (row >= 0)
				{
					FlightWithDelay flight = rowFlightMap.get(row); // flight must be in the same order as
					System.out.println("Row clicked: " + flight.flightId);
				}
			}
		});

		// instantiate now we have a new table model
		sorter = new TableRowSorter<>(tableModel);

		// set main layout
		cardLayout = new CardLayout();
		mainPanel = new JPanel(cardLayout);

		/*
		 * ---popualte user function area panel
		 */
		userFuncPanel = new JPanel();
		userFuncPanel.add(viewSelectorComboBox); // combobox

		setFlightsPanel(); // adds buttons and text fields for flights

		JPanel importRow = new JPanel();
		JButton importCSVBtn = new JButton("Import CSV data"); // for any time import of CSV
		importCSVBtn.addActionListener(new CSVActionListener(this));
		importRow.add(importCSVBtn);

		JPanel twoRowPanel = new JPanel(new BorderLayout());
		twoRowPanel.add(importRow, BorderLayout.NORTH);
		twoRowPanel.add(userFuncPanel, BorderLayout.SOUTH);

		fullPanel = new JPanel(new BorderLayout());
		fullPanel.add(twoRowPanel, BorderLayout.NORTH);

		// main table opanel (center)
		scrollPane = new JScrollPane(table);
		fullPanel.add(scrollPane, BorderLayout.CENTER);

		pageLabel = new JLabel();
		prevBtn.addActionListener(new PageButtonActionListener("prev"));
		nextBtn.addActionListener(new PageButtonActionListener("next"));
		currentSortColumn = "date";
		populateTableWithAllFlights(currentSortColumn, true);

		// Control panel for pagination
		JPanel pageControlPanel = new JPanel();
		pageControlPanel.add(prevBtn);
		pageControlPanel.add(nextBtn);
		pageControlPanel.add(pageLabel);

		fullPanel.add(pageControlPanel, BorderLayout.SOUTH);

		mainPanel.add(fullPanel, "TABLE V"); // default
		mainPanel.add(new FullSearchPanel(this), "FULL_SEARCH"); // extra view

		setViewSelectorListener(); // set listener to switch tables/panels/view when choosing options
		setTableHeaderListener();

		add(mainPanel);
	}
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
		this.searchType = type;
		this.searchTerm = term;

		searchMap = new HashMap<>();

		searchMap.put(searchType, searchTerm);

		populateTableWithFlightSearch(sortBy, ascending);
	}
	private final Color highlightColour = new Color(245,245,240);
	/**
	 * Populate the table based off all flights in the database
	 *
	 * @param sortBy    database field to sort returned results from e.g. "date"
	 * @param ascending true or false, converted to ASC/DESC in SQL.
	 */
	public void populateTableWithAllFlights(String sortBy, boolean ascending)
	{
		searchActive = false;
		tableModel.setColumnIdentifiers(flightColumns, table);

		//total time column highlight

		table.getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour));

		table.getColumnModel().getColumn(4).setCellRenderer(new CustomColumnColorRenderer(highlightColour));

		table.getColumnModel().getColumn(5).setCellRenderer(new CustomColumnColorRenderer(highlightColour));
		table.getColumnModel().getColumn(10).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); //reason
		table.getColumnModel().getColumn(11).setCellRenderer(new CustomColumnColorRenderer(highlightColour));// total delay
		try (DataDAO dataAccess = new DataDAO();)
		{
			// flightAccess = new FlightDAO();
			try
			{
				if (searchMap.containsKey("flight id"))
				{
					System.err.println("should go hgere change to fsort by flight id");
				}
				// flightAccess = new FlightDAO();
				List<FlightWithDelay> flights = dataAccess.getAllFlights(currentPage, PAGE_SIZE, sortBy, ascending);

				tableModel.setRowsFromFlights(flights, rowFlightMap);
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
	}

	/**
	 * Populate table with all flights matching searchMap contents
	 *
	 * @param sortBy    database field to sort returned results from e.g. "date"
	 * @param ascending true or false, converted to ASC/DESC in SQL.
	 */
	public void populateTableWithFlightSearch(String sortBy, boolean ascending)
	{
		System.out.println("pop with search");
		searchActive = true;
		try (DataDAO dataAccess = new DataDAO())
		{
			// Map<String,String> search = new HashMap<>();
			List<FlightWithDelay> flights = dataAccess.getFlightBySearch(searchMap,
					currentPage,
					PAGE_SIZE,
					sortBy,
					ascending);

			tableModel.setRowsFromFlights(flights, rowFlightMap);
		} catch (SQLException e)
		{
			System.err.println("Error populating ");
			e.printStackTrace();

		} catch (Exception f)
		{
			System.err.println("Error from creating flight dao, populatWithSearch");
			f.printStackTrace();
		}
	}

	/**
	 * Populates table with all airlines matching searchMap contents
	 */
	private void populatePieChartWithDelays(){
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

			fullPanel.remove(scrollPane);
			delayPiePanel = new ChartPanel(pieChart);
			fullPanel.add(delayPiePanel);

		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public void populateTableWithAirline()
	{
		try
		{
			System.out.println("popuylatewithairlines");

			DataDAO dataAccess = new DataDAO();

			List<AirlineWithData> airlines = dataAccess.getAirlines(searchMap);

			// setColumnsFromAirLines(airlines);


			tableModel.setRowCount(0); // This will remove all existing rows


			tableModel.setColumnIdentifiers(airlineColumns, table);

			table.getColumnModel().getColumn(4).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // total flights
			table.getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // avg delay
			tableModel.setRowsFromAirlines(airlines);
			// Create the JTable with the table model

			dataAccess.close();
		} catch (SQLException e)
		{
			e.printStackTrace();

		} catch (Exception f)
		{
			f.printStackTrace();
		}
	}

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
				System.out.println("fulldisplay");
				searchMap = new HashMap<>();
				searchMap.put("iata", iataToSearch);
				searchMap.put("date", dateToSearch);
				searchMap.put("airline", airlineToSearch);
				tableModel.setColumnIdentifiers(airportColumns, table);
				table.getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // delay orig
				table.getColumnModel().getColumn(4).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // delay dest
				table.getColumnModel().getColumn(8).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // total flights
				airports = dataAccess.searchAirports(searchMap,
						currentPage,
						airportFullPageSize,
						currentSortColumn,
						ascending);
				tableModel.setRowCount(0); // This will remove all existing rows, maynot be needed now, setcolumnIdent
				// now resets rows

				tableModel.setRowsFromAirport(airports);
			}
			else
			{
				System.out.println("basic");
				if (currentSortColumn.equals("date"))
				{
					currentSortColumn = "a.iata_code";
				}
				System.out.println("poplatewithairpors, sortby: " + currentSortColumn);
				tableModel.setColumnIdentifiers(airportMinColumns, table);
				table.getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour));
				airports = dataAccess.getAllAirports(currentPage, PAGE_SIZE, currentSortColumn, ascending);
				tableModel.setRowCount(0); // This will remove all existing rows, may not be needed here, column clears
				// row

				tableModel.setRowsFromAirport(airports);

			}

		} catch (SQLException e)
		{
			e.printStackTrace();

		} catch (Exception f)
		{
			f.printStackTrace();
		}

	}

	/**
	 * Populates the basic only table with airport matching the searchMap content
	 * (can be made redunant by using the other airport populate function)
	 *
	 * <p>
	 * No need for handling of which panel is selected, only called on
	 * </p>
	 *
	 * <p>
	 * Handling of min/max fields is left to DAO/SQL layer
	 * </p>
	 */
	private void populateBasicTableWithAirport()

	{

		tableModel.setRowCount(0); // This will remove all existing rows
		try (DataDAO dataAccess = new DataDAO())
		{
			List<Airport> airports;
			// if previously defaulted, setcto an airport specfic column
			if (currentSortColumn.equals("date"))
			{
				currentSortColumn = "iata_code";
			}
			System.out.println("fulldisplayM<inMax");
			airports = dataAccess.getBasicAirports(searchMap, currentPage, PAGE_SIZE, currentSortColumn, ascending);

			System.out.println("basic");
			tableModel.setColumnIdentifiers(airportMinColumns, table);
			table.getColumnModel().getColumn(2).setCellRenderer(new CustomColumnColorRenderer(highlightColour)); // flight count
			tableModel.setRowsFromAirport(airports);
			// flightAccess.close();

		} catch (SQLException e)
		{
			e.printStackTrace();

		} catch (Exception f)
		{
			f.printStackTrace();
		}

	}

	/*
	 * -------------------------------------------------------
	 *  -----------Set User Function Panels ------------------
	 *  -------- (the top row of user functionality) ---------
	 *  ----------------------------------------------
	 *  - airline
	 *  - airport basic
	 *  - airport full
	 *  - flights
	 *  ----------------------------------------------
	 */

	/**
	 * Set the user function panel for airline
	 */
	private void setDelayPanel() {
		userFuncPanel.removeAll();
		userFuncPanel.add(viewSelectorComboBox);
		populatePieChartWithDelays();
		userFuncPanel.revalidate();
		userFuncPanel.repaint();
	}
	private void setAirlinePanel()
	{


		if(delayPiePanel!=null)
		{
			fullPanel.remove(delayPiePanel);
			fullPanel.add(scrollPane);
		}
		userFuncPanel.removeAll();
		userFuncPanel.add(viewSelectorComboBox);
		JLabel yearLabel = new JLabel("Date of Flight ([YYYY] / [YYYY-MM] / [YYYY-MM-DD])");
		userFuncPanel.add(yearLabel);
		JTextField yearField = new JTextField(6);
		// ((AbstractDocument) yearField.getDocument()).setDocumentFilter(new
		// NumberOnlyFilter());
		yearField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (!yearField.getText().isEmpty())
				{
					searchMap.put("date", yearField.getText());
				}
				else
				{
					searchMap.remove("date");
				}
			}
		});
		userFuncPanel.add(yearField);

		JButton searchBtn = new JButton("Recalculate");
		searchBtn.addActionListener((ActionEvent e) ->
		{
			if (!yearField.getText().isEmpty())
			{
				System.out.println("working");
				populateTableWithAirline();
			}

		});

		userFuncPanel.add(searchBtn);
		userFuncPanel.revalidate();
		userFuncPanel.repaint();
	}

	/**
	 * Set the user function panel for airport basic flight count (minimum detail
	 * but more content in function panel)
	 */
	private void setAirportBasicPanel()
	{

		if(delayPiePanel!=null)
		{
			fullPanel.remove(delayPiePanel);
			fullPanel.add(scrollPane);
		}
		userFuncPanel.removeAll();
		// searchPanel.add(options);
		// searchPanel.add(new JLabel(" | "));

		JLabel minLabel = new JLabel("Min Flights:");
		JTextField minField = new JTextField(6); // set size to roughly 20 characters
		((AbstractDocument) minField.getDocument()).setDocumentFilter(new NumberOnlyFilter());
		JLabel maxLabel = new JLabel("Max Flights:");
		JTextField maxField = new JTextField(6);
		((AbstractDocument) minField.getDocument()).setDocumentFilter(new NumberOnlyFilter());
		JButton searchBtn = new JButton("Recalculate");

		JLabel originLabel = new JLabel("Origin Code:");
		JTextField originField = new JTextField(6);

		JLabel destLabel = new JLabel("Dest Code:");
		JTextField destField = new JTextField(6);

		JLabel airlineLabel = new JLabel("Airline Code");
		JTextField airlineField = new JTextField(6);
		JLabel dateLabel = new JLabel("Date:");
		JTextField dateField = new JTextField(8);

		searchBtn.addActionListener(e ->
		{
			boolean added = false;
			// atleast one none empty
			if (!minField.getText().isEmpty() || !maxField.getText().isEmpty() || !originField.getText().isEmpty()
					|| !destField.getText().isEmpty() || !airlineField.getText().isEmpty()
					|| !dateField.getText().isEmpty())
			{
				added = true;
				// remove any old searches now we have content
				searchMap = new HashMap<>();
			}

			// test each field individually
			if ((minField.getText() != null && !minField.getText().isEmpty()))
			{
				searchMap.put("min", minField.getText());
			}

			if ((maxField.getText() != null && !maxField.getText().isEmpty()))
			{

				searchMap.put("max", maxField.getText());

			}

			if ((originField.getText() != null && !originField.getText().isEmpty()))
			{

				searchMap.put("origin", originField.getText());
			}

			if ((destField.getText() != null && !destField.getText().isEmpty()))
			{

				searchMap.put("destination", destField.getText());

			}

			if ((airlineField.getText() != null && !airlineField.getText().isEmpty()))
			{

				searchMap.put("airline code", airlineField.getText());

			}
			if ((dateField.getText() != null && !dateField.getText().isEmpty()))
			{

				searchMap.put("date", dateField.getText());

			}

			searchActive = added;
			if (searchActive)
			{
				populateBasicTableWithAirport();

			}
			else
			{
				// System.err.println("not calling anything on search pressed");
				searchMap.clear();
				populateTableWithAirport(false);
			}
		});

		// dont need in memory jlabel vars duh!

		userFuncPanel.add(viewSelectorComboBox);

		userFuncPanel.add(minLabel);
		userFuncPanel.add(minField);
		userFuncPanel.add(maxLabel);
		userFuncPanel.add(maxField);
		userFuncPanel.add(new JLabel(" | "));
		userFuncPanel.add(new JLabel("Restric to set"));

		userFuncPanel.add(new JLabel(" | "));
		userFuncPanel.add(originLabel);
		userFuncPanel.add(originField);

		userFuncPanel.add(destLabel);
		userFuncPanel.add(destField);
		userFuncPanel.add(airlineLabel);
		userFuncPanel.add(airlineField);
		userFuncPanel.add(dateLabel);
		userFuncPanel.add(dateField);

		userFuncPanel.add(searchBtn);

		userFuncPanel.revalidate(); // Recalculates the layout
		userFuncPanel.repaint();
	}

	/**
	 * Set the user function panel to be that for the full airport display (various
	 * stat details but little panel content)
	 */
	private void setAirportFullPanel()
	{

		if(delayPiePanel!=null)
		{
			fullPanel.remove(delayPiePanel);
			fullPanel.add(scrollPane);
		}

		userFuncPanel.removeAll();
		userFuncPanel.add(viewSelectorComboBox);
		JLabel yearLabel = new JLabel("Date");


		JTextField yearField = new JTextField(AIRPORT_FULL_FIELD_SIZE);
		// ((AbstractDocument) yearField.getDocument()).setDocumentFilter(new
		// NumberOnlyFilter());
		yearField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (!yearField.getText().isEmpty())
				{
					// searchByDate = true;
					dateToSearch = yearField.getText();
				}
				else
				{
					// searchByDate = false;
					dateToSearch = null;
				}
			}
		});


		JLabel iataLabel = new JLabel("Airport Code");


		JTextField iataField = new JTextField(AIRPORT_FULL_FIELD_SIZE);
		iataField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (!iataField.getText().isEmpty())
				{
					// searchByDate = true;
					iataToSearch = iataField.getText();
				}
				else
				{
					// searchByDate = false;
					iataToSearch = null;
				}
			}
		});


		JLabel airlineLabel = new JLabel("Airline");

		JTextField airlineField = new JTextField(AIRPORT_FULL_FIELD_SIZE);


		airlineField.addFocusListener(new FocusAdapter()
		{

			@Override
			public void focusLost(FocusEvent e)
			{
				if (!airlineField.getText().isEmpty())
				{
					airlineToSearch = airlineField.getText();
				}
				else
				{
					// searchByDate = false;
					airlineToSearch = null;
				}
			}
		});

		// add after button but declare here,
		JTextField pageField = new JTextField(AIRPORT_FULL_FIELD_SIZE);

		JButton recalcBtn = new JButton("Recalculate");
		recalcBtn.addActionListener((ActionEvent e) ->
		{

			if (!pageField.getText().isEmpty())
			{
				airportFullPageSize = Integer.parseInt(pageField.getText());
			}

			// do in backgreoeund to allow progress button

			System.err.println("import pressed");
			
			Runnable task = () -> {
				// TIME CONSUMING
				if (dateToSearch != null || iataToSearch != null || airlineToSearch != null)
				{
					populateTableWithAirport(true);
				}
				else
				{
					searchMap.clear();
					populateTableWithAirport(true);
				}
			};
			
			runWithLoadingLabel(task, null, "Searching....");

			// else no action
		});

		userFuncPanel.add(iataLabel);
		userFuncPanel.add(iataField);
		userFuncPanel.add(yearLabel);
		userFuncPanel.add(yearField);
		userFuncPanel.add(airlineLabel);
		userFuncPanel.add(airlineField);
		userFuncPanel.add(recalcBtn);

		userFuncPanel.add(new JLabel("Page Size:"));
		pageField.setText(""+airportFullPageSize);
		userFuncPanel.add(pageField);

		userFuncPanel.revalidate(); // Recalculates the layout
		userFuncPanel.repaint();
	}

	/**
	 * Set the user function panel to be that for Flights.
	 */
	private void setFlightsPanel()
	{
		// reset from delay//
		if(delayPiePanel !=null) {
			fullPanel.remove(delayPiePanel);
			fullPanel.add(scrollPane);
		}
		userFuncPanel.removeAll();

		JLabel termLabel = new JLabel("Search:");
		JTextField term = new JTextField(20); // set size to roughly 20 characters
		JLabel in = new JLabel("In:");
		String[] dropdownOptions = new String[flightColumns.length + 1];
		dropdownOptions[0] = "All";
		System.arraycopy(flightColumns, 0, dropdownOptions, 1, flightColumns.length);
		JComboBox<String> columnDropdown = new JComboBox<>(dropdownOptions);

		JButton searchBtn = new JButton("Search");
		searchBtn.addActionListener(e ->
		{
			// searchType=;
			searchTerm = term.getText();
			String realColumnName = columnDropdown.getSelectedItem().toString().toLowerCase();
			if (realColumnName.toLowerCase().equals("origin"))
			{
				realColumnName = "flight_origin";
			}
			System.out.println(
					"type " + columnDropdown.getSelectedItem().toString().toLowerCase() + " term " + term.getText());
			populateTableWithSingleSearchTerm(columnDropdown.getSelectedItem().toString().toLowerCase(),
					term.getText(),
					currentSortColumn,
					true);
		});

		JButton fullSearchButton = new JButton("Full Search");
		fullSearchButton.addActionListener(e ->
		{
			term.setText("");
			cardLayout.show(mainPanel, "FULL_SEARCH");
		});

		userFuncPanel.add(viewSelectorComboBox);
		userFuncPanel.add(new JLabel(" | "));
		userFuncPanel.add(termLabel);
		userFuncPanel.add(term);
		userFuncPanel.add(in);
		userFuncPanel.add(columnDropdown);
		userFuncPanel.add(searchBtn);
		userFuncPanel.add(new JLabel(" | "));
		userFuncPanel.add(fullSearchButton);
		userFuncPanel.revalidate(); // Recalculates the layout
		userFuncPanel.repaint();
	}

	/*
	 * -------------------------------------------------------
	 * -----------Set Listeners-------------------------------
	 * -------------------------------------------------------
	 * - column headers listener
	 * - view options listener
	 * -------------------------------------------------------
	 */

	/**
	 * Action listener for special sorting actions on clicking column headers i.e.
	 * recall populateTableWith... to request a new sort order and and by returned from db
	 */
	private void setTableHeaderListener()
	{
		table.getTableHeader().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				System.err.println("mouse clicked on header");
				int columnIndex = table.columnAtPoint(e.getPoint());
				if (columnIndex >= 0)
				{
					String columnName = table.getColumnName(columnIndex);
					// currentSortColumn = columnName.toLowerCase().replace("
					// ", "_");
					// Convert display name to database field name (e.g., Flight
					// ID -> flight_id)
					String selectedColumn = columnName.toLowerCase().replace(" ", "_");
					String order = ascending ? "ASC" : "DESC";
					System.out.println("was " + order + "now opposite");

					ascending = !ascending;
					// Toggle order if same column clicked

					currentSortColumn = selectedColumn;

					currentPage = 0;
					JTableHeader header = table.getTableHeader();
					TableColumnModel colModel = header.getColumnModel();
					/*
					 * FUTURE OVERSITE would be missing re populate call by airlines returning >50,
					 * currently okay only 18 in db
					 *
					 */

					//restricting to non-airline selected
					if (!viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("line"))
					{
						for (int i = 0; i < colModel.getColumnCount(); i++)
						{
							String baseName = flightColumns[i];
							String displayName = baseName;

							String columnId = baseName.toLowerCase().replace(" ", "_");
							if (columnId.equals(currentSortColumn))
							{
								displayName += ascending ? " ▲" : " ▼";
								System.out.println(displayName);
							}

							colModel.getColumn(i).setHeaderValue(displayName);
						}
						// Call once after all updates
						table.getTableHeader().revalidate();
						table.getTableHeader().repaint();
						header.repaint();

						// tableModel.setRowCount(0);

						// cater for difference in headers vs sql table/columns
						if (selectedColumn.toLowerCase().contains("origin"))
						{
							currentSortColumn = "flight_origin";
						}
						if (selectedColumn.toLowerCase().contains("flights"))
						{
							currentSortColumn = "flight_count";
						}
						if (selectedColumn.toLowerCase().contains("delay_reason"))
						{
							currentSortColumn = "reason";
						}
						if (selectedColumn.toLowerCase().contains("time"))
						{
							currentSortColumn = "delay_length";
						}
						if (selectedColumn.toLowerCase().contains("destination"))
						{
							currentSortColumn = "flight_destination";
						}
						Runnable task = () -> {

							if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("flights"))
							{
								if (searchActive)
								{

									populateTableWithFlightSearch(currentSortColumn, ascending);
								}
								else
								{
									populateTableWithAllFlights(currentSortColumn, ascending);
								}

							}
							else if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains(
									"analysis"))
							{
								/*
								 * cater for difference ebteween column identifiers and db fields
								 */
								if (currentSortColumn.toLowerCase().contains("delay orig"))
								{
									currentSortColumn = "avg_delay_orig";
								}
								else if (currentSortColumn.toLowerCase().contains("delay dest"))
								{
									currentSortColumn = "avg_delay_dest";
								}
								else if (currentSortColumn.toLowerCase().contains("total delay dest"))
								{
									currentSortColumn = "total_delay_dest";
								}
								else if (currentSortColumn.toLowerCase().contains("total delay orig"))
								{
									currentSortColumn = "total_delay_orig";
								}
								else if (currentSortColumn.toLowerCase().contains("most_airline"))
								{
									currentSortColumn = "most_frequent";
								}
								else if (currentSortColumn.toLowerCase().contains("least_airline"))
								{
									currentSortColumn = "least_frequent";
								}

								populateTableWithAirport(true);
							}
							else if (!searchActive)
							{
								populateTableWithAirport(false);
							}
							else
							{
								populateBasicTableWithAirport();
							}

						}; // runnable task
						
						runWithLoadingLabel(task, null, "Reording...");
						
//						JDialog loading = new JDialog(null, "Please wait...", Dialog.ModalityType.APPLICATION_MODAL);
//						loading.setSize(420, 80);
//						loading.setLocationRelativeTo(null);
//						JLabel loadingLabel = new JLabel("Reording...");
//						loadingLabel.setVerticalAlignment(SwingConstants.TOP);
//						loadingLabel.setHorizontalAlignment(SwingConstants.LEFT);
//
//						int[] secondsElapsed = {0};
//						loading.setLayout(new BorderLayout());
//						loading.add(loadingLabel, BorderLayout.CENTER);
//						// Use an explicit ActionListener for Java 8 compatibility
//						ActionListener updateLabel = new ActionListener()
//						{
//							@Override
//							public void actionPerformed(ActionEvent e)
//							{
//								secondsElapsed[0]++;
//								loadingLabel.setText("Loading..." 
//								 + "[" + secondsElapsed[0] + "s]");
//								if(secondsElapsed[0]>10) {
//									loading.setSize(320,200);
//									loadingLabel.setText("<html>"+loadingLabel.getText()+"<br> Quite slow, check other processes for acccess to db. <br>"
//											+ "Previous Run / OneDrive </html>");
//									
//								}
//							}
//						};
//
//						final Timer timer = new Timer(1000, updateLabel);
//						timer.start();
//
//						//loadingLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//						loading.getContentPane().add(loadingLabel);
//						loading.pack();
//						loading.setLocationRelativeTo(null);
//						loading.setSize(250,80);
//
//						SwingWorker<Void, Void> worker = new SwingWorker<>()
//						{
//							@Override
//							protected Void doInBackground()
//							{
//
//								if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("flights"))
//								{
//									if (searchActive)
//									{
//
//										populateTableWithFlightSearch(currentSortColumn, ascending);
//									}
//									else
//									{
//										populateTableWithAllFlights(currentSortColumn, ascending);
//									}
//
//								}
//								else if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains(
//										"analysis"))
//								{
//									/*
//									 * cater for difference ebteween column identifiers and db fields
//									 */
//									if (currentSortColumn.toLowerCase().contains("delay orig"))
//									{
//										currentSortColumn = "avg_delay_orig";
//									}
//									else if (currentSortColumn.toLowerCase().contains("delay dest"))
//									{
//										currentSortColumn = "avg_delay_dest";
//									}
//									else if (currentSortColumn.toLowerCase().contains("total delay dest"))
//									{
//										currentSortColumn = "total_delay_dest";
//									}
//									else if (currentSortColumn.toLowerCase().contains("total delay orig"))
//									{
//										currentSortColumn = "total_delay_orig";
//									}
//									else if (currentSortColumn.toLowerCase().contains("most_airline"))
//									{
//										currentSortColumn = "most_frequent";
//									}
//									else if (currentSortColumn.toLowerCase().contains("least_airline"))
//									{
//										currentSortColumn = "least_frequent";
//									}
//
//									populateTableWithAirport(true);
//								}
//								else if (!searchActive)
//								{
//									populateTableWithAirport(false);
//								}
//								else
//								{
//									populateBasicTableWithAirport();
//								}
//								return null;
//							}
//
//							@Override
//							protected void done()
//							{
//								loading.dispose(); // close the popup
//							}
//						};
//
//						worker.execute();
//						loading.setVisible(true);

					}
					// else
					// if not contains 'line'
				} // colukmn index > 0
			}

		});
	}

	/**
	 * Set action listener for the view options to handle table updates and
	 * prev/next button state (e.g. where results lt50)
	 *
	 * Future enh.: create custom JCombo class and extrapolate (will need some
	 * params, probably a TableViewFrame (this))
	 */
	private void setViewSelectorListener()
	{
		// TODO Auto-generated method stub
		viewSelectorComboBox.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					currentPage=0;
					pageLabel.setText(""+currentPage);

					// reset lingering search flagS and data from previous screens/dropdown
					// selections
					searchActive = false;
					searchMap.clear();
					currentSortColumn = "date";

					String selectedItem = (String) e.getItem();
					tableModel.setRowCount(0);
					 Runnable task = () -> {
						 if (selectedItem.toLowerCase().contains("airline"))
							{
								setAirlinePanel(); // set function buttons

								populateTableWithAirline();

								// need numeric instead string sorter for number fields

								TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
								sorter.setComparator(4, numericComparator);// num flights
								sorter.setComparator(3, numericComparator);// total delay
								sorter.setComparator(2, numericComparator);// avg delay
								table.setRowSorter(sorter);
							}
							else if (selectedItem.toLowerCase().contains("flights"))
							{
								setFlightsPanel(); // set function buttons

								currentSortColumn = "date";

								sorter.setComparator(0, numericComparator);// Flight id
								populateTableWithAllFlights(currentSortColumn, true);
								TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);

								sorter.setComparator(11, numericComparator);// incorrect is airline code
							}
							else if (selectedItem.toLowerCase().contains("analysis"))
							{

								setAirportFullPanel();

								populateTableWithAirport(true); // the time consuming operation

								TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
								table.setRowSorter(sorter);
								sorter.setComparator(2, numericComparator);
								sorter.setComparator(3, numericComparator);
							}
							else if(selectedItem.toLowerCase().contains("basic"))
							{
								setAirportBasicPanel();
								populateTableWithAirport(false);

							}
							else{
								setDelayPanel();
							}
			            };
			            
					runWithLoadingLabel(task, e, "Loading...");
					
					if (tableModel.getRowCount() < 49 && !selectedItem.toLowerCase().contains("analysis"))
					{

						System.err.println("setting to false from options actionlist");
						nextBtn.setEnabled(false);
						prevBtn.setEnabled(false);
					}
					else
					{
						nextBtn.setEnabled(true);
						prevBtn.setEnabled(true);

					}
				}
			}

		});

	}

	/*
	 * -------------------------------------------------------
	 * -----------Util Classes--------------------------------
	 * -------------------------------------------------------
	 * - number only text filter class
	 * - page button action listener class
	 * -------------------------------------------------------
	 */

	/**
	 * Extend DocumentListener to only pass to super insert/replace if input is
	 * digits only i.e no text will be inserted into JComobBox if this is not the
	 * this case
	 *
	 * <p>
	 * Currently doesn't allow "-" charactwer for negative
	 * </p>
	 */
	public class NumberOnlyFilter extends DocumentFilter
	{

		// normal typing event
		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
				throws BadLocationException
		{
			if (string.matches("\\d+"))
			{
				super.insertString(fb, offset, string, attr);
			}
		}

		// highlighted then typed event
		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException
		{
			if (text.matches("\\d+"))
			{
				super.replace(fb, offset, length, text, attrs);
			}
		}
	}

	/**
	 * Action listener for page button, sets the current page +/- 1 depending on
	 * type used and also calls the populateTable... functions to retrieve next page
	 * from DB. Doesn't allow negative pages (stops at 0 first page)
	 *
	 * Future enh: calculate offset and allow negative, though probably not needed
	 * since user can sort by header to get the last/highest result anyway
	 *
	 */
	public class PageButtonActionListener implements ActionListener
	{
		String buttonType = null;

		/**
		 * Constructor; simply sets the member buttonType.
		 *
		 * @param buttonType "prev" or "next" will cause currentPage-- or currentPage++
		 *                   respectively
		 */
		public PageButtonActionListener(String buttonType)
		{
			this.buttonType = buttonType;
		}

		/**
		 * Click event
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			// check for null in case of duff caller
			if (buttonType != null && buttonType.contains("prev"))
			{
				if (currentPage > 0) // dont go negative
				{
					currentPage--;
				}
			}
			else
			{
				currentPage++;
			}

			if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("flights"))
			{
				if (searchActive)
				{
					populateTableWithFlightSearch(currentSortColumn, ascending);
				}
				else
				{
					populateTableWithAllFlights(currentSortColumn, ascending);

				}

			}
			else if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("line"))
			{
				populateTableWithAirline();
			}
			else if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("basic"))
			{
				populateTableWithAirport(false);
			}
			else
			{
				populateTableWithAirport(true);
				// loadFlights();
			}

			pageLabel.setText("" + currentPage); // hack, add emtpy string to get int as string?? shorter than Integer
			// parse int but probably not recc.
		}

	}

	/**
	 * Extends cell renderer intended to highlight a column with the supplied colour
	 */
	public class CustomColumnColorRenderer extends DefaultTableCellRenderer
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

			if (!isSelected) {
				c.setBackground(colour);
				// text readability? should it be white or black to match other columns?
			}

			return c;
		}
	}
	/**
	 * Attempt at getting the repeated loading screen work in background to be defined once,
	 * , able to pass code to a function in java? fantastic. 
	 * 
	 * @param task  the code to run on in backgorund
	 * @param event for the special cause of being on ViewOptions select
	 * @param baseInput label text
	 */
	public static void runWithLoadingLabel(Runnable task, ItemEvent event, String baseInput) 
	{
		
		JDialog loading = new JDialog(null, "Please wait...", Dialog.ModalityType.APPLICATION_MODAL);
		loading.setSize(420, 80);
		loading.setLocationRelativeTo(null);
		String base = new String(baseInput);
		//String base = "Loading..."+event.getItem();
		JLabel loadingLabel = new JLabel(base);
		if(event!=null)
			{
				base = base +event.getItem();
				loadingLabel.setText(base);
				
			}
		
		loadingLabel.setVerticalAlignment(SwingConstants.TOP);
		loadingLabel.setHorizontalAlignment(SwingConstants.LEFT);

		int[] secondsElapsed = {0};
		loading.setLayout(new BorderLayout());
		loading.add(loadingLabel, BorderLayout.CENTER);
		String finalBase = new String(base);
		//ItemEvent event = e;
		// Use an explicit ActionListener for Java 8 compatibility
		ActionListener updateLabel = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				secondsElapsed[0]++;
				loadingLabel.setText(finalBase
				 + "[" + secondsElapsed[0] + "s]");
				
				if(secondsElapsed[0]>5) {
					if(event!=null)
				
					{
						if((!event.getItem().toString().toLowerCase().contains("analysis")) || (secondsElapsed[0]>10 && event.getItem().toString().toLowerCase().contains("analysis")))
						{	

							loadingLabel.setText("<html>"+finalBase+"["+secondsElapsed[0]+"]"+"<br> Quite slow, check other processes for acccess to db. <br>"
									+ "Previous Run / OneDrive </html>");
					
							loading.setSize(420,160);
							loading.revalidate();
							loading.repaint();
					
						}	
					}//event not null					
				}//seconds >5
			}//actionperformed
		};//action list

		final Timer timer = new Timer(1000, updateLabel);
		timer.start();
		loading.add(loadingLabel);
		
		// **** need this bit before caller in calling code *******
		//tableModel.setRowCount(0);
		
		
		SwingWorker<Void, Void> csvWorker = new SwingWorker<>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				task.run();
				// Time-consuming operation
				return null;
			}

			@Override
			protected void done()
			{
				loading.dispose();
				// continueButton.setEnabled(true);
			}
			
		};//swing worker
	
		csvWorker.execute(); // worker.execute();
		loading.setVisible(true);
	}// void run with loading
	
	
}
