import agents.*;
import env.*;
import experiments.*;


public class MORL_Glue_Driver2
{

	public static void main(String[] args) 
	{
			Process server = null;
			// try to launch the MORL_Glue server
			Runtime rt = Runtime.getRuntime();
			try 
			{
				server = rt.exec("MO586rl_glue.exe");//local path
				System.out.println("Launching server");
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
		            //WSteeringTabularNonEpisodic.main(null);
		            //QSteeringTabularNonEpisodic.main(null);
		            //WSteeringTabularEpisodic.main(null);
		            //QSteeringTabularEpisodic.main(null);
		            //WSAgent.main(null);
		            //WSNoveltyAgent.main(null);
		            //QLearningAgentRichard.main(null);
		            //UserControlledAgent.main(null);
		            //TLO_Agent.main(null);
		            //TLO_EOVF_Agent.main(null);
		            //SideEffectSingleObjectiveAgent.main(null);
		            //SideEffectLinearWeightedAgent.main(null);
		            //SafetyFirstMOAgent.main(null);
		            //SatisficingMOAgent.main(null);
		            //LearningRelativeReachabilityAgent.main(null);
		            //TLO_Agent_Conditioned_On_Actual_Rewards.main(null);
		            //TLO_Agent_Conditioned_On_Expected_Rewards.main(null);
		          }
		        };
			agent.start();
	 	   // launch environment in its own thread
			Thread envt = new Thread(){
		          public void run(){
		            System.out.println("Started envt thread");
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
		            //BreakableBottlesSideEffectsV2.main(null);
		            //UnbreakableBottlesSideEffectsV2.main(null);
		            //SokobanSideEffects.main(null);
		            //Doors.main(null);
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
		            //DebuggingExperiment.main(null);
		            //DemoExperiment.main(null);
		            //ExplorationExperiment.main(null);
		            //HypervolumeExperiment.main(null);
		            //SteeringExperiment.main(null);
		            //SteeringExperimentWithTargetChange.main(null);
		            //TLOExplorationExperiment.main(null);
		            //SideEffectExperiment.main(null);
		            //SideEffectExperimentWithExcelOutput.main(null);
		            //TLOConditionedExperiment.main(null);
		          }
		        };
		    experiment.start();
		}
	
}
