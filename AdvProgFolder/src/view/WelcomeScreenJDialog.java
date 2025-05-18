package view;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import model.DatabaseManager;

/**
 * Intended to be the first splash screen. Extends JDlialog.
 *
 * <p>Contains 3 buttons:</p>
 * <ul>
 * <li>Create DB Button     - Deletes and creates a fresh database (must not be
 * locked)</li>
 * <li>Import Csv Button    - Inserts data from hard coded CSV file into
 * tables</li>
 * <li>Continue/view button - Closes(dispose) this splash and gives control back
 * to .setVisisble() caller</li>
 * </ul>
 *
 *
 * @author 23751662
 */
public class WelcomeScreenJDialog extends JDialog
{
	private static final long serialVersionUID = 6653535384761080166L;

	// wanted generated build-time but need maven external to generate system property.
	// hard-coding for now.
	private static String BUILD_ID="v1.0-alpha";
	/**
	 * Instantiates a new WelcomeScreenJDialog (does not set visible)
	 *
	 */
	public WelcomeScreenJDialog()
	{
		setTitle("Welcome " + BUILD_ID);
		setSize(300, 120);
		setLocationRelativeTo(null);


		// DO NOT GO TO MAIN FRAME ON CLOSE
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{

			@Override
			public void windowClosing(WindowEvent e) {
				// Exit the entire application
				System.exit(0);
			}
		});

		setModal(true); // Block input to other windows

		// Button to close this pre-window dialog and show main window frame
		JButton continueButton = new JButton("Continue (View Data)");
		continueButton.addActionListener(e -> dispose());
		if (DatabaseManager.existsAndPopulated())
		{
			continueButton.setEnabled(true); // not sure well ever get here
		}
		else
		{
			continueButton.setEnabled(false);
		}

		JButton importCsvBtn = new JButton("Import/Update from CSV");

		importCsvBtn.addActionListener(new CSVActionListener(continueButton));

		if (DatabaseManager.exists())
		{
			importCsvBtn.setEnabled(true);
		}
		else
		{
			importCsvBtn.setEnabled(false);
		}

		JButton createDbBtn = new JButton("Create/Reset Database");
		createDbBtn.addActionListener(e ->
		{
			DatabaseManager.createNewDataBase(true);
			importCsvBtn.setEnabled(true);
			continueButton.setEnabled(false);
		});
		add(createDbBtn, "North");
		add(importCsvBtn, "Center");
		add(continueButton, "South");

	}
}
