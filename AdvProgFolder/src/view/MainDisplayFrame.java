package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.jfree.chart.ChartPanel;

import model.DataDAO.FlightWithDelay;

/**
 * The main Frame containing the data table(analysis) and user function panels
 *
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

	private final int							AIRPORT_FULL_FIELD_SIZE	= 8;
	
	
	private CustomTableModel					tableModel;
	private JLabel								pageLabel;
	
	private TableRowSorter<DefaultTableModel>	sorter;

	/**
	 * The whole display
	 */
	
	private JButton								prevBtn					= new JButton("Previous");
	
	private JButton								nextBtn					= new JButton("Next");
	
	private String[]									viewOptions				= new String[]
			{
					"Flights",
					"Airline (~10sec)",
					"Airport - Basic Flight Count (~5sec)",
					"Airport - Analysis (10-45sec)",
					"Delay Reason Pie"
			};


	private JPanel										userFuncPanel;

	// for restrictinmg input to numbers
	private Comparator<Object>					numericComparator		= (o1, o2) ->
	{
		try
		{
			int i1 = Integer.parseInt(	o1.toString().split("\\.")[0]);
			int i2 = Integer.parseInt(  o2.toString().split("\\.")[0]);

			return Integer.compare(i1, i2);

		} catch (NumberFormatException e)
		{
			return o1.toString().compareTo(o2.toString());
		}
	};


	
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

	// quick search flag
	public boolean								searchActive			= false;
	
	public Map<String, String>					searchMap				= new HashMap<>();
	
	public JScrollPane scrollPane;

	public CardLayout							cardLayout;

	// tested for none null to activate search filters
	public String								dateToSearch			= null;
	public String								iataToSearch			= null;
	public String								airlineToSearch			= null;

	// public now populate methods moved to own table class
	public int									currentPage				= 0;
	public CustomJTable								table;
	public JPanel fullPanel ;
	public ChartPanel delayPiePanel;
	public Map<Integer, FlightWithDelay>				rowFlightMap			= new HashMap<>();
	
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
	 * - set selector and header listeners
	 * - add content to self
	 *
	 * (set visible left to parent)
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
		table = new CustomJTable(tableModel,this);

		// instantiate now we have a new table model
		sorter = new TableRowSorter<>(tableModel);

		// set main layout
		cardLayout = new CardLayout();
		mainPanel = new JPanel(cardLayout);

		/*
		 * ---populate user function area panel
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
		table.populateTableWithAllFlights(currentSortColumn, true);

		// Control panel for pagination
		JPanel pageControlPanel = new JPanel();
		pageControlPanel.add(prevBtn);
		pageControlPanel.add(nextBtn);
		pageControlPanel.add(pageLabel);

		fullPanel.add(pageControlPanel, BorderLayout.SOUTH);

		mainPanel.add(fullPanel, "TABLE V"); // default
		mainPanel.add(new FullSearchPanel(this), "FULL_SEARCH"); // extra view

		setViewSelectorListener(); // set listener to switch tables/panels/view when choosing options
		table.setTableRowListener();
		table.setTableHeaderListener();

		add(mainPanel);
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
		JLabel thisSearch =new JLabel("All");
		
		JLabel airlineLabel =new JLabel("Airline (iata only):");
		userFuncPanel.add(airlineLabel);
		JTextField input = new JTextField(6);
		userFuncPanel.add(input);
		JButton search = new JButton("Recaluclate");
		userFuncPanel.add(search);
		search.addActionListener((ActionEvent e) ->
		{

			Runnable task = ()->{
				table.populatePieChartWithAirlineDelay(input.getText());
			};
			
			runWithLoadingLabel(task, null, "Calulating pie chart");
			thisSearch.setText(input.getText().isEmpty()?"All":input.getText());
			userFuncPanel.revalidate();
			userFuncPanel.repaint();
			// else no action
		});
		userFuncPanel.add(thisSearch);
		table.populatePieChartWithDelays();
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
				table.populateTableWithAirline();
			}

		});

		userFuncPanel.add(searchBtn);
		userFuncPanel.revalidate();
		userFuncPanel.repaint();
	}//set airline panel

	/**
	 * Set the user function panel for airport basic flight count (minimum stats detail
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
				table.populateBasicTableWithAirport();

			}
			else
			{
				// System.err.println("not calling anything on search pressed");
				searchMap.clear();
				table.populateTableWithAirport(false);
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
	}// set airport basic panel

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
				table.airportFullPageSize = Integer.parseInt(pageField.getText());
			}

			// do in backgreoeund to allow progress button

			System.err.println("import pressed");

			Runnable task = () -> {
				// TIME CONSUMING
				if (dateToSearch != null || iataToSearch != null || airlineToSearch != null)
				{
					table.populateTableWithAirport(true);
				}
				else
				{
					searchMap.clear();
					table.populateTableWithAirport(true);
				}
			};

			runWithLoadingLabel(task, null, "Recalculating/Restricting...");

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
		pageField.setText(""+table.airportFullPageSize);
		userFuncPanel.add(pageField);

		userFuncPanel.revalidate(); // Recalculates the layout
		userFuncPanel.repaint();
	}// set airport panel

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
		String[] dropdownOptions = new String[table.flightColumns.length + 1];
		dropdownOptions[0] = "All";
		System.arraycopy(table.flightColumns, 0, dropdownOptions, 1, table.flightColumns.length);
		JComboBox<String> columnDropdown = new JComboBox<>(dropdownOptions);

		JButton searchBtn = new JButton("Search");
		searchBtn.addActionListener(e ->
		{
			// searchType=;
			//	searchTerm = term.getText();
			String realColumnName = columnDropdown.getSelectedItem().toString().toLowerCase();
			if (realColumnName.toLowerCase().equals("origin"))
			{
				realColumnName = "flight_origin";
			}
			System.out.println(
					"type " + columnDropdown.getSelectedItem().toString().toLowerCase() + " term " + term.getText());
			Runnable task = ()-> {
				table.populateTableWithSingleSearchTerm(columnDropdown.getSelectedItem().toString().toLowerCase(),
						term.getText(),
						currentSortColumn,
						true);
			};

			runWithLoadingLabel(task, null, "Searching..."+term.getText());

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
	}// set flight panel

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
	 * recall populateTableWith... to request a new 'sort order' and and 'sort by' returned from db
	 */

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
					Runnable task = () ->
					{
						if (selectedItem.toLowerCase().contains("airline"))
						{
							setAirlinePanel(); // set function buttons

							table.populateTableWithAirline();

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
							table.populateTableWithAllFlights(currentSortColumn, true);
							TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);

							sorter.setComparator(11, numericComparator);// incorrect is airline code
						}
						else if (selectedItem.toLowerCase().contains("analysis"))
						{

							setAirportFullPanel();

							table.populateTableWithAirport(true); // the time consuming operation

							TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
							table.setRowSorter(sorter);
							sorter.setComparator(2, numericComparator);
							sorter.setComparator(3, numericComparator);
						}
						else if(selectedItem.toLowerCase().contains("basic"))
						{
							setAirportBasicPanel();
							table.populateTableWithAirport(false);

						}
						else{
							setDelayPanel();
						}

					};// runnable



					JTableHeader header = table.getTableHeader();

					header.setToolTipText(""); // enables

					String[] tips = {
							"<html>Avg arrival delay when Origin Airport. <br> Not the Departure delay.<html>",
							"<html>Total arrival delay when Origin Airport. <br> Not the departure delay.<html>",
							"Avg arrival delay when Destination Airport. ",
							"Total delay when Destination Airport",
					};
					// Override getToolTipText to show column-specific tooltips
					header.addMouseMotionListener(new MouseMotionAdapter()
					{

						@Override
						public void mouseMoved(MouseEvent e) {

							JTableHeader h = (JTableHeader) e.getSource();

							TableColumnModel colModel = h.getColumnModel();

							int column = colModel.getColumnIndexAtX(e.getX());
							int tipStartOffset = 2;

							if (column >= tipStartOffset && column < tips.length+tipStartOffset)
							{

								h.setToolTipText(tips[column-tipStartOffset]);
							} else
							{

								h.setToolTipText(null);
							}
						}
					});
					runWithLoadingLabel(task, e, "Loading...");
					fullPanel.revalidate();
					fullPanel.repaint();
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

	}// set view selector listener

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
	 * the case
	 *
	 * <p>
	 * Currently doesn't allow '-' character for negative
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
	}// number filter

	/**
	 * Action listener for page button, sets the current page +/- 1 depending on
	 * type used and also calls the populateTable... functions to retrieve next page
	 * from DB. Doesn't allow negative pages (stops at 0 first page)
	 *
	 * Future enh: calculate offset and allow negative, though probably not needed
	 * since user can sort by header to get the last/highest result that way anyway
	 *
	 * How to stop page number increasing past results set????
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

			Runnable task = ()->{
				if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("flights"))
				{
					if (searchActive)
					{
						table.populateTableWithFlightSearch(currentSortColumn, ascending);
					}
					else
					{
						table.populateTableWithAllFlights(currentSortColumn, ascending);

					}

				}
				else if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("line"))
				{
					table.populateTableWithAirline();
				}
				else if (viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains("basic"))
				{
					table.populateTableWithAirport(false);
				}
				else
				{
					table.populateTableWithAirport(true);
					// loadFlights();
				}

			};

			runWithLoadingLabel(task, null, "Loading page...");
			pageLabel.setText("" + currentPage); // hack, add emtpy string to get int as string?? shorter than Integer
			// parse int but probably not recc.
		}

	}//page button action listner


	/**
	 * Late attempt at getting the repeated loading screen work in background to be defined once,
	 *
	 *
	 * <p>Sets up a label and loading JDialog and uses swing worker to execute the task in
	 * background off the EDT.</p>
	 *
	 * , ones able to pass code to a function in java? fantastic. (Runnable class; might have to see the code for that when i know more adv. java.)
	 *
	 * @param task  the code to run in background
	 * @param event for the special case of being a ViewOptions select, expected null if not ComboBox item event
	 * @param baseInput label text
	 */
	public void runWithLoadingLabel(Runnable task, ItemEvent event, String baseInput)
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

				if(secondsElapsed[0]>5 && event != null)
				{	// update label on >5 for all other selections other than analaysis
					// for analysis selection use > 15 (expected to be > 5)

					if((!event.getItem().toString().toLowerCase().contains("analysis")) || (secondsElapsed[0]>15 && event.getItem().toString().toLowerCase().contains("analysis")))
					{
						loadingLabel.setText("<html>"+finalBase+"["+secondsElapsed[0]+"]"+"<br> Quite slow, check other processes for access to database. <br>"
								+ "e.g. Previous run / OneDrive </html>");

						loading.setSize(420,160);
						loading.revalidate();
						loading.repaint();

					}
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
				task.run();  		// Time-consuming operation

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
