package controller;

import javax.swing.SwingUtilities;

import view.WelcomeScreenJDialog;
import view.MainDisplayFrame;
/**
 * Java entry point.
 *
 * <p>
 * Creates and sets visible a splash screen (WelcomeScreenJDialog) and after close, does the same
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
			WelcomeScreenJDialog first = new WelcomeScreenJDialog();

			first.setVisible(true);

			MainDisplayFrame frame = new MainDisplayFrame();
			//open in center, must pack/load first
			frame.setLocationRelativeTo(null);

			frame.setVisible(true);
		});
	}
}
