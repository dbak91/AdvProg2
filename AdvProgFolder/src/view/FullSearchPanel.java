package view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;

/**
 * Panel for displaying the full search fields
 *
 * <p>search fields:</p>
 * <ul>
 *  <li>flight id</li>
 *  <li>date</li>
 *  <li>airline code</li>
 *  <li>flight number</li>
 *  <li>origin</li>
 *  <li>destination</li>
 *  <li>scheduled departure from and to</li>
 *  <li>actual departure from and to</li>
 *  <li>scheduled arrival from and to</li>
 *  <li>actual arrival from and to</li>
 *  <li>reason</li>
 *  <li>delay time from to</li>
 * </ul>
 */
public class FullSearchPanel extends JPanel
{

	private static final long	serialVersionUID	= 7767727814538820383L;
	private boolean						fieldChanged		= false;				// flag for changed text
	private List<JTextField>			fields				= new ArrayList<>();

	/**
	 * Constructor
	 * <ul>
	 * - set layout</li>
	 * <li>add fields
	 * <li>add action listener to set field chanmged flag</li>
	 * <li>set label</li>
	 * <li>add using gridbag layout (needed special case for from/to fields being on
	 * same line)</li></li>
	 * <li>add search button and listener
	 * <li>scan through changed fields and add to parent searchMap</li>
	 * <li>call parent populate table with flight search</li></li>
	 * </ul>
	 *
	 * Takes a TableViewFrame as parent to access populate method and search map
	 *
	 * @param parent
	 */
	/**
	 * @param parent
	 */
	public FullSearchPanel(MainDisplayFrame parent)
	{

		setLayout(new BorderLayout());

		JPanel form = new JPanel();
		form.setLayout(new GridBagLayout());

		GridBagConstraints grid = new GridBagConstraints();

		grid.insets = new Insets(4, 4, 4, 4); // padding
		grid.anchor = GridBagConstraints.WEST; // align labels to the left

		// do 12 times i.e. 12 rows to add
		for (int i = 0; i < 12; i++)
		{
			JLabel label = new JLabel("");
			JTextField field = new JTextField();
			field.addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					if (!field.getText().isEmpty())
					{
						fieldChanged = true;
					}
				}
			});

			switch (i)
			{
			case 0:
				label = new JLabel("Flight ID");
				break;
			case 1:
				label = new JLabel("Date");
				field.setText("YYYY-MM-DD");
				field.addFocusListener(new FocusAdapter()
				{
					@Override
					public void focusGained(FocusEvent e)
					{
						if (field.getText().equals("YYYY-MM-DD"))
						{
							field.setText("");
							// setForeground(Color.BLACK);
						}
					}

					@Override
					public void focusLost(FocusEvent e)
					{
						if (field.getText().isEmpty())
						{
							field.setText("YYYY-MM-DD");
							// setForeground(Color.GRAY);
						}
					}
				});
				break;
			case 2:
				label.setText("Airline (Code or Naame)");
				break;
			case 3:
				label.setText("Flight Number");
				break;
			case 4:
				label.setText("Origin (Code or Name)");
				break;
			case 5:
				label.setText("Destination (Code or Name)");
				break;
			case 6:
				label.setText("Scheduled Departure");
				break;
			case 7:
				label.setText("Actual Departure");
				break;
			case 8:
				label.setText("Scheduled Arrival");
				break;
			case 9:
				label.setText("Actual Arrival");
				break;
			case 10:
				label.setText("Delay Reason");
				break;
			case 11:
				label.setText("Delay Time(Total)");
				break;
			}

			grid.gridy = (i);

			// Label column
			grid.gridx = 0;
			grid.gridwidth = 1;
			grid.weightx = 0;
			grid.fill = GridBagConstraints.NONE;
			form.add(label, grid);

			if (i == 6 || i == 7 || i == 8 || i == 9 || i == 11)
			{
				grid.gridx = 1;
				grid.fill = GridBagConstraints.NONE;
				grid.weightx = 0;

				JLabel fromLabel = new JLabel("From:");
				JTextField fromField = new JTextField(10);

				// let delay time have - sign dont add any filter
				if (i != 11)
				{
					((AbstractDocument) fromField.getDocument()).setDocumentFilter(parent.new NumberOnlyFilter());
				}
				fromField.addFocusListener(new FocusAdapter()
				{

					@Override
					public void focusLost(FocusEvent e)
					{
						// JOptionPane.showMessageDialog(null, "Lost");
						if (!fromField.getText().isEmpty())
						{
							// JOptionPane.showMessageDialog(null, "Setting true");
							fieldChanged = true;
						}
					}

				});
				fields.add(fromField);
				JLabel toLabel = new JLabel("To:");
				JTextField toField = new JTextField(10);

				// let delay time have - sign dont add any filter
				if (i != 11)
				{
					((AbstractDocument) toField.getDocument()).setDocumentFilter(parent.new NumberOnlyFilter());
				}
				toField.addFocusListener(new FocusAdapter()
				{
					@Override
					public void focusLost(FocusEvent e)
					{
						if (!toField.getText().isEmpty())
						{
							fieldChanged = true;
						}
					}
				});
				fields.add(toField);

				// start after initial label
				grid.gridx = 1;
				form.add(fromLabel, grid);
				grid.gridx = 2;
				form.add(fromField, grid);
				grid.gridx = 3;
				form.add(toLabel, grid);
				grid.gridx = 4;
				form.add(toField, grid);

			}
			else
			{
				// start after initial label
				grid.gridx = 1;
				grid.gridwidth = 8; // all taking 2 but we have added 4 more for the from and to fields on other
				// rows, so width =4x2
				// span
				grid.fill = GridBagConstraints.HORIZONTAL;
				// single weight of width 8
				grid.weightx = 1;
				form.add(field, grid);
				fields.add(field);
			}

			//
		} // switch on number of fields

		JButton searchBtn = new JButton("Search");

		searchBtn.addActionListener(new ActionListener()
		{
			/**
			 * click event
			 *
			 * @param e auto supplied
			 */
			@Override
			public void actionPerformed(ActionEvent e)
			{
				searchBtn.requestFocusInWindow(); // force fields to lose focus to trigger their listeners

				for (JTextField field : fields)
				{
					if (!field.getText().isEmpty() && !field.getText().contains("YYYY"))
					{
						System.out.println("setting true");
						fieldChanged = true;
						break;
					}

				}
				if (fieldChanged)
				{
					parent.searchMap.clear();

					// build search term and populate table
					for (int i = 0; i < 17; i++)
					{

						if (fields.get(i) != null && !fields.get(i).toString().isEmpty())
						{
							String fieldText = fields.get(i).getText();
							String key = "";
							switch (i)
							{
							case 0:
								key = "flight_id";
								break;
							case 1:
								key = "date";
								break;
							case 2:
								key = "airline code";
								break;
							case 3:
								key = "flight number";
								break;
							case 4:
								key = "origin";
								break;
							case 5:
								key = "destination";
								break;
							case 6:
								key = "scheduled departure from";
								break;
							case 7:
								key = "scheduled departure to";
								break;
							case 8:
								key = "departure from";
								break;
							case 9:
								key = "departure to";
								break;
							case 10:
								key = "scheduled arrival from";
								break;
							case 11:
								key = "scheduled arrival to";
								break;
							case 12:
								key = "arrival from";
								break;
							case 13:
								key = "arrival to";
								break;
							case 14:
								key = "reason";
								break;
							case 15:
								key = "delay from";
								break;
							case 16:
								key = "delay to";
								break;
							default:
								System.err.println(
										"Error, no case for this switch (FullSearchPanel->searchBtn->addAction...");
								break;
							}
							if (!key.isEmpty())
							{
								// cater for date placeholder
								if (!fieldText.equals("YYYY-MM-DD") && !fieldText.isEmpty())
								{
									/*
									 * add to search map, big comment cause hard to find on quick scan
									 */
									parent.searchMap.put(key, fieldText);
								}
							}

						} // field not empty
					} // for all fields
					parent.searchActive = true;
					Runnable task = () -> {
						parent.populateTableWithFlightSearch("date", true);
					};

					parent.runWithLoadingLabel(task, null, "Full search...May be slow if too unique");
				}
				else
				{
					JOptionPane.showMessageDialog(null, "No change");
				}

				parent.cardLayout.show(parent.mainPanel, "TABLE V");
			}
		});
		// second column, give it some left space
		grid.gridx = 2;
		grid.gridy = grid.gridy + 1; // next row down

		form.add(searchBtn, grid);

		// addining without a NORTH BorderLayout to gridbad will stretch/fill
		// so add to north wrapper for non stretched fields and northwest start
		JPanel northWrapper = new JPanel(new BorderLayout());
		northWrapper.add(form, BorderLayout.NORTH);
		// add(wrapper, BorderLayout.WEST);
		add(northWrapper, BorderLayout.WEST);
	}
}
