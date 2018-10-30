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



public class OneByOneStratCovoitAgent extends CovoitAgent {
	
//	protected But but_agent;
//	protected ArrayList<AID> passengers;
//	protected ArrayList<AID> refused;
//	protected int price;
//	protected ArrayList<AID> acquaintances;
//	protected Boolean recruited;
	
	protected void setup() {
		super.setup();
		//System.out.println("current_price setup : "+String.valueOf(current_price));
	}
	
	protected void init() {
		super.init();
		addBehaviour(new DriverBehaviour());
	}

	
	protected void behaviors() {
		//passenger agent behavior, same as Group Strategy
		addBehaviour(new TickerBehaviour(this, 10000) {
			protected void onTick() {
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
						}
						else {
							
							proposal.setPerformative(ACLMessage.REFUSE);
						}
						//System.out.println(proposal.getInReplyTo());
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
		} );
		
		
	}
	
	//driver agent behavior, DIFFERENT FROM GROUP STRATEGY
	protected class DriverBehaviour extends CyclicBehaviour{
		public void action(){
			if(recruited) {
				System.out.println(getAID().getName()+" recruited at the beginning of the loop");
			}
			System.out.println("DriverBehaviour has been called by agent "+getAID().getName());
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
				
				
				int step = 0;
				int i = 0;
				
				switch (step) {
				case 0:
					// Send the cfp to seller i
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					if(!passengers.contains(acquaintances.get(i))) {
						cfp.addReceiver(acquaintances.get(i));
					}
					i++;
					if(i==acquaintances.size())
					{ //so agent stops when it has asked all acquaintances
						step = 2; //MAKE STEP FINAL STEP
						break;
					}
					
					cfp.setContent(String.valueOf(price/(2+passengers.size())));
					cfp.setConversationId("covoit_cfp");
					cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
					myAgent.send(cfp);
					step = 1;
					break;
				case 1:
					//Prepare the template to get proposals
					MessageTemplate mt = MessageTemplate.MatchConversationId("covoit_cfp");
					// Receive all proposals/refusals from seller agents
					ACLMessage answer = myAgent.receive(mt);
					if(answer != null) {
						step = 2;
						if(answer.getPerformative()== ACLMessage.PROPOSE) {
							but_agent.set_nbPlaces(but_agent.get_nbPlaces() - 1);
							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							confirm.addReceiver(answer.getSender());
							confirm.setContent(String.valueOf(price/(2+passengers.size())));
							passengers.add(answer.getSender());
							confirm.setConversationId("covoit");
							myAgent.send(confirm);
							System.out.println(getAID().getName()+" accepted proposal from"+answer.getSender().getName());
							System.out.println("Number of passengers : "+String.valueOf(passengers.size()));
							System.out.println("Remaning seats : "+String.valueOf(but_agent.get_nbPlaces()));
							
							if(but_agent.get_nbPlaces() == 0){
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
						
						if(answer.getPerformative()== ACLMessage.REFUSE){
							System.out.println(getAID().getName()+" refused proposal");
						}
					}
					else {
						block();
					}
					step = 0;
					break;
				case 2: //just to stop the switch-case
					break;
				}
				
				////////////////////////////////////////
				
				
				
				
				
			}
			/*try{
				Thread.sleep(1000);
				//System.out.println("pause");
			}
			catch(InterruptedException e){}*/
		}
	}
}


