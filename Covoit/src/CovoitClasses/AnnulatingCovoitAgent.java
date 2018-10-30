package CovoitClasses;
import java.util.*;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


public abstract class AnnulatingCovoitAgent extends CovoitAgent {
	
	// ici toutes les methodes vont etre les memes (init, takedown...) a part les behaviors
	// et le setup dans lequel on ajoute juste la proba d'annuler
	//on redefinit juste la methode des behaviors et les autres seront celles de CovoitAgent
	
	protected double anulation_proba;
	protected AID current_recruiter;
	protected double current_price;
	
	protected void setup() {
		super.setup();
		anulation_proba = 1.0;
	}
	
	protected abstract void behaviors();
	
	protected class update_current_price extends CyclicBehaviour{
		public void action(){
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				if(msg.getConversationId().equals("new price")) {
					//sets the new price to a higher one as a member of the coalition has canceled
					System.out.println("Agent "+getAID().getName()+" has new price : "+msg.getContent()+".");
					current_price = Double.parseDouble(msg.getContent());	
				}
			}
		}
	}

}
