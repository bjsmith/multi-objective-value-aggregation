import agents.*;
import env.*;
import experiments.*;
//copied from MORL_Glue_Driver.java

public class MORL_Glue_Driver_BJS
{

	public static void main(String[] args) 
	{
			Process server = null;
			// try to launch the MORL_Glue server
			Runtime rt = Runtime.getRuntime();
			try 
			{
//				server = rt.exec(new String[] {"wine","../morlglue-server/morlglue_x86.exe"}); // Linux, MacOS
				server = new ProcessBuilder(
						"/Applications/Wine Stable.app/Contents/Resources/wine/bin/wine",
						"../morlglue-server/morlglue_x64.exe"
						).start();
				//server = rt.exec("..\\morlglue-server\\morlglue_x86.exe"); // Windows
				System.out.println("Launching server on Ben Smith's computer");
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		   // launch agent in its own thread
			Thread agent = 
			new Thread(){
		          public void run(){
		            System.out.println("Started agent thread");
		            //MAIN AGENTS OF INTEREST
		            WSAgent.main(null);
		            //Agg_Agent.main(new String[] {"SFLLA"});
		            
		            
		            //OTHER STUFF
		            //WSteeringTabularNonEpisodic.main(null);
		            //QSteeringTabularNonEpisodic.main(null);
		            //WSteeringTabularEpisodic.main(null);
		            //QSteeringTabularEpisodic.main(null);
		            
		            //WSNoveltyAgent.main(null);
		            //QLearningAgentRichard.main(null);
		            //UserControlledAgent.main(null);
		           
		            //TLO_EOVF_Agent.main(null);
		            //SideEffectSingleObjectiveAgent.main(null);
		            //SideEffectLinearWeightedAgent.main(null);
		            //SafetyFirstMOAgent.main(null);
		            //agent.agent_message("start-debugging");
		            //SatisficingMOAgent.main(null);
		            //LearningRelativeReachabilityAgent.main(null);
		            //TLO_Agent_Conditioned_On_Actual_Rewards.main(null);
		            
		            //TLO_Agent_Conditioned_On_Expected_Rewards.main(null);
		            //TLO_Agent.main(null);
		          }
		        };
			agent.start();
	 	   // launch environment in its own thread
			Thread envt = new Thread(){
		          public void run(){
		            System.out.println("Started envt thread");
		            
		            //From Vamplew et al. (2021):
		            //BreakableBottlesSideEffectsV2.main(null);
		            //UnbreakableBottlesSideEffectsV2.main(null);
		            //SokobanSideEffects.main(null);
		            Doors.main(null);
		            
		            //OTHERS:
		            
		            //DeepSeaTreasureEnv.main(null);
		            //DeepSeaTreasureEnv_TimeFirst.main(null);
		            //DeepSeaTreasureMixed.main(null);
		            //String[] gdstArgs = {"15","4","1","3","0.0","0.0",""+GeneralisedDeepSeaTreasureEnv.CONCAVE,"471"};
		            //GeneralisedDeepSeaTreasureEnv.main(gdstArgs);
		            //LinkedRings.main(null);
		            //NonRecurrentRings.main(null);
		            //MOMountainCarDiscretised.main(null);
		            //ResourceGatheringEpisodic.main(null);
		            //BonusWorld.main(null);
		            //SpaceExploration.main(null);

		            //UnbreakableBottlesSideEffectsNoop.main(null);
		            //BreakableBottlesSideEffectsNoop.main(null);
		            //SokobanSideEffectsNoop.main(null);
		            //DoorsNoop.main(null);
		            //StochasticMOMDP.main(null);
		            //SpaceTraders.main(null);
		          }
		        };
		     envt.start();
	 	   // launch experiment in its own thread
			Thread experiment = new Thread(){
		          public void run(){
		            System.out.println("Started experiment thread");
		            
		            //default demo file
		            //DemoExperiment.main(null);
		            
		            //main experiments of interest, I think:
		            //TLOExplorationExperiment.main(null);
		            TLOConditionedExperiment.main(null);
		            
		            //others:
		            //DebuggingExperiment.main(null);
		            //ExplorationExperiment.main(null);
		            //HypervolumeExperiment.main(null);
		            //SteeringExperiment.main(null);
		            //SteeringExperimentWithTargetChange.main(null);
		            //SideEffectExperiment.main(null);
		            //SideEffectExperimentWithExcelOutput.main(null);
		            
		          }
		        };
		    experiment.start();
		    
		    for(int i=0;i<10000;i++)
		    {
		    	try {
					Thread.sleep(1000);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	System.out.println("testing...");
		    	System.out.println(experiment.isAlive());
		    	
		    	
		    	System.out.println(agent.isAlive());
		    	System.out.println(envt.isAlive());
		    	if(!experiment.isAlive())
		    	{
		    		agent.interrupt();
		    		envt.interrupt();
		    	}
		    }
		    
		    System.out.println("finished!");
		    
		    
		}
	
}
