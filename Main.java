
public class Main {

	public static void main(String[] args) {
		
		SceneRenderer app=new SceneRenderer();
		
		try {
			app.run();
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}finally {
			app.cleanupBuffers();
		}

	}
	
}
