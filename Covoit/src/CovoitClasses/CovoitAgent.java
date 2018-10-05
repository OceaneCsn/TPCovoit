package CovoitClasses;
import java.util.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class CovoitAgent extends Agent {
	
	//private CovoitGui mygui;
	
	private String startingCity;
	private String targetCity;
	private int leavingTime;
	private int carScore;
	private int nbPlaces;
	private ArrayList<AID> passengers;
	private ArrayList<AID> refused;
	private int price;
	private ArrayList<AID> acquaintances;
	
	
	protected void setup( ) {
		
		startingCity = "Montpellier";
		targetCity = "Lyon";
		leavingTime = 15;
		carScore = 4;
		nbPlaces = 3;
		price = 4; 
		passengers = new ArrayList<AID>();
		refused = new ArrayList<AID>();
		
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(startingCity+";"+targetCity);
		sd.setName("JADE-covoit");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new NegociationDriver());
		addBehaviour(new NegociationPassenger());
	}
	
	private class NegociationDriver extends CyclicBehaviour {
		
		private MessageTemplate mt;
		
		public void action(){
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(startingCity+";"+targetCity);
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template); 
				//System.out.println("Found the following agents:");
				acquaintances = new ArrayList(result.length);
				for (int i = 0; i < result.length; ++i) {
					acquaintances.add(result[i].getName());
					//System.out.println(result[i].getName());
				}
				acquaintances.remove(getAID());
				for(AID a:passengers) {
					acquaintances.remove(a);
				}
				for(AID a:refused) {
					acquaintances.remove(a);
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			
			
			//envoi aux premier passager potentiel
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			
			for (int i = 0; i < acquaintances.size(); ++i) {
				cfp.addReceiver(acquaintances.get(i));
			} 
			cfp.setContent(String.valueOf(price));
			cfp.setConversationId("covoit");
			cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
			myAgent.send(cfp);
			
			
			// Prepare the template to get proposals
			mt = MessageTemplate.MatchConversationId("cfp_answer");
			//System.out.println("template matched");
			ACLMessage reply = myAgent.receive(mt);
			//System.out.println("message received");
			if(reply != null) {
				System.out.println("message received");
				if(reply.getPerformative()== ACLMessage.PROPOSE) {
					System.out.println("cfp proposed");
					
					passengers.add(reply.getSender());
					nbPlaces --;
					if(nbPlaces == 0){
						for(AID a : passengers) {
							ACLMessage die = new ACLMessage(ACLMessage.REQUEST);
							die.addReceiver(a);
							die.setConversationId("apoptosis");
							myAgent.send(die);
						}
						doDelete();
						
					}
					
					
					ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					confirm.addReceiver(reply.getSender());
					confirm.setContent(String.valueOf(price/(1+acquaintances.size())));
					confirm.setConversationId("covoit");
					myAgent.send(confirm);
					System.out.println(getAID().getName()+" accepted proposal from"+reply.getSender());
					System.out.println(String.valueOf(passengers.size()));
					System.out.println(String.valueOf(nbPlaces));
				}
				
				if(reply.getPerformative()== ACLMessage.REFUSE){
					System.out.println(getAID().getName()+" refused proposal");
					
				}
			}
			
		}
	}
	
	private class NegociationPassenger extends CyclicBehaviour {
		public void action(){
			if(passengers.size() == 0) {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null) {
					// if the proposed price is inferior than the the price of the agent's travel on its own
					if(Integer.parseInt(msg.getContent()) <= price) {
						//System.out.println("received price interesting");
						ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);
						proposal.addReceiver(msg.getSender());
						proposal.setContent("ok");
						proposal.setConversationId("cfp_answer");
						myAgent.send(proposal);
						//System.out.println(proposal.getSender()+" sent a proposal");
					}
					else {
						ACLMessage refuse = new ACLMessage(ACLMessage.REFUSE);
						myAgent.send(refuse);
					}
				}
			}			
		}
	}
	
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
		//myGui.dispose();
		// Printout a dismissal message
		System.out.println("Agent "+getAID().getName()+" terminating.");
	}
	
	protected class pleaseDie extends CyclicBehaviour{
		public void action(){
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if(msg.getConversationId().equals("apoptosis")) {
				System.out.println("Agent "+getAID().getName()+" terminating.");
				doDelete();
			}
			
		}
	}
	
	

}
