package CovoitClasses;
import java.util.*;

import CovoitClasses.CovoitAgent.PleaseDie;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public class AnnulatingCovoitAgent extends CovoitAgent {
	
	// ici toutes les methodes vont etre les memes (init, takedown...) a part les behaviors
	// et le setup dans lequel on ajoute juste la proba d'annuler
	//on redefinit juste la methode des behaviors et les autres seront celles de CovoitAgent
	
	protected double anulation_proba;
	protected AID current_recruiter;
	protected int current_price;
	
	protected void setup() {
		super.setup();
		anulation_proba = 1.0;
		
		//System.out.println("current_price setup : "+String.valueOf(current_price));

	}
	
	protected void behaviors() {
		current_price = price;
		//sera appel�e dans la m�thode init() de CovoitAgent
		//passenger agent behavior
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
				System.out.println(getAID().getName()+" : "+String.valueOf(passengers.size()));
				if(passengers.size() == 0) {
					MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					ACLMessage msg = myAgent.receive(mt);
					System.out.println(getAID().getName()+" passengers empty!! ");
					if(msg != null && !msg.getSender().equals(current_recruiter)) {
						System.out.println(getAID().getName()+" : proposed price : "+msg.getContent()+ " current price : "+String.valueOf(current_price));
						System.out.println("received from : "+ msg.getSender().getName());
						ACLMessage proposal = msg.createReply();
						// if the proposed price is inferior than the the price of the agent's travel on its own
						if(Integer.parseInt(msg.getContent()) <= current_price) {
							double r = Math.random();
							// on envoie une proposition que si le recruter n'est pas notre driver actuel
							if(!recruited || recruited  && r < anulation_proba) {
								proposal.setPerformative(ACLMessage.PROPOSE);
								proposal.addReceiver(msg.getSender());
								proposal.setContent("ok");
								proposal.setConversationId("covoit_cfp");
							}
						}
						else {
							proposal.setPerformative(ACLMessage.REFUSE);
						}
						myAgent.send(proposal);
					}
					else {
						block();
					}
					MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
					ACLMessage msg2 = myAgent.receive(mt2);
					if(msg2 != null) {
						if(!recruited) {
							recruited = true;
							current_recruiter = msg2.getSender();
							current_price = Integer.parseInt(msg2.getContent());
						}
						else {
							//on est un rascal et on decide de lacher notre premier driver
							//on l'en informe avec un air contrit (ou pas)
							//la classe qui ecoute un CANCEL est dans la classe mere
							//(meme un agent qui lui n'annulerait pas un trajet doit pouvoir gerer
							//les annulations des autres)
							ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
							cancel.addReceiver(current_recruiter);
							cancel.setContent("I'm betraying you");//not useful I know
							cancel.setConversationId("cancel");
							myAgent.send(cancel);							
							//mise a jour de son driver actuel
							current_recruiter = msg2.getSender();
							current_price = Integer.parseInt(msg2.getContent());
						
						}
						
					}
					else {
						block();
					}
				}
				else {
					current_price = price/(2+passengers.size());
				}
			}
		} );
		
		//driver agent behavior
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected MessageTemplate mt;
			protected Boolean already_recruited;
			protected void onTick() {
				current_price = price/(2+passengers.size());
				if(!recruited) {
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(but_agent.get_startingCity()+";"+but_agent.get_targetCity());
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						acquaintances = new ArrayList(result.length);
						for (int i = 0; i < result.length; ++i) {
							acquaintances.add(result[i].getName());
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
					
					
					//envoi aux  passagers potentiels
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					
					for (int i = 0; i < acquaintances.size(); ++i) {
						if(!passengers.contains(acquaintances.get(i))) {
							cfp.addReceiver(acquaintances.get(i));
						}
					} 
					cfp.setContent(String.valueOf(price/(2+passengers.size())));
					cfp.setConversationId("covoit_cfp");
					cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
					myAgent.send(cfp);
					//Prepare the template to get proposals
					mt = MessageTemplate.MatchConversationId("covoit_cfp");
					ACLMessage reply = myAgent.receive(mt);
					if(reply != null) {
						
						
						if(reply.getPerformative()== ACLMessage.PROPOSE && !passengers.contains(reply.getSender())) {
							
							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							confirm.addReceiver(reply.getSender());
							confirm.setContent(String.valueOf(price/(2+passengers.size())));
							confirm.setConversationId("covoit");
							myAgent.send(confirm);
							System.out.println(getAID().getName()+" recruited "+reply.getSender().getName());
							System.out.println("Agreed price : "+confirm.getContent());
							passengers.add(reply.getSender());
							but_agent.set_nbPlaces(but_agent.get_nbPlaces() - 1);
							System.out.println("Number of passengers : "+String.valueOf(passengers.size()));
							System.out.println("Remaning seats : "+String.valueOf(but_agent.get_nbPlaces()));
							if(but_agent.get_nbPlaces() == 0){
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
							System.out.println(getAID().getName()+" refused proposal from "+ reply.getSender().getName());
						}
					}
					else {
						block();
					}
				}
			}
		} );
		addBehaviour(new PleaseDie());
	}

}
