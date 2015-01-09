package com.juniper.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.juniper.game.GdxSandbox;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		if(arg.length > 1){
			cfg.width = Integer.parseInt(arg[0]);
			cfg.height = Integer.parseInt(arg[1]);
		}else{
	        cfg.width = 800;
	        cfg.height = 600;
		}
		if(arg.length > 2){
			new LwjglApplication(new GdxSandbox(arg[2]), cfg);
		}else{
			new LwjglApplication(new GdxSandbox(), cfg);
		}
	}
}
