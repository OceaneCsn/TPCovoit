package CovoitClasses;
import java.io.PrintWriter;
import java.util.*;


//import CovoitClasses.CovoitAgent.PleaseDie;
import jade.core.AID;
import jade.core.Agent;
import jade.core.AID;
import java.util.Collections;

import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;




public class PatientCovoitAgent extends CovoitAgent {
	


	protected int counter;
	protected int waiting_time = 5;
	private HashMap<AID, Double> propositions = new HashMap<AID, Double>();
	protected double current_price;
	
	protected void setup() {
		super.setup();
		counter = 0;
		
	}

	
	protected void behaviors() {
		current_price = price;
		//passenger agent behavior : now the agent will wait waiting time to send a proposal, to its best cfp
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
				if(passengers.size() == 0 && !recruited) {
					
					//checks if waiting_time has passed to contact the most interesting driver
					if(counter % waiting_time==0 && counter > 0) {
						
						if(!propositions.isEmpty()) {
							//takes the more interesting price and sends a proposition to
							//the corresponding agent
							System.out.println("propositions de "+getAID().getName());
							for(AID a : propositions.keySet()) {
								System.out.println(a.getName()+" : "+String.valueOf(propositions.get(a)));
							}
							Map.Entry<AID, Double> maxEntry = null;
							for (Map.Entry<AID, Double> entry : propositions.entrySet())
							{
							    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
							    {
							        maxEntry = entry;
							    }
							}
							
							AID bestDriver = maxEntry.getKey();
							System.out.println("sending propose to "+bestDriver.getName());
							ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);
							proposal.addReceiver(bestDriver);
							proposal.setContent("ok");
							proposal.setConversationId("covoit_cfp");
							myAgent.send(proposal);
							//removes everything from the propositions memory for next session
							propositions.clear();
						}
						
						
					}
					
					
					MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					ACLMessage msg = myAgent.receive(mt);

					if(msg != null) {
						// if the proposed price is inferior than the the price of the agent's travel on its own
						if(Double.parseDouble(msg.getContent()) <= current_price){
							//on stocke ce driver dans une hashmap
							propositions.put(msg.getSender(),Double.parseDouble(msg.getContent()));
							System.out.println(getAID().getName()+ " stored the nice price from "+msg.getSender().getName());
						}
					}
					else {
						block();
					}
					MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
					ACLMessage msg2 = myAgent.receive(mt2);
					if(msg2 != null) {
						System.out.println(getAID().getName()+" recruited");
						recruited = true;
						current_price = Double.parseDouble(msg2.getContent());
					}
					else {
						block();
					}
				}
				counter ++;
			}
		} );
		

		//driver agent behavior, same as covoit agent
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected MessageTemplate mt;
			//protected Boolean already_recruited;
			protected void onTick() {
				if(!recruited) {
					current_price = price/(1+passengers.size());
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
					
					
					//envoi aux passagers potentiels
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
				
							but_agent.set_nbPlaces(but_agent.get_nbPlaces() - 1);
							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							confirm.addReceiver(reply.getSender());
							confirm.setContent(String.valueOf(price/(2+passengers.size())));
							current_price = Double.parseDouble(confirm.getContent());
							passengers.add(reply.getSender());
							confirm.setConversationId("covoit");
							myAgent.send(confirm);
							System.out.println(getAID().getName()+" accepted proposal from"+reply.getSender().getName());
							System.out.println("Number of passengers : "+String.valueOf(passengers.size()));
							System.out.println("Remaning seats : "+String.valueOf(but_agent.get_nbPlaces()));
							
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


