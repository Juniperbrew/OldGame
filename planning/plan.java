public Entity{

X
Y
Map
Rotation
Sprite

}

public Civilian extends Entity implements Acting, Talkable{
	Vector3 destination;
	String message;
	Rectangle moveArea;

	public act(){
		if(destination != null){
			//If we have a destination walk towards it
		}else{
			//If we dont have a destination find a new one inside moving bounds
			destination = Random.newDestination(moveArea);
		}
	}
	public String interact(){
		return message;
	}
}

public Enemy extends Entity implements Acting{
	public act(){
		//Move towards player
	}
}

public Sign extends Entity implements Talkable{
	String message;

	public String interact(){
		return message;
	}
}

public interface Acting{
	public act(){

	}
}

public interface Talkable{
	public String talk(){
	}
}
