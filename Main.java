
//import static org.lwjgl.assimp.Assimp.*;

public class Main {

	public static void main(String[] args) {
		
		/*
			System.out.println(aiGetVersionMajor()+" "+aiGetVersionMinor()+" "+"Git-Revision: "+Integer.toHexString(aiGetVersionRevision()));
			System.exit(-1);
		*/
		
		SceneRenderer app=new SceneRenderer();
		
		try {
			app.run();
		}catch(Exception e) {//Make more explicit
			e.printStackTrace();
			System.exit(-1);
		}finally {
			app.cleanupBuffers();
		}

	}
	
}
