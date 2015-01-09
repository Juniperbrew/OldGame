package com.juniper.network;


import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

// This class is a convenient place to keep things common to both the client and server.
public class Network {
        static public final int port = 54555;

        // This registers objects that are going to be sent over the network.
        static public void register (EndPoint endPoint) {
                Kryo kryo = endPoint.getKryo();
                kryo.register(Register.class);
                kryo.register(String[].class);
                //kryo.register(UpdateNames.class);
                kryo.register(ChatMessage.class);
                //kryo.register(UpdatePositions.class);
                kryo.register(MoveTo.class);
                kryo.register(Vector3.class);
                kryo.register(Vector3[].class);
                kryo.register(UpdateClients.class);
                kryo.register(float[].class);
        }

        static public class Register {
                public String name;
        		public Vector3 position;
        		public float rotation;
        		public String map;
        }

        static public class ChatMessage {
        		public String name;
                public String text;
        }
        
        static public class UpdateClients {
            public String[] names;
    		public Vector3[] positions;
    		public float[] rotations;
    		public String[] maps;
        }
        
        static public class MoveTo {
        		public Vector3 position;
        		public float rotation;
        		public String map;
        }
}