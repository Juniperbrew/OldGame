package com.juniper.game;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.juniper.network.Network;
import com.juniper.network.Network.ChatMessage;
import com.juniper.network.Network.MoveTo;
import com.juniper.network.Network.Register;
import com.juniper.network.Network.UpdateClients;
import com.badlogic.gdx.math.MathUtils;

public class GdxSandbox implements ApplicationListener, InputProcessor{
	
	Client client;
	
	//Gui and graphics
	private SpriteBatch batch; //Probably not needed
	private Skin skin;
	private Stage stage;
	private Table menuLayout;
	private Table chatLayout;
	private Table inGameLayout;
	private TextArea chatArea;
	HashMap<String,TextField> chatBubbles;
	private OrthographicCamera camera;
	float w;
	float h;
	private TextField gameChat;
	private TextArea messageWindow;
	
	//Player sprite
	private TextureAtlas textureAtlas;
	private String currentAtlasKey;
	private Animation animationE;
	private Animation animationN;
	private Animation animationNW;
	private Animation animationW;
	private Animation animationSW;
	private Animation animationS;
	private Animation animationNE;
	private Animation animationSE;
	final private float SPRITE_SCALE = 2.0f;
	
	//Local player state
	Player player;
	String playerName;
	final int SPEED = 500; //100 pix per sec
	private String playerLatestTeleportDestination;
	
	//World state
	String startingMap = "untitled.tmx";
	private HashMap<String, TiledMap> maps;
	private HashMap<String, OrthogonalTiledMapRendererWithSprites> mapRenderers;
	HashMap<String,Player> players;
	private List<String> nameList;
	
	//Current map
	private String activeMapName;
	private TiledMap tiledMap;
	private MapLayer objectLayer; 
	private OrthogonalTiledMapRendererWithSprites tiledMapRenderer;
	private int tileHeight;
	private int tileWidth;
	
	//Game state
	private boolean startGame = false;
	boolean left;
	boolean right;
	boolean up;
	boolean down;
	private boolean strafeLeft;
	private boolean strafeRight;
	private boolean forward;
	private boolean backward;

	float debugTimer;
	float networkUpdateTimer;
	//final float networkUpdateInterval = 0.01f;

	final static int TIME_ZONE_OFFSET_MILLISECONDS = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
	private FPSLogger fpsLogger;
	
	public GdxSandbox(){

	}

	public GdxSandbox(String mapName){
		startingMap = mapName.toLowerCase();
	}
	
	@Override
	public void create() {    

		fpsLogger = new FPSLogger();
		batch = new SpriteBatch();
		skin = new Skin(Gdx.files.internal("data/uiskin.json"));
		stage = new Stage(new ScreenViewport(), batch);
		Gdx.input.setInputProcessor(stage);

		players = new HashMap<String,Player>();
		chatBubbles = new HashMap<String,TextField>();
		
		maps = new HashMap<String,TiledMap>();
		mapRenderers = new HashMap<String,OrthogonalTiledMapRendererWithSprites>();
		
		textureAtlas = new TextureAtlas(Gdx.files.internal("data/spritesheetindexed.atlas"));
		
		animationN = new Animation(1/10f, textureAtlas.findRegions("n"));
		animationNW = new Animation(1/10f, textureAtlas.findRegions("nw"));
		animationW = new Animation(1/10f, textureAtlas.findRegions("w"));
		animationSW = new Animation(1/10f, textureAtlas.findRegions("sw"));
		animationS = new Animation(1/10f, textureAtlas.findRegions("s"));
		animationSE = new Animation(1/10f, textureAtlas.findRegions("se"));
		animationE = new Animation(1/10f, textureAtlas.findRegions("e"));
		animationNE = new Animation(1/10f, textureAtlas.findRegions("ne"));

		//player = new Sprite(texture);
		
		w = Gdx.graphics.getWidth();
		h = Gdx.graphics.getHeight();
		
        //AtlasRegion region = textureAtlas.findRegion("s");
		camera = new OrthographicCamera();
		camera.setToOrtho(false,w,h);
		camera.update();
		System.out.println("Local storage path: " + Gdx.files.getLocalStoragePath());
		
		tiledMap = new TmxMapLoader().load(startingMap);
		maps.put(startingMap, tiledMap);
		
		activeMapName = startingMap;
		
		//Retrieve objectlayer
		if(tiledMap.getLayers().get("Object Layer") != null){
			objectLayer = tiledMap.getLayers().get("Object Layer");
		}
		
		tileHeight = (Integer) tiledMap.getProperties().get("tileheight");
		tileWidth = (Integer) tiledMap.getProperties().get("tilewidth");
		tiledMapRenderer = new OrthogonalTiledMapRendererWithSprites(tiledMap);
		
		tiledMapRenderer.showSpecialObjects(false);
		
		mapRenderers.put(startingMap, tiledMapRenderer);
		
		printMapProperties();

		createGUI();
	}
	
	private void changeMap(String mapName){
		boolean showSpecialObjects = tiledMapRenderer.areSpecialObjectsVisible();
		tiledMap = maps.get(mapName);
		tiledMapRenderer = mapRenderers.get(mapName);
		tiledMapRenderer.showSpecialObjects(showSpecialObjects);
		activeMapName = mapName;
		
		//Update objectlayer
		if(tiledMap.getLayers().get("Object Layer") != null){
			objectLayer = tiledMap.getLayers().get("Object Layer");
		}
	}
	
	private void loadSubMaps(MapLayer layer){
		for(MapObject object : layer.getObjects()){
			if(object.getProperties().containsKey("exit")){
				object.getProperties().get("exit");
				String mapName = ((String) object.getProperties().get("exit")).toLowerCase();
				System.out.println("Loading " + mapName);
				TiledMap newMap = new TmxMapLoader().load(mapName);
				OrthogonalTiledMapRendererWithSprites newTiledMapRenderer = new OrthogonalTiledMapRendererWithSprites(newMap);
				maps.put(mapName, newMap);
				mapRenderers.put(mapName, newTiledMapRenderer);
				
				for(OrthogonalTiledMapRendererWithSprites mapRenderer: mapRenderers.values()){
					System.out.println(mapRenderer);
				}
			}
		}
	}

	private void addPlayerToMap(Player player, String mapName){
		//First remove player from all maps
		for(OrthogonalTiledMapRendererWithSprites mapRenderer : mapRenderers.values()){
			mapRenderer.removeSprite(player);
		}
		System.out.println("Adding " + player.getName() + " to " + mapName);
		mapRenderers.get(mapName).addSprite(player);
		player.setMap(mapName);
	}
	
	private void rotatePlayer(Player player, float degrees){
		player.turnTo(degrees);
		
		if(-22.5 <= degrees && degrees < 22.5){
			 currentAtlasKey = "e";
			 player.setAnimation(animationE);
		}
		if(22.5 <= degrees && degrees < 67.5){
			 currentAtlasKey = "ne";
			 player.setAnimation(animationNE);
		}
		if(67.5 <= degrees && degrees < 112.5){
			 currentAtlasKey = "n";
			 player.setAnimation(animationN);
		}
		if(112.5 <= degrees && degrees < 157.5){
			 currentAtlasKey = "nw";
			 player.setAnimation(animationNW);
		}
		if(157.5 <= degrees || degrees < -157.5){
			 currentAtlasKey = "w";
			 player.setAnimation(animationW);
		}
		if(-157.5 <= degrees && degrees < -112.5){
			 currentAtlasKey = "sw";
			 player.setAnimation(animationSW);
		}
		if(-112.5 <= degrees && degrees < -67.5){
			 currentAtlasKey = "s";
			 player.setAnimation(animationS);
		}
		if(-67.5 <= degrees && degrees < -22.5){
			 currentAtlasKey = "se";
			 player.setAnimation(animationSE);
		}
        //player.setRegion(textureAtlas.findRegion(currentAtlasKey));
	}

	
	private void printMapProperties(){
		//Print map properties
		MapProperties mapProperties = tiledMap.getProperties();
		Iterator<String> iterMapKeys = mapProperties.getKeys();
		Iterator<Object> iterMapValues = mapProperties.getValues();
		while(iterMapKeys.hasNext()){
			System.out.println(iterMapKeys.next() + ": " + iterMapValues.next());
		}

		MapLayers layers = tiledMap.getLayers();
		System.out.println("Layer count: " + layers.getCount());
		Iterator<MapLayer> iterLayers = layers.iterator();
		//Print layer names, types and properties
		while (iterLayers.hasNext()){
			MapLayer layer = iterLayers.next();
			System.out.println(layer.getName() + ": " + layer.getClass());

			MapProperties layerProperties = layer.getProperties();
			Iterator<String> iterLayerKeys = layerProperties.getKeys();
			Iterator<Object> iterLayerValues = layerProperties.getValues();
			while(iterLayerKeys.hasNext()){
				System.out.println("    " + iterLayerKeys.next() + ": "+ iterLayerValues.next());
			}
			if(layer instanceof TiledMapTileLayer){
				TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
				//Print tile(0,0) properties for each layer
				TiledMapTileLayer.Cell cell = tileLayer.getCell(0, 0);
				if(cell == null){
					System.out.println("       Tile (0,0)| is null");
				}else{
					TiledMapTile tile = cell.getTile();
					MapProperties tileProperties = tile.getProperties();
					Iterator<String> iterTileKeys = tileProperties.getKeys();
					Iterator<Object> iterTileValues = tileProperties.getValues();
					while(iterTileKeys.hasNext()){
						System.out.println("       Tile (0,0)| " + iterTileKeys.next() + ": " + iterTileValues.next());
					}
				}
			}
		}
	}


	private void createGUI(){
		Label nameLabel = new Label("Name:", skin);
		final TextField nameText = new TextField("frog", skin);
		Label addressLabel = new Label("Address:", skin);
		final TextField addressText = new TextField("localhost", skin);
		final TextButton chatButton = new TextButton("Chat", skin, "default");
		final TextButton playButton = new TextButton("Play", skin, "default");

		nameText.selectAll();

		nameText.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent event, int keycode){
				if(keycode == Input.Keys.ENTER){
					stage.setKeyboardFocus(addressText);
					addressText.selectAll();
				}
				return true;
			}
		});

		addressText.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent event, int keycode){
				if(keycode == Input.Keys.ENTER){
					playButton.setText("Connecting..");
					joinServer(addressText.getText(), nameText.getText());
					System.out.println("Joining " + addressText.getText());
				}
				return true;
			}
		});

		playButton.addListener(new ClickListener(){
			@Override 
			public void clicked(InputEvent event, float x, float y){
				playButton.setText("Connecting..");
				menuLayout.setVisible(false);
				inGameLayout.setVisible(true);
				Gdx.input.setInputProcessor(GdxSandbox.this);
				joinServer(addressText.getText(), nameText.getText());
				System.out.println("Joining play" + addressText.getText());
			}
		});

		chatButton.addListener(new ClickListener(){
			@Override 
			public void clicked(InputEvent event, float x, float y){
				chatButton.setText("Connecting..");
				menuLayout.setVisible(false);
				chatLayout.setVisible(true);
				startGame = false;
				joinServer(addressText.getText(), nameText.getText());
				System.out.println("Joining chat" + addressText.getText());
			}
		});


		//MENU
		menuLayout = new Table();
		//table.debug(); // turn on all debug lines (table, cell, and widget)
		//table.debugTable(); // turn on only table lines

		menuLayout.setFillParent(true);
		menuLayout.add(nameLabel);
		menuLayout.add(nameText).width(150);
		menuLayout.row();
		menuLayout.add(addressLabel);
		menuLayout.add(addressText).width(150);
		menuLayout.row();
		menuLayout.add(playButton).width(100).padTop(20);
		menuLayout.add(chatButton).width(100).padTop(20);

		//CHAT
		chatLayout = new Table();
		Table subChatLayout = new Table();
		//chatLayout.debug(); // turn on all debug lines (table, cell, and widget)
		//chatLayout.debugTable(); // turn on only table lines

		chatLayout.setFillParent(true);
		nameList = new List<String>(skin);
		chatArea = new TextArea("", skin);
		final TextField messageArea = new TextField("", skin);
		chatArea.setDisabled(true);

		messageArea.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent event, int keycode){
				if(keycode == Input.Keys.ENTER){
					ChatMessage chatMessage = new ChatMessage();
					chatMessage.text = messageArea.getText();
					messageArea.setText("");
					client.sendTCP(chatMessage);
				}
				return true;
			}
		});

		subChatLayout.add(chatArea).expand().fill();
		subChatLayout.row();
		subChatLayout.add(messageArea).expandX().fillX();
		chatLayout.add(subChatLayout).expand().fill();
		chatLayout.add(nameList).width(150).top();
		chatLayout.setVisible(false);

		//Game GUI
		inGameLayout = new Table();
		inGameLayout.setFillParent(true);
		//inGameLayout.debug();
		//chatBubble = new TextField("", skin);
		//chatBubble.setVisible(false);
		gameChat = new TextField("",skin);
		gameChat.setVisible(false);
		gameChat.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent event, int keycode){
				if(keycode == Input.Keys.ENTER){
					ChatMessage chatMessage = new ChatMessage();
					chatMessage.text = gameChat.getText();

					if(gameChat.getText().length() > 0){
						//show own chat bubble
						//showChatBubble(playerName, gameChat.getText());				
					}
					gameChat.setText("");
					client.sendTCP(chatMessage);
					gameChat.setVisible(false);
					Gdx.input.setInputProcessor(GdxSandbox.this);
				}
				return true;
			}
		});
		messageWindow = new TextArea("" , skin);
		messageWindow.setVisible(false);

		inGameLayout.add(messageWindow).bottom().expand().fillX().height(200);
		inGameLayout.row();
		inGameLayout.add(gameChat).bottom().expandX().fillX();
		inGameLayout.setVisible(false);


		stage.addActor(chatLayout);
		stage.addActor(menuLayout);
		stage.addActor(inGameLayout);

		stage.setKeyboardFocus(nameText);
	}

	private void showChatBubble(final String playerName, String message){
		//Ignore if no chat bubble exists yet
		if(chatBubbles.get(playerName) == null){
			System.out.println(playerName + " does not yet have a chat bubble to show message: " + message);
			return;
		}
		chatBubbles.get(playerName).setText(message);
		chatBubbles.get(playerName).setVisible(true);

		//CHAT BUBBLE POSITION
		float bubbleX = players.get(playerName).getX() + players.get(playerName).getWidth()/2;
		float bubbleY = players.get(playerName).getY() + players.get(playerName).getHeight()/2 + 50;
		Vector3 bubbleProjected = camera.project(new Vector3(bubbleX, bubbleY, 0));
		chatBubbles.get(playerName).setCenterPosition(bubbleProjected.x, bubbleProjected.y);

		//Hide chat bubble after 5 seconds and remove old timers
		Task chatBubbleFade = new Task() {

			@Override
			public void run() {
				chatBubbles.get(playerName).setVisible(false);
			}

		};
		Timer.schedule(chatBubbleFade, 5);
	}

	private void joinServer(final String host, final String name){
		client = new Client();
		client.start();
		
		playerName = name;
        player = new Player(name, animationS);
        
		//Find spawnpoint
		Rectangle spawn = ((RectangleMapObject) objectLayer.getObjects().get("Spawn")).getRectangle();
		float spawnX = spawn.x + (spawn.width/2) - (player.getWidth()/2);
		float spawnY = spawn.y + (spawn.height/2) - (player.getHeight()/2);
		player.setPosition(spawnX, spawnY);
		
		player.setScale(SPRITE_SCALE);
		players.put(playerName, player);
		
		addPlayerToMap(player, activeMapName);
		
		//Load submaps
		loadSubMaps(objectLayer);	

		
		
		TextField chatBubble = new TextField("",skin);
		chatBubble.setVisible(false);
		chatBubbles.put(playerName, chatBubble);
		stage.addActor(chatBubble);
		
		startGame = true;

		// For consistency, the classes to be sent over the network are
		// registered by the same method for both the client and server.
		Network.register(client);

		client.addListener(new Listener() {
			public void connected (Connection connection) {
				Register register = new Register();
				register.name = name;
				register.position = new Vector3(player.getX(), player.getY(), 0);
				register.rotation = player.getHeading();
				register.map = player.getMap();
				client.sendTCP(register);
			}

			public void received (Connection connection, Object object) {

				if (object instanceof UpdateClients) {
					UpdateClients updateClients = (UpdateClients)object;
					//System.out.println("Update received " + updateClients.names.length + " " + updateClients.positions.length + " " + players.size());
					if(updateClients != null && updateClients.names != null && updateClients.names.length > 0){
						if(Arrays.asList(updateClients.names).contains(null)){
							System.out.println("SERVER SENT CLIENT NAMELIST CONTAINING NULL");
						}else{
							nameList.setItems(updateClients.names);
						}
					}

					if(updateClients.names.length != players.size()){
						System.out.println(updateClients.names.length + " " + updateClients.positions.length + " " + players.size());
						System.out.println("ARRAY SIZES NOT MATCHING");

						//If local playerlist smaller than servers playerlist, add existing players to player list
						if(players.size()<updateClients.names.length){
							//Loop through the servers namelist
							for(int i = 0; i < updateClients.names.length; i++ ){
								System.out.println("i=" + i);
								System.out.println("Does playerlist contain " + updateClients.names[i]);
								//If player doesnt exist in local list we add it
								if(!players.containsKey(updateClients.names[i])){
									System.out.println("Adding player " + updateClients.names[i] + " to " + updateClients.maps[i]);
									
							        AtlasRegion region = textureAtlas.findRegion("s");
									Player newPlayer = new Player(updateClients.names[i], animationS);
									newPlayer.setScale(SPRITE_SCALE);
									System.out.println("Scaling" + updateClients.names[i]);
							        rotatePlayer(newPlayer, updateClients.rotations[i]);
							        newPlayer.setMap(updateClients.maps[i]);
							        addPlayerToMap(newPlayer, updateClients.maps[i]);
									players.put(updateClients.names[i], newPlayer);
									
									TextField chatBubble = new TextField("",skin);
									chatBubble.setVisible(false);
									chatBubbles.put(updateClients.names[i], chatBubble);
									stage.addActor(chatBubble);
									//tiledMapRenderer.addSprite(newPlayer);
								}
							}
							//If local playerlist bigger than server playerlist, remove the non existing players
						}else if(players.size() > updateClients.names.length){
							//Loop through local playerlist
							Iterator<Entry<String, Player>> it = players.entrySet().iterator();
							while (it.hasNext()) {
								Map.Entry <String,Player> pairs = it.next();
								if(!Arrays.asList(updateClients.names).contains(pairs.getKey())){
									//If server list doesnt contain the local playerlist name remove it
									System.out.println("Removing player " + pairs.getKey());
									Player removedPlayer = pairs.getValue();
									players.remove(pairs.getKey());
									//REMOVE CHATBUBBLE FROM STAGE???
									stage.getActors().removeValue(chatBubbles.get(pairs.getKey()), true);
									chatBubbles.remove(pairs.getKey());
									//REMOVE SPRITE FROM MAP RENDERER??								
									mapRenderers.get(removedPlayer.getMap()).removeSprite(removedPlayer);
								}
							}
						}
					}else{
						for(int i = 0; i < players.size(); i++){

							if(updateClients.names[i].equals(playerName)){
								//Don't update own position
							}else{
								Player updatingPlayer = players.get(updateClients.names[i]);
								updatingPlayer.setPosition(updateClients.positions[i].x, updateClients.positions[i].y);
								rotatePlayer(updatingPlayer, updateClients.rotations[i]);
								
								//If players map has changed we change it
								if(!updatingPlayer.getMap().equals(updateClients.maps[i])){
									updatingPlayer.setMap(updateClients.maps[i]);
									addPlayerToMap(updatingPlayer, updateClients.maps[i]);
								}

								
							}
						}
					}
					return;
				}

				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;

					int rawSeconds = (int) ((TimeUtils.millis() + TIME_ZONE_OFFSET_MILLISECONDS)/1000);


					int hours = (rawSeconds/3600) % 24;
					int minutes = (rawSeconds/60) % 60;
					int seconds = rawSeconds % 60;

					String timestamp = String.format("[%02d:%02d:%02d]", hours, minutes, seconds);

					String message = timestamp + " " + chatMessage.name + ": " + chatMessage.text;
					chatArea.setText(chatArea.getText() + "\n" + message);
					System.out.println(message);
					if(startGame){
						showChatBubble(chatMessage.name, chatMessage.text);
					}
					return;
				}
			}

			public void disconnected (Connection connection) {
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						chatArea.setText(chatArea.getText() + "\n" + "Disconnected from server.");
					}
				});
			}
		});

		// We'll do the connect on a new thread so the ChatFrame can show a progress bar.
		// Connecting to localhost is usually so fast you won't see the progress bar.
		new Thread("Connect") {
			public void run () {
				try {
					client.connect(5000, host, Network.port);
					// Server communication after connection can go here, or in Listener#connected().
				} catch (IOException ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
		}.start();
	}

	@Override
	public void dispose() {
		batch.dispose();
	}
	
	private void checkCollissionsOnAxis(float oldAxisPos, boolean xAxis){
		//Check collission on X or Y axis
		
		//Find the tiles player is occupying
		//Bottom left corner
		int bottomLeftX = (int) Math.floor(player.getX()/tileWidth);
		int bottomLeftY = (int) Math.floor(player.getY()/tileHeight);
		//System.out.println("Bottom left: " + bottomLeftX + "," + bottomLeftY);
		//Top right corner
		int topRightX = (int) Math.floor((player.getX() + player.getWidth())/tileWidth);
		int topRightY = (int) Math.floor((player.getY() + player.getHeight())/tileHeight);
		//System.out.println("Top right: " + topRightX + "," + topRightY);			
		
		//Loop all tiles player is occupying
		collissiondetection: for(int x = bottomLeftX; x <= topRightX; x++){
			for(int y = bottomLeftY; y <= topRightY; y++){
				Iterator<MapLayer> iterLayers = tiledMap.getLayers().iterator();
				while (iterLayers.hasNext()){
					MapLayer layer = iterLayers.next();
					if(layer instanceof TiledMapTileLayer){
						TiledMapTileLayer.Cell cell = ((TiledMapTileLayer) layer).getCell(x, y);
						if(cell != null){
							if(cell.getTile().getProperties().containsKey("blocked")){
								//Collission revert axis movement
								if(xAxis){
									player.setX(oldAxisPos);
								}else{
									player.setY(oldAxisPos);
								}
								break collissiondetection;
							}
						}
					}else{ //Check collissions with objects
						MapObjects objects = layer.getObjects();
						
						//Iterate all map objects
						for(MapObject object : objects){
							if(object instanceof RectangleMapObject){
								//RectangleMapObject rectangleObject = (RectangleMapObject) object;
								Rectangle rectangle = ((RectangleMapObject) object).getRectangle();
								
								if(object.getProperties().containsKey("gid")){
									//If object is tile object we need to create rectangle size from tilesize
									rectangle.setSize(tileWidth, tileHeight);
								}
								
								//Collission area
								if(object.getProperties().containsKey("blocked")){
									if(player.getBoundingRectangle().overlaps(rectangle)){
										//Collission revert axis movement
										if(xAxis){
											player.setX(oldAxisPos);
										}else{
											player.setY(oldAxisPos);
										}
										break collissiondetection;
									}
								}
								//Map exit
								if(object.getProperties().containsKey("exit")){
									//If teleport area fully contains player we exit him
									if(rectangle.contains(player.getBoundingRectangle())){
										if(playerLatestTeleportDestination != null && object.getName().equals(playerLatestTeleportDestination)){
											//Dont teleport back if we just teleported here
										}else{
											String destinationMap = ((String) object.getProperties().get("exit")).toLowerCase();
											String destinationObject = (String) object.getProperties().get("location");
											System.out.println("Exiting to " + destinationObject + " in " + destinationMap);
											changeMap(destinationMap);
											addPlayerToMap(player, destinationMap);
											
											MapLayer objectLayer = tiledMap.getLayers().get("Object Layer");
											Rectangle destRect = ((RectangleMapObject) objectLayer.getObjects().get(destinationObject)).getRectangle();
											float playerX = destRect.x + (destRect.width/2) - (player.getWidth()/2);
											float playerY = destRect.y + (destRect.height/2) - (player.getHeight()/2);
											player.setPosition(playerX, playerY);
											//Remember last teleport destination to prevent infinite loop teleport
											playerLatestTeleportDestination = destinationObject;
											break collissiondetection;
										}			
									}
									if(object.getName().equals(playerLatestTeleportDestination)){
										//If player no longer touches latest teleport destination we allow him to teleport again
										if(!rectangle.overlaps(player.getBoundingRectangle())){
											playerLatestTeleportDestination = null;
										}
									}
								}
								//Teleport area
								if(object.getProperties().containsKey("destination")){
									//If teleport area fully contains player we move him to destination
									if(rectangle.contains(player.getBoundingRectangle())){
										if(playerLatestTeleportDestination != null && object.getName().equals(playerLatestTeleportDestination)){
											//Dont teleport back if we just teleported here
										}else{
											String destination = (String) object.getProperties().get("destination");
											System.out.println("Destination: " + destination);
											Rectangle destRect = ((RectangleMapObject)objects.get(destination)).getRectangle();
											float playerX = destRect.x + (destRect.width/2) - (player.getWidth()/2);
											float playerY = destRect.y + (destRect.height/2) - (player.getHeight()/2);
											player.setPosition(playerX, playerY);
											//Remember last teleport destination to prevent infinite loop teleport
											playerLatestTeleportDestination = destination;
											break collissiondetection;
										}
									}
									if(object.getName().equals(playerLatestTeleportDestination)){
										//If player no longer touches latest teleport destination we allow him to teleport again
										if(!rectangle.overlaps(player.getBoundingRectangle())){
											playerLatestTeleportDestination = null;
										}
									}
								}
							}
							//Ignore all objects that are not rectangles
						}
					}
				}
			}
		}
	}

	@Override
	public void render() {        
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if(startGame){
			
			float delta = Gdx.graphics.getDeltaTime();
			
			
			//Move on Y axis
			float oldY = player.getY();
			//if(up) player.translateY(SPEED*delta);
			//if(down) player.translateY(SPEED*delta*-1);
			
			if(strafeLeft){
				float strafeAngle = player.getHeading() + 90;
				float deltaY = MathUtils.sinDeg(strafeAngle)*(SPEED*delta);
				player.translateY(deltaY);

			}
			if(strafeRight){
				float strafeAngle = player.getHeading() - 90;
				float deltaY = MathUtils.sinDeg(strafeAngle)*(SPEED*delta);
				player.translateY(deltaY);
			}
			if(forward){
				float deltaY = MathUtils.sinDeg(player.getHeading())*(SPEED*delta);
				player.translateY(deltaY);
			}
			if(backward){
				float deltaY = MathUtils.sinDeg(player.getHeading())*(SPEED*delta*-1);
				player.translateY(deltaY);
			}
			
			//Check collission on Y axis
			checkCollissionsOnAxis(oldY, false);
			
			//Move on X axis
			float oldX = player.getX();
			//if(left) player.translateX(SPEED*delta*-1);
			//if(right) player.translateX(SPEED*delta);
			
			if(strafeLeft){
				float strafeAngle = player.getHeading() + 90;
				float deltaX = MathUtils.cosDeg(strafeAngle)*(SPEED*delta);
				player.translateX(deltaX);

			}
			if(strafeRight){
				float strafeAngle = player.getHeading() - 90;
				float deltaX = MathUtils.cosDeg(strafeAngle)*(SPEED*delta);
				player.translateX(deltaX);
			}
			if(forward){
				float deltaX = MathUtils.cosDeg(player.getHeading())*(SPEED*delta);
				player.translateX(deltaX);
			}
			if(backward){
				float deltaX = MathUtils.cosDeg(player.getHeading())*(SPEED*delta*-1);
				player.translateX(deltaX);
			}
			
			//Check collission on X axis
			checkCollissionsOnAxis(oldX, true);

			//Update player animations
			if(forward || backward || strafeLeft || strafeRight){
				player.addToStateTime(Gdx.graphics.getDeltaTime());
			}else{
				player.clearStateTime();
			}
			
			
			//Keep chat bubbles over players
			if(chatBubbles != null){
				Iterator<Entry<String, TextField>> it = chatBubbles.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry <String,TextField> pairs = (Map.Entry<String,TextField>)it.next();
					String name = pairs.getKey();

					float bubbleX = players.get(name).getX() + players.get(name).getWidth()/2;
					float bubbleY = players.get(name).getY() + players.get(name).getHeight()/2 + 50;
					Vector3 bubbleProjected = camera.project(new Vector3(bubbleX, bubbleY, 0));
					chatBubbles.get(name).setCenterPosition(bubbleProjected.x, bubbleProjected.y);					
					//it.remove(); // avoids a ConcurrentModificationException
				}
			}

			camera.translate(player.getX() + player.getWidth()/2 - camera.position.x, player.getY() + player.getHeight()/2 - camera.position.y);
			camera.update();
			tiledMapRenderer.setView(camera);
			tiledMapRenderer.render();
			
			//Network update
			networkUpdateTimer += Gdx.graphics.getDeltaTime();
			//if(networkUpdateTimer > networkUpdateInterval){
			//networkUpdateTimer = 0;
			MoveTo moveTo = new MoveTo();
			moveTo.position = new Vector3(player.getX(), player.getY(), 0);
			moveTo.rotation = player.getHeading();
			moveTo.map = player.getMap();
			if(client != null){
				//System.out.println("move sent");
				client.sendTCP(moveTo);
			}
		}

		//Draw GUI
		stage.draw();

		//Debug
		debugTimer += Gdx.graphics.getDeltaTime();
		if(debugTimer > 5){
			//System.out.println("---------");
			debugTimer = 0;
			/*for(int i = 0;i < nameList.getItems().size; i++){
				System.out.println(nameList.getItems().get(i));
			}
			if(positions != null){
				for(int i = 0;i < positions.length; i++){
					System.out.println(positions[i]);
				}
			}*/
			/*Iterator<Entry<String, Player>> it = players.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry <String,Player> pairs = (Map.Entry<String,Player>)it.next();	
				System.out.println(pairs.getKey() + ": " + pairs.getValue().getX() + "," + pairs.getValue().getY());
				//it.remove(); // avoids a ConcurrentModificationException
			}
			*/
			//System.out.println("---------");
		}
		Gdx.graphics.setTitle("FPS: " + Gdx.graphics.getFramesPerSecond());
		//fpsLogger.log();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}


	@Override
	public boolean keyDown(int keycode) {

		if(keycode == Input.Keys.LEFT){
			left = true;
			forward = true;
			rotatePlayer(player, 180);
		}
		if(keycode == Input.Keys.RIGHT){
			right = true;
			forward = true;
			rotatePlayer(player, 0);
		}
		if(keycode == Input.Keys.UP){
			up = true;
			forward = true;
			rotatePlayer(player, 90);
		}
		if(keycode == Input.Keys.DOWN){
			down = true;
			forward = true;
			rotatePlayer(player, -90);
		}
		
		if(keycode == Input.Keys.A)
			strafeLeft = true;
		if(keycode == Input.Keys.D)
			strafeRight = true;
		if(keycode == Input.Keys.W)
			forward = true;
		if(keycode == Input.Keys.S)
			backward = true;

		if(keycode == Input.Keys.NUM_0){
			if(tiledMapRenderer.isGridVisible()){
				tiledMapRenderer.showGrid(false);
			}else{
				tiledMapRenderer.showGrid(true);
			}
		}
		if(keycode == Input.Keys.NUM_1) if(tiledMap.getLayers().getCount()>0)
			tiledMap.getLayers().get(0).setVisible(!tiledMap.getLayers().get(0).isVisible());
		if(keycode == Input.Keys.NUM_2) if(tiledMap.getLayers().getCount()>1)
			tiledMap.getLayers().get(1).setVisible(!tiledMap.getLayers().get(1).isVisible());
		if(keycode == Input.Keys.NUM_3) if(tiledMap.getLayers().getCount()>2)
			tiledMap.getLayers().get(2).setVisible(!tiledMap.getLayers().get(2).isVisible());
		if(keycode == Input.Keys.NUM_4) if(tiledMap.getLayers().getCount()>3)
			tiledMap.getLayers().get(3).setVisible(!tiledMap.getLayers().get(3).isVisible());
		
		if(keycode == Input.Keys.NUM_8){
			if(tiledMapRenderer.areSpecialObjectsVisible()){
				tiledMapRenderer.showSpecialObjects(false);
			}else{
				tiledMapRenderer.showSpecialObjects(true);
			}
		}
		
		if(keycode == Input.Keys.NUM_9){
			if(tiledMapRenderer.isSpriteBoundingBoxVisible()){
				tiledMapRenderer.showSpriteBoundingBox(false);
			}else{
				tiledMapRenderer.showSpriteBoundingBox(true);
			}
		}

		if(keycode == Input.Keys.ENTER){
			if(gameChat.isVisible()){
				//This should never happen unless gamechat is visible when game starts
				gameChat.setVisible(false);
			}else{
				gameChat.setVisible(true);
				stage.setKeyboardFocus(gameChat);
				Gdx.input.setInputProcessor(stage);
			}
		}

		return false;
	}


	@Override
	public boolean keyUp(int keycode) {

		if(keycode == Input.Keys.LEFT){
			left = false;
			forward = false;
		}

		if(keycode == Input.Keys.RIGHT){
			right = false;
			forward = false;
		}

		if(keycode == Input.Keys.UP){
			up = false;
			forward = false;
		}

		if(keycode == Input.Keys.DOWN){
			down = false;
			forward = false;
		}
		
		if(left || right || up || down){
			forward = true;
		}

		
		if(keycode == Input.Keys.A)
			strafeLeft = false;
		if(keycode == Input.Keys.D)
			strafeRight = false;
		if(keycode == Input.Keys.W)
			forward = false;
		if(keycode == Input.Keys.S)
			backward = false;

		return false;
	}


	@Override
	public boolean keyTyped(char character) {

		return false;
	}


	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		
		
		/*Vector3 clickCoordinates = new Vector3(screenX,screenY,0);
		Vector3 position = camera.unproject(clickCoordinates);
		player.setPosition(position.x - (position.x % tileWidth), position.y - (position.y % tileHeight));
		camera.translate(player.getX() - camera.position.x, player.getY() - camera.position.y);
		//camera.lookAt(sprite.getOriginX(), sprite.getOriginY(), 0);
		camera.update();

		System.out.println("Clicked screen at: x=" + screenX + " y=" + screenY);
		System.out.println("Moving to tile: x=" + player.getX()/tileWidth + " y=" + player.getY()/tileHeight);
		*/
		messageWindow.setVisible(false);
		
		
		Vector3 clickCoordinates = new Vector3(screenX,screenY,0);
		Vector3 position = camera.unproject(clickCoordinates);
		for(MapObject object : objectLayer.getObjects()){
			if(object instanceof RectangleMapObject){
				RectangleMapObject rectangleObject = (RectangleMapObject) object;
				Rectangle objectArea = rectangleObject.getRectangle();
				if(rectangleObject.getProperties().containsKey("gid")){
					//If object is tile object we need to create rectangle size from tilesize
					objectArea.setSize(tileWidth, tileHeight);
				}
				if(objectArea.contains(position.x, position.y)){
					if(rectangleObject.getProperties().containsKey("message")){
						String message = (String) rectangleObject.getProperties().get("message");
						System.out.println("Message: " + message);						
						//Fix broken newlines
						String fixedMessage = message.replace("\\n", "\n");
						System.out.println("Fixed message: " + fixedMessage);
						messageWindow.setText(fixedMessage);
						
						messageWindow.setVisible(true);
					}
				}
			}
		}

		return true;
	}


	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		Vector3 mouseCoordinates = new Vector3(screenX,screenY,0);
		Vector3 position = camera.unproject(mouseCoordinates);
		
		int x = (int) (position.x - (position.x % tileWidth));
		int y = (int) (position.y - (position.y % tileHeight));
		tiledMapRenderer.highlightTile(x, y);
		
		float centerX = player.getX() + (player.getWidth()/2);
		float centerY = player.getY() + (player.getHeight()/2);
		
		rotatePlayer(player, (MathUtils.atan2(position.y - centerY, position.x - centerX))*MathUtils.radiansToDegrees);
		//System.out.println(player.getHeading());
		
		return false;
	}


	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}
}