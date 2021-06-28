package org.skarph.shimenji;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

public class ShimenjiState {
	
	//indicates whether shimenji is not being dragged, is currently being dragged, or was let go after being dragged
	public enum DragState {
		NORMAL, DRAGGING, DROPPED
	}
	
	Map<String,ShimenjiAnimation> animations;
	public double[] pos = {0.0,0.0}; //screen coordinates
	public double t = 0.0; //time accumulator
	public int spriteW = 1;
	public int spriteH = 1;
	double screenW;
	double screenH;
	ShimenjiAnimation currentAnimation; //init is always first animation
	public DragState dragState = DragState.NORMAL;
	boolean hasMomentum = false;
	double dragMomentum = 0; //momentum on x-axis imparted by mouse when releasing, pixels/second
	int floor = 0; //collision check used when shimenji is dropped
	
	ShimenjiState(Map<String,ShimenjiAnimation> msa){
		animations = msa;
		currentAnimation = animations.get("init"); //init always first
	}
	
	@SuppressWarnings("unchecked")
	ShimenjiState(JSONObject jsono, double W, double H){
		this(
				jsono.toMap()
				.entrySet()
				.stream()
				.filter(e -> ! (e.getKey().equals("#SPRITE_SIZE") || e.getKey().equals("#DRAG") || e.getKey().equals("#RELEASE")) ) //dont pass in the sprite sheet info!
				.collect(
						Collectors.toMap(
								e -> e.getKey(), //also we get name twice, maybe make that cleaner???
								e -> new ShimenjiAnimation( e.getKey(), (HashMap<String,Object>) e.getValue() ) //TODO: ensure data is *exactly* in the type we want it
										)
						)
		    );
		screenW = W;
		screenH = H;
		spriteW = jsono.getJSONArray("#SPRITE_SIZE").getInt(0);
		spriteH = jsono.getJSONArray("#SPRITE_SIZE").getInt(1);
		
		JSONObject jd = jsono.getJSONObject("#DRAG"); 
		animations.put("#DRAG", new ShimenjiAnimation("#DRAG", jd.getInt("animation"), 0.0, ((BigDecimal) jd.get("rate")).doubleValue(), "0", "0", null, true, true));
		JSONObject rd = jsono.getJSONObject("#RELEASE"); 
		animations.put("#RELEASE", new ShimenjiAnimation("#RELEASE", rd.getInt("animation"), 0.0, ((BigDecimal) rd.get("rate")).doubleValue(), "0", rd.getString("dy"),
				(Map<String, Double>) ((Map<String,Object>) rd.getJSONObject("next").toMap()).entrySet()
				.stream()
				.collect(
						Collectors.toMap(
								e -> e.getKey(), //also we get name twice, maybe make that cleaner???
								e -> ((BigDecimal) e.getValue()).doubleValue() //TODO: ensure data is *exactly* in the type we want it
								)
			
						)
				, true, true));
		floor = (int) (new Expression(rd.getString("floor"), new Argument("H", H))).calculate();
	}
	
	
	public double[] update(double dt) {
		t += dt;
		switch(dragState) {
			
		case NORMAL:
			if(t > currentAnimation.timeout) {
				t = 0;
				setOrAddPos(
						currentAnimation.calculateX(1.0,screenW,screenH),
						currentAnimation.calculateY(1.0,screenW,screenH),
						dt
						);
				nextAnimation(Math.random());
			}else {
				setOrAddPos(
						currentAnimation.calculateX(t/currentAnimation.timeout,screenW,screenH),
						currentAnimation.calculateY(t/currentAnimation.timeout,screenW,screenH),
						dt
						);
			}
			break;
		
		case DRAGGING:
			return pos; //no need to check to fix coords
			
		case DROPPED:
			setOrAddPos(dragMomentum, currentAnimation.calculateY(0,screenW,screenH), dt);
			if(pos[1]>floor) {
				pos[1] = floor;
				dragState = DragState.NORMAL;
				nextAnimation(Math.random());
			}
			break;
		}
		
		//could be done with some modulo math, am tired
		if(pos[0] > screenW+spriteW) {
			pos[0] = -spriteW;
		}else if(pos[0] < -spriteW) {
			pos[0] = screenW+spriteW;
		}
		
		if(pos[1] > screenH+spriteH) {
			pos[1] = -spriteH;
		}else if(pos[0] < -spriteH) {
			pos[1] = screenH+spriteH;
		}
		return pos;
	}
	
	//sets or adds x or y values to location, depending on whether the current animation x y is a delta instead of absolute position
	public void setOrAddPos(double x, double y, double dt) {
		if(currentAnimation.deltaX) {
			pos[0] += x*dt;
		}else{
			pos[0] = x;
		}
		
		if(currentAnimation.deltaY) {
			pos[1] += y*dt;
		}else{
			pos[1] = y;
		}
	}
	
	//picks next random animation using 0r<1
	public ShimenjiAnimation nextAnimation(double r) {
		double a = 0;
		for(Entry<String, Double> v : currentAnimation.next.entrySet()) {
			a += v.getValue();
			if(r<=a) {
				currentAnimation = animations.get(v.getKey());
				return currentAnimation;
			}
		}
		return currentAnimation; //should never occur
	}
	public void pickUp() {
		dragState = DragState.DRAGGING;
		currentAnimation = animations.get("#DRAG");
	}
	public void drop(double x, double y, double momentum) {
		dragState = DragState.DROPPED;
		pos[0] = x;
		pos[1] = y;
		if(hasMomentum)
			dragMomentum = momentum;
		currentAnimation = animations.get("#RELEASE");
	}
}
