package CovoitClasses;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
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


/**
 * Abstract class that defines an agent ready for covoiturage
 * Will be used by: GroupStratCovoitAgent,OneByOneStratCovoitAgent, PatientCovoitAgent,
 * AnnulationGroupCovoitAgent, AnnulationOneByOneCovoitAgent
 * 
 * @author oceane_louise
 *
 */
public abstract class CovoitAgent extends Agent {
	
	
	static int counterAgents = 0; //counts number of existing agents
	static String coalition_times = "";
	//static BufferedWriter output = new BufferedWriter(new FileWriter("times.txt", true));
	//catch{Error r
	
	
	protected CovoitAgentGui myGui;
	protected But but_agent;
	protected double price;
	
	protected ArrayList<AID> passengers; //if agent becomes driver, list of passengers
	protected ArrayList<AID> refused; //agents that refused this agent's offer
	protected ArrayList<AID> acquaintances; //potential passengers to contact
	
	protected Boolean recruited; //TRUE if this agent has become passenger of another driver; else FALSE
	
	protected Boolean processing; //if agent has been contacted by another driver and hasn't answered yet
	
	protected long creation_time;
	protected long found_coalition_time;
	
	protected void setup() {
		passengers  = new ArrayList<AID>();
		recruited = false;
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
		
		counterAgents++;
		refused = new ArrayList<AID>();
		recruited = false;
		processing = false;

		creation_time = System.currentTimeMillis();
		
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
	
	protected long get_creation_time() {
		return creation_time;
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
	

	/**
	 * if this agent has joined another coalition and the coalition is complete:
	 * coalition driver will ask agent to kill itself by sending REQUEST message
	 *
	 */
	protected class PleaseDie extends CyclicBehaviour{
		public void action(){
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				if(msg.getConversationId().equals("apoptosis")) {
					coalition_times += String.valueOf(System.currentTimeMillis()-creation_time)+"\r\n";
					try (PrintWriter out = new PrintWriter("Coalition_times.txt")) {
					    out.println(coalition_times);
					}catch(Exception e){System.out.println(e);}  
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
				System.out.println("cancel received!");
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


