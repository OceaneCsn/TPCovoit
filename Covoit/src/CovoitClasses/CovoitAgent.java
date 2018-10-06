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
	
	private CovoitAgentGui myGui;
	
	private String startingCity;
	private String targetCity;
	private int leavingTime;
	private int carScore;
	private int nbPlaces;
	private ArrayList<AID> passengers;
	private ArrayList<AID> refused;
	private int price;
	private ArrayList<AID> acquaintances;
	private Boolean recruited;
	
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
		addBehaviour(new NegociationPassenger());
		addBehaviour(new NegociationDriver());
		
		addBehaviour(new PleaseDie());
	}
	
	private class NegociationDriver extends CyclicBehaviour {
		
		private MessageTemplate mt;
		private Boolean already_recruited;
		public void action(){
			
			if(recruited) {
				//System.out.println(getAID().getName()+" recruited at the beginning of the loop");
			}
			
			if(!recruited) {
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
						//System.out.println(a+" removed");
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
					//System.out.println(getAID().getName()+"sending cfp to "+acquaintances.get(i).getName());
				} 
				cfp.setContent(String.valueOf(price));
				cfp.setConversationId("covoit_cfp");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				//System.out.println(cfp.getReplyWith());
				/*mt = MessageTemplate.and(MessageTemplate.MatchConversationId("covoit_cfp"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));*/
				//Prepare the template to get proposals
				mt = MessageTemplate.MatchConversationId("covoit_cfp");
				//mt = MessageTemplate.MatchInReplyTo(cfp.getReplyWith());
				ACLMessage reply = myAgent.receive(mt);
				if(reply != null) {
					//System.out.println("cfp proposal received, not null");
					already_recruited = false;
					for(AID a:passengers) {
						if(a.equals(reply.getSender())) {
							already_recruited=true;
						}
					}
					//System.out.println("already_recruited : "+already_recruited+ reply.getPerformative());
					if(reply.getPerformative()== ACLMessage.PROPOSE && !already_recruited) {
						System.out.println("cfp proposal");
						passengers.add(reply.getSender());
						nbPlaces --;
						ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						confirm.addReceiver(reply.getSender());
						confirm.setContent(String.valueOf(price/(1+acquaintances.size())));
						confirm.setConversationId("covoit");
						myAgent.send(confirm);
						System.out.println(getAID().getName()+" accepted proposal from"+reply.getSender().getName());
						System.out.println(String.valueOf(passengers.size()));
						System.out.println(String.valueOf(nbPlaces));
						
						if(nbPlaces == 0){
							for(AID a : passengers) {
								ACLMessage die = new ACLMessage(ACLMessage.REQUEST);
								die.addReceiver(a);
								die.setConversationId("apoptosis");
								myAgent.send(die);
							}
							doDelete();
						}
					}
					
					if(reply.getPerformative()== ACLMessage.REFUSE){
						System.out.println(getAID().getName()+" refused proposal");
					}
				}
				else {
					block();
				}
				
			}
			/*try{
				Thread.sleep(1000);
				//System.out.println("pause");
			}
			catch(InterruptedException e){}*/
		}
	}
	
	private class NegociationPassenger extends CyclicBehaviour {
		public void action(){
			if(passengers.size() == 0 && !recruited) {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
				ACLMessage msg = myAgent.receive(mt);
				//System.out.println(getAID().getName()+" before received cfp "+msg.getPerformative());

				if(msg != null) {
					//System.out.println(getAID().getName()+" received cfp"+msg.getPerformative());

					ACLMessage proposal = msg.createReply();
					// if the proposed price is inferior than the the price of the agent's travel on its own
					if(Integer.parseInt(msg.getContent()) <= price) {
						//System.out.println("received price interesting");
						proposal.setPerformative(ACLMessage.PROPOSE);
						proposal.addReceiver(msg.getSender());
						proposal.setContent("ok");
						proposal.setConversationId("covoit_cfp");
						System.out.println(getAID().getName()+" sent a proposal");
					}
					else {
						
						proposal.setPerformative(ACLMessage.REFUSE);
					}
					System.out.println(proposal.getInReplyTo());
					myAgent.send(proposal);
				}
				else {
					block();
				}
				MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
				ACLMessage msg2 = myAgent.receive(mt2);
				if(msg2 != null) {
					System.out.println(getAID().getName()+" recruited");
					recruited = true;
				}
				else {
					block();
				}
			}
			/*try{
				//Thread.sleep(2000);
				//System.out.println("pause");
			}
			catch(InterruptedException e){}*/
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
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("Agent "+getAID().getName()+" terminating.");
	}
	
	protected void updateTravel(String sCity, String tCity, Integer lTime, Integer cScore, Integer nbP, Integer pr) {
		startingCity = sCity;
		targetCity = tCity;
		leavingTime = lTime;
		carScore = cScore;
		nbPlaces = nbP;
		price = pr;
		System.out.println("Agent "+getAID().getName()+" going from "+startingCity+" to "+targetCity);
		this.init();
	}
	
	protected class PleaseDie extends CyclicBehaviour{
		public void action(){
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				if(msg.getConversationId().equals("apoptosis")) {
					System.out.println("Agent "+getAID().getName()+" terminating.");
					doDelete();
				}
			}
		}
	}
	
	

}
