
package com.juniper.network;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.juniper.network.Network.ChatMessage;
import com.juniper.network.Network.MoveTo;
import com.juniper.network.Network.Register;
import com.juniper.network.Network.UpdateClients;
import com.esotericsoftware.minlog.Log;

public class ChatServer {
	Server server;
	JTextArea log;
	ArrayList<String> names;
	JLabel connectionCount;

	public ChatServer () throws IOException {
		server = new Server() {
			protected Connection newConnection () {
				// By providing our own connection implementation, we can store per
				// connection state without a connection ID to state look up.
				return new ChatConnection();
			}
		};       

		// For consistency, the classes to be sent over the network are
		// registered by the same method for both the client and server.
		Network.register(server);


		Runnable updateThread = new Runnable(){

			@Override
			public void run() {
				while(true){
					updateClients();
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		};
		new Thread(updateThread).start();

		server.addListener(new Listener() {
			public void received (final Connection c, Object object) {
				// We know all connections for this server are actually ChatConnections.
				ChatConnection connection = (ChatConnection)c;

				if (object instanceof Register) {
					System.out.println("Connected clients:" + server.getConnections().length);
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							connectionCount.setText("Connections: " + server.getConnections().length);
						}
					});
					// Ignore the object if a client has already registered a name. This is
					// impossible with our client, but a hacker could send messages at any time.
					if (connection.name != null) return;
					// Ignore the object if the name is invalid.
					Register registerPlayer = (Register) object;
					String name = registerPlayer.name;
					if (name == null) return;
					name = name.trim();
					if (name.length() == 0) return;

					if (names != null && names.contains(name)){
						//If name exists try rename 5 times
						for(int i = 1; i < 6; i++){
							if(!names.contains(name + i)){
								name = name + i;
								break;
							}else if(i == 5){
								return;
							}
						}
					}
					// Store the name on the connection.
					connection.name = name;
					// Give the connection a position
					connection.position = registerPlayer.position;
					connection.map = registerPlayer.map;
					connection.rotation = registerPlayer.rotation;
					// Send a "connected" message to everyone except the new client.
					ChatMessage chatMessage = new ChatMessage();
					chatMessage.text = name + " connected.";
					server.sendToAllExceptTCP(connection.getID(), chatMessage);

					// Send everyone a new list of connection names and positions
					updateClients();                                       

					log(name + " connected from " + c.getRemoteAddressTCP() + "\n");
					return;
				}

				if (object instanceof ChatMessage) {
					// Ignore the object if a client tries to chat before registering a name.
					if (connection.name == null) return;
					ChatMessage chatMessage = (ChatMessage)object;
					// Ignore the object if the chat message is invalid.
					String message = chatMessage.text;
					if (message == null) return;
					message = message.trim();
					if (message.length() == 0) return;
					// Prepend the connection's name and send to everyone.
					chatMessage.text = message;
					chatMessage.name = connection.name;
					server.sendToAllTCP(chatMessage);
					return;
				}
				if (object instanceof MoveTo) {
					MoveTo moveTo = (MoveTo) object;
					connection.position = moveTo.position;
					connection.rotation = moveTo.rotation;
					connection.map = moveTo.map;
					//log(connection.name + " position: " + connection.position.x + "," + connection.position.y + "\n");
					//Send everyone a list of the positions
					//updateClients();
					return;
				}
			}

			public void disconnected (Connection c) {
				ChatConnection connection = (ChatConnection)c;
				if (connection.name != null) {
					// Announce to everyone that someone (with a registered name) has left.
					ChatMessage chatMessage = new ChatMessage();
					chatMessage.text = connection.name + " disconnected.";
					server.sendToAllTCP(chatMessage);
					updateClients();

					log(connection.name + " disconnected from " + c.getRemoteAddressTCP() + "\n");
				}
				System.out.println("Connected clients:" + server.getConnections().length);
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						connectionCount.setText("Connections: " + server.getConnections().length);
					}
				});
			}
		});
		server.bind(Network.port);
		server.start();

		// Open a window to provide an easy way to stop the server.
		JFrame frame = new JFrame("Chat Server");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed (WindowEvent evt) {
				server.stop();
				System.out.println("Server stopped");
				System.exit(0);
			}
		});
		String externalIp;
		try {
			externalIp = IpChecker.getIp();
		} catch (Exception e1) {
			externalIp = "Error finding external IP";
			e1.printStackTrace();
		}
		System.out.println(externalIp);

		log = new JTextArea();

		frame.getContentPane().setLayout(new BorderLayout());
		JPanel top = new JPanel(new BorderLayout());
		connectionCount = new JLabel("Connections: " + server.getConnections().length);
		top.add(connectionCount , BorderLayout.LINE_START);
		top.add(new JLabel(externalIp), BorderLayout.LINE_END);
		frame.getContentPane().add(top, BorderLayout.PAGE_START);
		frame.getContentPane().add(log, BorderLayout.CENTER);
		frame.setSize(320, 200);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void log(final String text){
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				log.append(text);
			}
		});
	}

	void updateClients(){
		// Collect the names for each connection.
		Connection[] connections = server.getConnections();
		names = new ArrayList<String>(connections.length);
		ArrayList <Vector3> positions = new ArrayList<Vector3>(connections.length);
		float[] rotations = new float[connections.length];
		ArrayList <String> maps = new ArrayList<String>(connections.length);
		for (int i = 0; i < connections.length; i++) {
			ChatConnection connection = (ChatConnection)connections[i];
			names.add(connection.name);
			positions.add(connection.position);
			rotations[i] = connection.rotation;
			maps.add(connection.map);
		}
		UpdateClients updateClients = new UpdateClients();
		updateClients.names = names.toArray(new String[names.size()]);
		updateClients.positions = positions.toArray(new Vector3[positions.size()]);
		updateClients.rotations = rotations;
		updateClients.maps = maps.toArray(new String[maps.size()]);
		server.sendToAllTCP(updateClients);
	}

	// This holds per connection state.
	static class ChatConnection extends Connection {
		public String name;
		public Vector3 position;
		public float rotation;
		public String map;
	}

	public static void main (String[] args) throws IOException {
		Log.set(Log.LEVEL_TRACE);
		new ChatServer();

	}
}
