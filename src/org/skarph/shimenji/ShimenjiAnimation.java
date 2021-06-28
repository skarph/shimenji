package org.skarph.shimenji;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Function;

//Represents a single shimenji animation from JSON

public class ShimenjiAnimation {
	String name; //name of the animation
	int animation; //row that this animation should use
	double timeout; //time remaining for this animation, seconds
	double rate; //rate of animation, seconds
	Map<String, Double> next; //next possible animations and their probability of happening
	
	public boolean deltaX; //true if animation computes delta x instead of absolute x
	public boolean deltaY; //true if animation computes delta y instead of absolute y
	
	public Function xFormula; //formula for the animation's X-displacement
	public Function yFormula; //formula for the animation's Y-displacement
	
	
	//width and height of monitor
	//static final Constant WIDTH = new Constant("W", Toolkit.getDefaultToolkit().getScreenSize().getWidth());
	//static final Constant HEIGHT = new Constant("H", Toolkit.getDefaultToolkit().getScreenSize().getWidth());
	
	public ShimenjiAnimation(String name, int animation, double timeout, double rate, String xFormula, String yFormula, Map<String, Double> next, boolean deltaX, boolean deltaY) {
		this.name = name;
		this.animation = animation;
		this.timeout = timeout;
		this.rate = rate;
		this.next = next;
		
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		
		this.xFormula = new Function("f(t,W,H) = " + xFormula);
		this.yFormula = new Function("f(t,W,H) = " + yFormula);
	}
	
	
	public ShimenjiAnimation(String name, HashMap<String, Object> hmJSON) {
		this.name = name;
		this.animation = (Integer) hmJSON.get("animation");
		this.timeout = ((BigDecimal) hmJSON.get("time")).doubleValue();
		this.rate = ((BigDecimal) hmJSON.get("rate")).doubleValue();
		this.next = (HashMap<String, Double>) ((HashMap<String, BigDecimal>) hmJSON.get("next")).entrySet()
				.stream()
				.collect(
						Collectors.toMap(
								e -> e.getKey(), //also we get name twice, maybe make that cleaner???
								e -> e.getValue().doubleValue() //TODO: ensure data is *exactly* in the type we want it
								)
			
						);
		
		this.deltaX = hmJSON.containsKey("dx");
		this.deltaY = hmJSON.containsKey("dy");
		
		this.xFormula = new Function("f(t,W,H) = " + (deltaX ? (String) hmJSON.get("dx") : (String) hmJSON.get("x")) );
		this.yFormula = new Function("f(t,W,H) = " + (deltaY ? (String) hmJSON.get("dy") : (String) hmJSON.get("y")) );
				
	}
	
	public double calculateX(double t, double W, double H) {
		return xFormula.calculate(t,W,H);
	}
	
	public double calculateY(double t, double W, double H) {
		return yFormula.calculate(t,W,H);
	}
}
