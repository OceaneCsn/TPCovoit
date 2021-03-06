package CovoitClasses;
import java.io.PrintWriter;
import java.util.*;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


/**
 * 
 * When this agent is driver: will contact each potential passenger,
 * then wait for an answer before asking another passenger
 *
 */
public class OneByOneStratCovoitAgent extends CovoitAgent {
	
	protected Boolean finished = true; //FALSE if agent is waiting for answer from a potential passenger; else TRUE
	protected Boolean start_new_asking = true;
	protected int i = 0;
	protected Boolean is_driver = false;
	
	protected void setup() {
		super.setup();
		//System.out.println("current_price setup : "+String.valueOf(current_price));
	}
	
	protected void init() {
		super.init();
	}

	
	protected void behaviors() {
		
		//passenger agent behavior, same as Group Strategy
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
				/*if agent 
				 * - isn't driver of its own coalition
				 * - doesn't belong to another coalition
				 * - isn't already in the process of negotating a deal with another agent
				 */
				if(passengers.size() == 0 && !recruited && !is_driver) {
					
					if(!processing) {
						//prepares template to receive Call For Proposals from another agent
						MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
						ACLMessage msg = myAgent.receive(mt);
						//System.out.println("passed processing");
						if(msg != null) {
							//System.out.println(getAID().getName()+ "received cfp");
							ACLMessage proposal = msg.createReply();
							proposal.setConversationId("covoit_propose");
							// if the proposed price is inferior than the the price of the agent's travel on its own: accept
							if(Float.valueOf(msg.getContent()) <= (float) price) {
								//System.out.println("received price interesting");
								proposal.setPerformative(ACLMessage.PROPOSE);
								proposal.setContent("ok");
							}
							else {
								
								proposal.setPerformative(ACLMessage.REFUSE);
								processing = false; //negociation ends with refusal
							}
							myAgent.send(proposal);
							processing = true; //negociation begins
							System.out.println(getAID().getName()+" sent proposal to "+msg.getSender().getName());
						}
//						else {
//							block();
//						}
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
		
		addBehaviour(new TickerBehaviour(this, 5000) {
			protected void onTick() {
				addBehaviour(new DriverBehaviour());
				//System.out.println(finished);
			} 
		} );
		
		
	}
	
	//driver agent behavior, DIFFERENT FROM GROUP STRATEGY
	protected class DriverBehaviour extends OneShotBehaviour{

		public void action(){
			
			if(recruited) {
				//System.out.println(getAID().getName()+" recruited at the beginning of the loop");
			}
			
			if(!recruited) {
				if (start_new_asking)
				{
					start_new_asking = false;
					i = 0;
					//System.out.println(getAID().getName()+ " entered in driver");
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
						for (int j = 0; j < result.length; ++j) {
							acquaintances.add(result[j].getName());
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
						//System.out.println("for "+getAID().getName()+" there are "+acquaintances.size()+" acquaintances");
	
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
				}
				
				//contact potential passengers one by one
				
				
				if (finished) {
					// Send the cfp to seller i
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					if(i==acquaintances.size())
					{ //so agent stops when it has asked all acquaintances
						start_new_asking = true;
					}
					if(acquaintances.size()>i) {
						if(!passengers.contains(acquaintances.get(i))) {
							cfp.addReceiver(acquaintances.get(i));
						}
					}
					
					
					cfp.setContent(String.valueOf(price/(2+passengers.size())));
					cfp.setConversationId("covoit_cfp");
					cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
					myAgent.send(cfp);
					finished=false;
					if(acquaintances.size()>i)
						System.out.println("cfp sent by "+getAID().getName()+" to "+acquaintances.get(i).getName());
				}

				//Prepare the template to get proposal from agent i
				//ACLMessage buf = receive();
				//flushMessages();
				MessageTemplate mt = MessageTemplate.MatchConversationId("covoit_propose");
				// Receive proposal or refusal from agent i
				ACLMessage answer = myAgent.receive(mt);
				//System.out.println("message received should be propose or refuse, is: "+answer.getPerformative(answer.getPerformative()));
				if(answer != null && !processing) {
					if(answer.getPerformative()== ACLMessage.PROPOSE) { //if agent i accepts
						System.out.println(getAID().getName()+" wants to recruit "+answer.getSender().getName());
						but_agent.set_nbPlaces(but_agent.get_nbPlaces() - 1); //one less seat in this agent's car
						is_driver = true;
						ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						confirm.addReceiver(answer.getSender());
						confirm.setContent(String.valueOf(price/(2+passengers.size())));
						
						passengers.add(answer.getSender()); //agent i added to list of passengers
						
						confirm.setConversationId("covoit");
						myAgent.send(confirm);
						finished=true;
						//give info to whoever is looking at console
						System.out.println(getAID().getName()+" accepted proposal from"+answer.getSender().getName());
						System.out.println("Number of passengers for "+getAID().getName()+": "+String.valueOf(passengers.size()));
						System.out.println("Remaning seats : "+String.valueOf(but_agent.get_nbPlaces()));
						//end of console time
						
						i++;
						
						if(but_agent.get_nbPlaces() == 0){
							//if coalition is complete (no room left)
							coalition_times += String.valueOf(System.currentTimeMillis()-creation_time)+"\r\n";
							try (PrintWriter out = new PrintWriter("Coalition_times.txt")) {
							    out.println(coalition_times);
							}
							catch(Exception e){System.out.println(e);} 
							
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
					
					if(answer.getPerformative()== ACLMessage.REFUSE){
						//if agent i refused
						System.out.println(getAID().getName()+" refused proposal");
						finished=true;
						i++;
					}
				}
				else {
					if(processing)
						finished=true;
					else
						block();
				}
				
				
				
//				switch (step) {
//				case 0:
//					if (finished) {
//						// Send the cfp to seller i
//						ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
//						if(acquaintances.size()>0) {
//							if(!passengers.contains(acquaintances.get(i))) {
//								cfp.addReceiver(acquaintances.get(i));
//							}
//						}
//						if(i==acquaintances.size())
//						{ //so agent stops when it has asked all acquaintances
//							step = 2;
//							break;
//						}
//						
//						cfp.setContent(String.valueOf(price/(2+passengers.size())));
//						cfp.setConversationId("covoit_cfp");
//						cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
//						myAgent.send(cfp);
//						finished=false;
//						System.out.println("cfp sent by "+getAID().getName());
//					}
//					System.out.println("step is now:");
//					step = 1;
//					System.out.print(step);
//					break;
//				case 1:
//					//Prepare the template to get proposal from agent i
//					System.out.println("entered case 1 of switch");
//					MessageTemplate mt = MessageTemplate.MatchConversationId("covoit_cfp");
//					// Receive proposal or refusal from agent i
//					ACLMessage answer = myAgent.receive(mt);
//					if(answer != null) {
//						if(answer.getPerformative()== ACLMessage.PROPOSE) { //if agent i accepts
//							System.out.println(getAID().getName()+" wants to recruit "+answer.getSender().getName());
//							but_agent.set_nbPlaces(but_agent.get_nbPlaces() - 1); //one less seat in this agent's car
//							
//							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
//							confirm.addReceiver(answer.getSender());
//							confirm.setContent(String.valueOf(price/(2+passengers.size())));
//							
//							passengers.add(answer.getSender()); //agent i added to list of passengers
//							
//							confirm.setConversationId("covoit");
//							myAgent.send(confirm);
//							finished=true;
//							//give info to whoever is looking at console
//							System.out.println(getAID().getName()+" accepted proposal from"+answer.getSender().getName());
//							System.out.println("Number of passengers : "+String.valueOf(passengers.size()));
//							System.out.println("Remaning seats : "+String.valueOf(but_agent.get_nbPlaces()));
//							//end of console time
//							
//							if(but_agent.get_nbPlaces() == 0){
//								//if coalition is complete (no room left)
//								coalition_times += String.valueOf(System.currentTimeMillis()-creation_time)+"\r\n";
//								try (PrintWriter out = new PrintWriter("Coalition_times.txt")) {
//								    out.println(coalition_times);
//								}
//								catch(Exception e){System.out.println(e);} 
//								
//								//kills all the agents as they all formed their definitive coalition
//								for(AID a : passengers) {
//									ACLMessage die = new ACLMessage(ACLMessage.REQUEST);
//									die.addReceiver(a);
//									die.setConversationId("apoptosis");
//									myAgent.send(die);
//								}
//								doDelete();
//							}
//						}
//						
//						if(answer.getPerformative()== ACLMessage.REFUSE){
//							//if agent i refused
//							System.out.println(getAID().getName()+" refused proposal");
//							finished=true;
//						}
//					}
//					else {
//						block();
//					}
//					step = 0; //go back to step 0 and contact agent i+1
//					i++;
//					break;
//				case 2: //just to stop the switch-case once there are no more agents to contact
//					break;
//				}
				//finished = true; //now another cycle of DriverBehaviour can be started by DriverLoop
			}
			
			 
		}
	}
	
	/**
	 * 
	 * will only call DriverBehaviour if previous cycle of DriverBehaviour is over
	 * (aka if finished == true)
	 * (avoids having 2+ cycles of DriverBehaviours running at the same time,
	 * which sort of defeats the point of OneByOne strategy)
	 *
	 */
//	protected class DriverLoop extends CyclicBehaviour{
//		public void action() {
//			
//			if(finished)
//				addBehaviour(new DriverBehaviour());
//				//System.out.println(finished);
//		}
//	}
	
}


