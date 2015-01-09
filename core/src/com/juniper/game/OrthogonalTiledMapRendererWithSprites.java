package com.juniper.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

//Subclass to enable drawing of sprites and objects
public class OrthogonalTiledMapRendererWithSprites extends OrthogonalTiledMapRenderer {
    private Player player;
    private List<Player> players;
    private int drawSpritesAfterLayer = 2;
    private ShapeRenderer shapeRenderer;
    private Rectangle highlightTile = new Rectangle();
    private boolean showGrid = false;
    private boolean showSpriteBoundingBox = false;
    private boolean showSpecialObjects = true;

    public OrthogonalTiledMapRendererWithSprites(TiledMap map) {
        super(map);
        players = new ArrayList<Player>();
        shapeRenderer = new ShapeRenderer();
        Color color = Color.YELLOW;
		shapeRenderer.setColor(color);
    }

    public void addSprite(Player sprite){
    	players.add(sprite);
    }
    
    public void removeSprite(Player sprite){
    	players.remove(sprite);
    }
    
    public void showGrid(boolean flag){
    	showGrid = flag;
    }
    public boolean isGridVisible(){
    	return showGrid;
    }
    public void showSpriteBoundingBox(boolean flag){
    	showSpriteBoundingBox = flag;
    }
    public boolean isSpriteBoundingBoxVisible(){
    	return showSpriteBoundingBox;
    }
	@Override
	public void setView (OrthographicCamera camera) {
		super.setView(camera);
		shapeRenderer.setProjectionMatrix(camera.combined);
	}
    
    @Override
    public void renderObject (MapObject object){
    	if(object instanceof RectangleMapObject){
    		Rectangle rect = ((RectangleMapObject) object).getRectangle();
    		shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
    	}
    }
    
    public void renderGrid(){
    	TiledMapTileLayer firstLayer = ((TiledMapTileLayer)getMap().getLayers().get(0));
    	int layerWidth = firstLayer.getWidth();
    	int layerHeight = firstLayer.getHeight();
    	float tileWidth =  firstLayer.getTileWidth();
    	float tileHeight =  firstLayer.getTileHeight();
    	
    	for(float x = 0; x < layerWidth*tileWidth; x += tileWidth){
    		shapeRenderer.line(x, layerHeight*tileHeight, x, 0);
    	}
		for(float y = 0; y < layerHeight*tileHeight; y += tileHeight){
    		shapeRenderer.line(0, y, layerWidth*tileWidth, y);
		}
    }
    
    public void highlightTile(int x, int y){
        float tileWidth = ((TiledMapTileLayer)getMap().getLayers().get(0)).getTileWidth();
        float tileHeight = ((TiledMapTileLayer)getMap().getLayers().get(0)).getTileHeight();
    	highlightTile.x = x;
    	highlightTile.y = y;
    	highlightTile.width = tileWidth;
    	highlightTile.height = tileHeight;
    }

    @Override
    public void render() {
        beginRender();
        shapeRenderer.begin(ShapeType.Filled);
        
        int currentLayer = 0;
        for (MapLayer layer : map.getLayers()) {
            if (layer.isVisible()) {
                if (layer instanceof TiledMapTileLayer) {
                    renderTileLayer((TiledMapTileLayer)layer);
                    currentLayer++;
                    if(currentLayer == drawSpritesAfterLayer){
                        for(Player player : players){
                        	player.updateKeyFrame();
                            player.draw(this.getSpriteBatch());
                            if(showSpriteBoundingBox){
                            	shapeRenderer.rect(player.getX(), player.getY(), player.getWidth(), player.getHeight());
                            }
                        }
                    }
                } else {
                    for (MapObject object : layer.getObjects()) {
                    	
                    	if(showSpecialObjects){
                            renderObject(object);
                    	}
                    	//Look for tile objects and draw them
                    	if(object instanceof RectangleMapObject){
                    		RectangleMapObject rectObj = (RectangleMapObject) object;
                        	if(rectObj.getProperties().containsKey("gid")) {
                        		TiledMapTile tile = getMap().getTileSets().getTile(rectObj.getProperties().get("gid", Integer.class));
                        		getSpriteBatch().draw(tile.getTextureRegion(), rectObj.getRectangle().getX(), rectObj.getRectangle().getY());
                        	}
                    	}
                    }
                }
            }
        }
        endRender();
        shapeRenderer.end();
        shapeRenderer.begin(ShapeType.Line);
        //Draw highlighted tile
        shapeRenderer.rect(highlightTile.x, highlightTile.y, highlightTile.width, highlightTile.height);
        //Draw grid
        if(showGrid){
        	renderGrid();
        }
        shapeRenderer.end();

    }
    
    public void showSpecialObjects(boolean flag){
    	showSpecialObjects = flag;
    }
    
    public boolean areSpecialObjectsVisible(){
    	return showSpecialObjects;
    }
    
}