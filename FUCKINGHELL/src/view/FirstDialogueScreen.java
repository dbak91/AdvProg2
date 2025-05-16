package view;

import javax.swing.JButton;
import javax.swing.JDialog;

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
public class FirstDialogueScreen extends JDialog
{
	private static final long serialVersionUID = 6653535384761080166L;

	/**
	 * Instantiates a new FirstDialogueScreen (does not set visible)
	 *
	 */
	public FirstDialogueScreen()
	{
		setTitle("Welcome");
		setSize(300, 120);
		setLocationRelativeTo(null);
		setModal(true); // Block input to other windows

		// Button to close pre-window and show main window
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

		JButton createDbBtn = new JButton("Create Db");
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
