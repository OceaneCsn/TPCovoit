package CovoitClasses;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


public class CovoitAgentGui extends JFrame{
	
	private CovoitAgent myAgent;
	private JTextField leavingTimeField, priceField, placesField;
	private JComboBox<String> startingCityField, targetCityField, carScoreField;
	
	private Hashtable<String, Integer> state_to_num;

	CovoitAgentGui(CovoitAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(7, 2));
		
		state_to_num = new Hashtable();
		state_to_num.put("Awesome", 1);
		state_to_num.put("Nice", 2);
		state_to_num.put("Correct", 3);
		state_to_num.put("Bad", 4);
		
		p.add(new JLabel("Carpooling agent settings"));
		p.add(new JLabel(""));

		
		p.add(new JLabel("Going from :"));
		String[] startingCities = {"Lyon","Montpellier","Paris"};
		startingCityField = new JComboBox<String>(startingCities);
		p.add(startingCityField);
		
		p.add(new JLabel("To :"));
		String[] targetCities = {"Montpellier","Lyon","Paris"};
		targetCityField = new JComboBox<String>(targetCities);
		p.add(targetCityField);
		
		p.add(new JLabel("Leaving at (in hours, from 0 to 24): "));
		leavingTimeField = new JTextField(15);
		leavingTimeField.setText("15");
		p.add(leavingTimeField);
		
		p.add(new JLabel("Number of available seats :"));
		placesField = new JTextField(15);
		placesField.setText("4");
		p.add(placesField);
		
		p.add(new JLabel("Car score :"));
		String[] scores = {"Awesome","Nice","Correct", "Bad"};
		//String[] scores = {"1","2","3", "4"};
		carScoreField = new JComboBox<String>(scores);
		p.add(carScoreField);
				
		p.add(new JLabel("Price :"));
		priceField = new JTextField(15);
		priceField.setText("40");
		p.add(priceField);
		
		
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Ok");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String startingCity = (String)startingCityField.getSelectedItem();
					String targetCity = (String)targetCityField.getSelectedItem();
					String price = priceField.getText().trim();
					String places = placesField.getText().trim();
					String leavingTime = leavingTimeField.getText().trim();
					String carScore = (String)carScoreField.getSelectedItem();
					myAgent.updateTravel(startingCity, targetCity, Integer.parseInt(leavingTime), state_to_num.get(carScore),Integer.parseInt(places), Integer.parseInt(price));
					addButton.setEnabled(false);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(CovoitAgentGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		// Make the agent terminate when the user closes 
		// the GUI using the button on the upper right corner	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}	

}
