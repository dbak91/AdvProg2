package controller;

import javax.swing.SwingUtilities;

import view.FirstDialogueScreen;
import view.MainDisplayFrame;
/**
 * Java entry point.
 *
 * <p>
 * Creates and sets visible a splash screen (FirstDialogScreen) and after close, does the same
 * for the main swing frame (TableViewFrame).
 * </p>
 *
 * @author 23751662
 */
public class FlightAnalyisApp
{
	public static void main(String[] args)
	{

		SwingUtilities.invokeLater(() ->
		{
			FirstDialogueScreen first = new FirstDialogueScreen();

			first.setVisible(true);

			MainDisplayFrame frame = new MainDisplayFrame();
			//open in center, must pack/load first
			frame.setLocationRelativeTo(null);

			frame.setVisible(true);
		});
	}
}
