import agents.*;
import env.*;
import experiments.*;


public class MORL_Glue_Driver
{

	public static void main(String[] args) 
	{
			Process server = null;
			// try to launch the MORL_Glue server
			Runtime rt = Runtime.getRuntime();
			try 
			{
				server = rt.exec(new String[] {"wine","../morlglue-server/morlglue_x86.exe"}); // Linux, MacOS
				//server = rt.exec("..\\morlglue-server\\morlglue_x86.exe"); // Windows
				System.out.println("Launching server");
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		   // launch agent in its own thread
			Thread agent = 
			new Thread(){
		          public void run(){
		            System.out.println("Started agent thread");
		            
		            //TLO_Agent.main(null);
		            Agg_Agent.main(null);
		            //UserControlledAgent.main(null);
		          }
		        };
			agent.start();

	 	   // launch environment in its own thread
			Thread envt = new Thread(){
		          public void run(){
		            System.out.println("Started envt thread");
		            
		            String[] gdstArgs = {"15","4","1","3","0.0","0.0",""+GeneralisedDeepSeaTreasureEnv.CONCAVE,"471"};
		            GeneralisedDeepSeaTreasureEnv.main(gdstArgs);
		            //BonusWorld.main(null); // -> error
		            //DeepSeaTreasureEnv.main(null); // -> error
		            //DeepSeaTreasureMixed.main(null); // -> error
		            //LinkedRings.main(null); // -> error
		            //MOMountainCarDiscretised.main(null); // -> error
		            //SpaceExploration.main(null); // -> error
		            //NonRecurrentRings.main(null); // -> unsupported error
		            //ResourceGatheringEpisodic.main(null); // -> out of bounds error
		          }
		        };
		  envt.start();

	 	   // launch experiment in its own thread
			Thread experiment = new Thread(){
		          public void run(){
		            System.out.println("Started experiment thread");
		            
		            DemoExperiment.main(null);
		            //SkeletonExperiment.main(null);
		          }
		        };
		  experiment.start();
		}
	
}
