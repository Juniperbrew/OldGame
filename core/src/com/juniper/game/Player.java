package com.juniper.game;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class Player extends Sprite{
	
	private String name;
	private float rotation;
	private Animation animation;
	private float stateTime = 0;
	private String map;
	
	public Player(String name, AtlasRegion region){
		super(region);
		this.name = name;
		rotation = -90;
	}
	
	public Player(String name, Animation animation){
		super(animation.getKeyFrame(0));
		this.name = name;
		this.animation = animation;
		rotation = -90;
	}
	
	public Player(Animation animation){
		super(animation.getKeyFrame(0));
		this.animation = animation;
		rotation = -90;
	}
	
	public Player(AtlasRegion region){
		super(region);
		rotation = -90;
	}
	
	public void setMap(String newMap){
		map = newMap;
	}
	public String getMap(){
		return map;
	}
	
	public void addToStateTime(float addTime){
		stateTime += addTime;
	}
	
	public void clearStateTime(){
		stateTime = 0;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public void updateKeyFrame(){
		setRegion(animation.getKeyFrame(stateTime, true));
	}
	
	public void setAnimation(Animation animation){
		this.animation = animation;
		setRegion(animation.getKeyFrame(stateTime));
	}
	
	public void turnTo(float degrees){
		rotation = degrees;
	}
	
	public float getHeading(){
		return rotation;
	}

}
