package view;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import model.util.CSVImporter;

/**
 * Extends Action listener solely for a 'import csv' Button
 * <br>
 * Calls CSVImporter.importcsv on button pressed,
 * <ul>
 * <li>if continue button and successful sets to enabled</li>
 * <li>if frame supplied calls the populate table methods for selected view
 * options</li>
 * </ul>
 * 
 * <p>
 * Handles both cases where caller is either splash screen (with continue btn)
 * or main Frame (a TableViewFrame)
 * </p>
 *
 * @author 23751662
 */
public class CSVActionListener implements ActionListener
{

	private JButton			continueBtn	= null;
	private MainTableFrame	parent;

	/**
	 * Constructor (with continue button)<br>
	 * Table frame will be null
	 * @param continueBtn the button to be set enabled if import successful
	 * 
	 */
	public CSVActionListener(JButton continueBtn)
	{
		this.continueBtn = continueBtn;
		this.parent = null;
	}

	/**
	 * Constructor (with parent MainTableFrame)<br>
	 * continue button will be null
	 * 
	 * @param parent frame that is the parent. must have used methods
	 */
	public CSVActionListener(MainTableFrame parent)
	{
		this.parent = parent;
		continueBtn = null;
	}

	boolean importSuccessful = false;

	/**
	 * Click event
	 * Call import csv and set button/table
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		// TODO Auto-generated method stub
		try
		{
			System.err.println("import pressed");
			JDialog loading = new JDialog(null, "Please wait...", Dialog.ModalityType.APPLICATION_MODAL);
			loading.setSize(420, 100);
			loading.setLocationRelativeTo(null);

			JLabel loadingLabel = new JLabel("Loading...CSV Import ");
			loading.add(loadingLabel);
			// loading.setVisible(true);
			
			// do in background so laoding can be displayed and updated
			SwingWorker<Void, Void> csvWorker = new SwingWorker<>()
			{
				@Override
				protected Void doInBackground() throws Exception
				{
					try
					{
						CSVImporter.importCSV(loadingLabel); // Time-consuming operation

						if (parent != null)
						{
							if (parent.viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains(
									"airline"))
							{

								loadingLabel.setText("Loading...Populating Airline Table");
								parent.populateTableWithAirline();

							}
							else if (parent.viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains(
									"flights"))
							{
								loadingLabel.setText("Loading...Populating Flights Table");
								parent.populateTableWithAllFlights(parent.currentSortColumn, true);
							}
							else if (parent.viewSelectorComboBox.getSelectedItem().toString().toLowerCase().contains(
									"analysis"))
							{
								loadingLabel.setText("Loading...Populating Airport Analysis Table");
								parent.populateTableWithAirport(true);
							}
							else
							{
								loadingLabel.setText("Loading...Populating Airport Basic Table");
								parent.populateTableWithAirport(false);

							}
						}

						importSuccessful = true;

					} catch (Exception e)
					{
						JOptionPane.showMessageDialog(null,
								"Import problem:\n" + "Type:" + e.getClass().getName() + "\n" + e.getMessage());
						e.printStackTrace();
					}
					return null;
				}

				@Override
				protected void done()
				{
					loading.dispose();
					if (continueBtn != null)
					{
						continueBtn.setEnabled(true);
					}
				}
			};
			csvWorker.execute(); // worker.execute();
			loading.setVisible(true);

			if (continueBtn != null && importSuccessful)
			{
				continueBtn.setEnabled(true);
			}
		} catch (Exception e1) // csv exception caught inside swing worker and never thrown, not sure this path
								// is possible, leaving for debug
		{
			JOptionPane.showMessageDialog(null,
					"Something went wrong on import:\n" + "Type:" + e.getClass().getName() + "\n"
							+ e1.getLocalizedMessage());

			e1.printStackTrace();

			throw e1;
		}

	}

}
