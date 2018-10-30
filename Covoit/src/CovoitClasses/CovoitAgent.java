package CovoitClasses;
import java.util.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.AgentContainer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.wrapper.AgentController;

public abstract class CovoitAgent extends Agent {
	
	static int counterAgents = 0; //counts number of existing agents
	protected CovoitAgentGui myGui;
	protected But but_agent;
	protected ArrayList<AID> passengers;
	protected ArrayList<AID> refused;
	protected double price;
	protected ArrayList<AID> acquaintances;
	protected Boolean recruited;
	//private 
	
	protected void setup() {
		myGui = new CovoitAgentGui(this);
		myGui.showGui();
		
		
	}
	
	protected void init() {		
		/*startingCity = "Montpellier";
		targetCity = "Lyon";
		leavingTime = 15;
		carScore = 4;
		nbPlaces = 3;
		price = 4; */
		passengers = new ArrayList<AID>();
		refused = new ArrayList<AID>();
		recruited = false;
		
		counterAgents++;
		
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(but_agent.get_startingCity()+";"+but_agent.get_targetCity());
		sd.setName("JADE-covoit");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new PleaseDie());
		addBehaviour(new cancelPassenger());
		this.behaviors();
	}
	
	// initialize key variables
	protected void updateTravel(String sCity, String tCity, Integer lTime, Integer cScore, Integer nbP, Integer pr) {
		but_agent = new But(sCity,tCity,lTime,cScore,nbP);
		price = pr;
		System.out.println("Agent "+getAID().getName()+" going from "+this.but_agent.get_startingCity()+" to "+this.but_agent.get_targetCity()+ " at price "+String.valueOf(price));
		this.init();
	}
	
	protected abstract void behaviors();
	
	
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("Agent "+getAID().getName()+" terminating.");
	}
	

	
	protected class PleaseDie extends CyclicBehaviour{
		public void action(){
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				if(msg.getConversationId().equals("apoptosis")) {
					System.out.println("Agent "+getAID().getName()+" was aked to terminate. I did.");
					doDelete();
				}
			}
		}
	}
	
	protected class cancelPassenger extends CyclicBehaviour{
		public void action(){
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				System.out.println("canel received!");
				if(msg.getConversationId().equals("cancel")) {
					System.out.println("Agent "+getAID().getName()+" deletes "+msg.getSender().getName()+" of its passengers");
					passengers.remove(msg.getSender());

					//sets the new price of all the other passengers that will have to pay more
					double new_price = price /(1+passengers.size());
					ACLMessage update = new ACLMessage(ACLMessage.INFORM);
					for(AID a : passengers) {
						update.addReceiver(a);
					}
					//also sends it to itself
					update.addReceiver(getAID());
					update.setConversationId("new price");
					update.setContent(String.valueOf(new_price));
					myAgent.send(update);
					but_agent.set_nbPlaces(but_agent.get_nbPlaces() +1);
				}
			}
		}
	}
}


