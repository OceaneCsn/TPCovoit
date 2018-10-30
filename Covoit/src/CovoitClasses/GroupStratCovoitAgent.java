package CovoitClasses;
import java.io.PrintWriter;
import java.util.*;


import CovoitClasses.CovoitAgent.PleaseDie;
import jade.core.AID;
import jade.core.Agent;
import jade.core.AID;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;



/**
 * When this agent is driver: will send its proposal to ALL potential passengers
 * in its list, then treat answers as they arrive
 *
 */
public class GroupStratCovoitAgent extends CovoitAgent {

	
	protected void setup() {
		super.setup();
	}

	
	protected void behaviors() {

		//passenger agent behavior, same as 1by1 Strategy

		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
				/*if agent 
				 * - isn't driver of its own coalition
				 * - doesn't belong to another coalition
				 * - isn't already in the process of negotating a deal with another agent
				 */
				if(passengers.size() == 0 && !recruited &&!processing) {
					System.out.println("Entered passenger behaviour for agent "+getAID().getName());
					
					//prepares template to receive Call For Proposals from another agent
					MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					ACLMessage msg = myAgent.receive(mt);

					if(msg != null) {
						
						processing = true; //negociation begins
						
						ACLMessage proposal = msg.createReply();
						// if the proposed price is inferior than the the price of the agent's travel on its own: accept
						if(Float.valueOf(msg.getContent()) <= (float) price) {
							//System.out.println("received price interesting");
							proposal.setPerformative(ACLMessage.PROPOSE);
							proposal.setContent("ok");
							proposal.setConversationId("covoit_cfp");
						}
						else {
							
							proposal.setPerformative(ACLMessage.REFUSE);
							processing = false; //negociation ends with refusal
						}
						myAgent.send(proposal);
					}
					else {
						block();
					}
					MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
					ACLMessage msg2 = myAgent.receive(mt2);
					if(msg2 != null) {
						System.out.println(getAID().getName()+" recruited");
						recruited = true; //negociation ends with acceptance
						processing = false; //technically useless here since agent has joined a coalition and cannot renege on deal
						//will be useful for Annulating agents that can renege though
					}
					else {
						block();
					}
				}
			}
		} );
		

		//driver agent behavior, DIFFERENT FROM 1BY1 STRATEGY

		addBehaviour(new TickerBehaviour(this, 10000) {
			
			protected MessageTemplate mt;
			
			protected void onTick() {
				if(recruited) {
					System.out.println(getAID().getName()+" recruited at the beginning of the loop");
				}
				
				if(!recruited) {
					/*
					 * makes template of passengers with similar departure and arrival cities
					 * (aka potential passengers)
					 */
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(but_agent.get_startingCity()+";"+but_agent.get_targetCity());
					template.addServices(sd);
					
					try {
						//make list of potential passengers in acquaintances
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						acquaintances = new ArrayList(result.length);
						for (int i = 0; i < result.length; ++i) {
							acquaintances.add(result[i].getName());
							//System.out.println(result[i].getName());
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
					
					
					//contact all potential passengers
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
						
						if(reply.getPerformative()== ACLMessage.PROPOSE) {
							//if other agent accepts
							
							but_agent.set_nbPlaces(but_agent.get_nbPlaces() - 1); //one less seat in this agent's car
							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							confirm.addReceiver(reply.getSender());
							confirm.setContent(String.valueOf(price/(2+passengers.size())));
							
							passengers.add(reply.getSender()); //other agent added to list of passengers
							
							confirm.setConversationId("covoit");
							myAgent.send(confirm);
							
							//give info to whoever is looking at console
							System.out.println(getAID().getName()+" accepted proposal from"+reply.getSender().getName());
							System.out.println("Number of passengers : "+String.valueOf(passengers.size()));
							System.out.println("Remaning seats : "+String.valueOf(but_agent.get_nbPlaces()));
							//end of console time
							
							if(but_agent.get_nbPlaces() == 0){
								//fills the register of the time to form the coalition
								coalition_times += String.valueOf(System.currentTimeMillis()-creation_time)+"\r\n";
								try (PrintWriter out = new PrintWriter("Coalition_times.txt")) {
								    out.println(coalition_times);
								}catch(Exception e){System.out.println(e);} 
								
								//kills all the agents as they all formed their definitive coalition
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
							System.out.println(getAID().getName()+" refused proposal");
						}
					}
					else {
						block();
					}
					
				}
			}
		} );

	}
}


