package CovoitClasses;
import java.io.PrintWriter;
import java.util.*;

import CovoitClasses.AnnulatingCovoitAgent.update_current_price;
import CovoitClasses.CovoitAgent.PleaseDie;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * same as GroupStratCovoitAgent but with possible annulation
 * if agent receives a more interesting offer
 *
 */
public class AnnulationGroupCovoitAgent extends AnnulatingCovoitAgent {
	
	// ici toutes les methodes vont etre les memes (init, takedown...) a part les behaviors
	// et le setup dans lequel on ajoute juste la proba d'annuler
	//on redefinit juste la methode des behaviors et les autres seront celles de CovoitAgent
	
	protected void setup() {
		super.setup();
	}

	protected void behaviors() {
		current_price = price;
		//sera appel�e dans la m�thode init() de CovoitAgent
		
		//passenger agent behavior
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
				
				System.out.println(getAID().getName()+" : "+String.valueOf(passengers.size())+" passengers, "+String.valueOf(current_price)+" current price");
				
				/*if agent 
				 * - isn't driver of its own coalition
				 * - isn't already in the process of negotating a deal with another agent
				 * (NB: contrary to non-annulating agents, annulating agents will listen to CFPs
				 * even if they have already joined a coalition)
				 */
				if(passengers.size() == 0 &&!processing) {
					MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					ACLMessage msg = myAgent.receive(mt);
					
					if(msg != null && !msg.getSender().equals(current_recruiter)) {
						//if CFP is from a new driver
						
						processing = true; //negociation begins
						
						System.out.println(getAID().getName()+" received a proposed price : "+msg.getContent()+" from "+msg.getSender().getName()+ ". Current price : "+String.valueOf(current_price));
						System.out.println("received from : "+ msg.getSender().getName());
						
						ACLMessage proposal = msg.createReply();
						
						/* if proposed price is inferior to price that this agent would pay right now
						 * (either price of travel on its own if agent hasn't joined a coalition
						 * or if it has: price for the trip in current coalition)
						 */
						if(Double.parseDouble(msg.getContent()) <= current_price) {
							double r = Math.random();
							//if agent isn't in a coalition, or if it is but it's worth changing
							if(!recruited || recruited  && r < anulation_proba) {
								//agent accepts proposal
								proposal.setPerformative(ACLMessage.PROPOSE);
								proposal.setContent("ok");
								proposal.setConversationId("covoit_cfp");
							}
						}
						else {
							//agent refuses proposal
							proposal.setPerformative(ACLMessage.REFUSE);
							processing = false;
						}
						myAgent.send(proposal);
					}
					else {
						block();
					}
					
					MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
					ACLMessage msg2 = myAgent.receive(mt2);
					if(msg2 != null) {
						//aget receives confirmation of deal from driver
						if(!recruited) {
							recruited = true;
							current_recruiter = msg2.getSender();
							current_price = Double.parseDouble(msg2.getContent());
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
							current_price = Double.parseDouble(msg2.getContent());
						
						}
						
					}
					else {
						block();
					}
				}
			}
		} );
		
		//driver agent behavior
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected MessageTemplate mt;
			protected Boolean already_recruited;
			protected void onTick() {
				
				if(!recruited) {
					
					current_price = price/(1+passengers.size());
					
					/*
					 * makes template of passengers with similar departure and arrival cities
					 * (aka potential passengers)
					 */
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(but_agent.get_startingCity()+";"+but_agent.get_targetCity());
					template.addServices(sd);
					
					try {
						//make list of potential passengers in acquaintance
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						acquaintances = new ArrayList(result.length);
						for (int i = 0; i < result.length; ++i) {
							acquaintances.add(result[i].getName());
						}
						acquaintances.remove(getAID()); //remove this agent from the list (agent won't contact itself)
						
						for(AID a:passengers) {
							//remove agents that are already passengers
							acquaintances.remove(a);
						}
						
						for(AID a:refused) {
							//remove agents that have already refused
							acquaintances.remove(a);
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
					
					
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					//contact all potential passengers
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
							//if other agent accepts
							
							but_agent.set_nbPlaces(but_agent.get_nbPlaces() - 1); //one less seat in this agent's car
							
							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							confirm.addReceiver(reply.getSender());
							confirm.setContent(String.valueOf(price/(2+passengers.size())));
							current_price = Double.parseDouble(confirm.getContent());
							
							passengers.add(reply.getSender()); //other agent added to list of passengers
							
							confirm.setConversationId("covoit");
							myAgent.send(confirm);
							
							//give info to whoever is looking at console
							System.out.println(getAID().getName()+" accepted proposal from"+reply.getSender().getName());
							System.out.println("Number of passengers : "+String.valueOf(passengers.size()));
							System.out.println("Remaning seats : "+String.valueOf(but_agent.get_nbPlaces()));
							//end of console time
							
							System.out.println("Number of passengers : "+String.valueOf(passengers.size()));
							//updates the price of all the passengers, as it is divided between more agents
							
							ACLMessage update = new ACLMessage(ACLMessage.INFORM);
							for(AID a : passengers) {
								update.addReceiver(a);
							}
							
							//also sends it to itself so it can change its price
							update.setConversationId("new price");
							update.setContent(String.valueOf(current_price));
							myAgent.send(update);
							

							System.out.println("Remaining seats : "+String.valueOf(but_agent.get_nbPlaces()));
							if(but_agent.get_nbPlaces() == 0){
								//found_coalition_time = 
								coalition_times += String.valueOf(System.currentTimeMillis()-creation_time)+"\r\n";
								try (PrintWriter out = new PrintWriter("Coalition_times.txt")) {
								    out.println(coalition_times);
								}catch(Exception e){System.out.println(e);}   
								
								//kills the agents that formed a definitive coalition
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
							//if other agent refuses
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
		addBehaviour(new update_current_price());
	}
	

}
