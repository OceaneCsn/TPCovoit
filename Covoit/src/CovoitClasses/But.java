package CovoitClasses;

import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class But {
	protected String startingCity;
	protected String targetCity;
	protected int leavingTime;
	protected int carScore;
	protected int nbPlaces;
	
	private List<String> cities = Arrays.asList("Lyon","Paris","Montpellier","Bordeaux","Marseille","Toulouse","Lille","Poitiers");
	private List<Integer> scores = Arrays.asList(0,1,2,3,4,5);
	private List<Integer> places = Arrays.asList(1,2,3,4,5,6);
	private List<Integer> times = Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24);
	
	
	//constructor with existing variables to pass
	protected But(String sCity, String tCity, int ltime, int cScore,int nbP) {
		this.startingCity = sCity;
		this.targetCity = tCity;
		this.leavingTime = ltime;
		this.carScore = cScore;
		this.nbPlaces = nbP;
	}
	//constructor when variables must be chosen at random
	protected But() {
		Random rand = new Random();
		this.startingCity = cities.get(rand.nextInt(cities.size()));
		String arrivalCity = cities.get(rand.nextInt(cities.size()));
		while(arrivalCity.equals(startingCity)) {
			arrivalCity = cities.get(rand.nextInt(cities.size()));
		}
		this.targetCity = arrivalCity;
		this.carScore = scores.get(rand.nextInt(scores.size()));
		this.nbPlaces = places.get(rand.nextInt(places.size()));
		this.leavingTime = times.get(rand.nextInt(times.size()));
	}
	
	public String get_startingCity() {
		return this.startingCity;
	}
	
	public String get_targetCity() {
		return this.targetCity;
	}
	
	public int get_leavingTime() {
		return this.leavingTime;
	}
	
	public int get_carScore() {
		return this.carScore;
	}
	
	public int get_nbPlaces() {
		return this.nbPlaces;
	}
	
	public void set_nbPlaces(int newNbPlaces) {
		this.nbPlaces = newNbPlaces;
	}
}
