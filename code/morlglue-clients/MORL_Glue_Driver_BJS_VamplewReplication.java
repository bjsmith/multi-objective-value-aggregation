import agents.*;
import env.*;
import experiments.*;
//copied from MORL_Glue_Driver.java

public class MORL_Glue_Driver_BJS_VamplewReplication
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
		            
		            String agent_to_run = "TLO^{PA}";
		            
		            
		            
		            
		            //MAIN AGENTS OF INTEREST
		            //WSAgent.main(null);
		            
		            //AGENTS VAMPLEW SAYS ARE KEY FOR THE SAFETY PAPER
		            //SideEffectSingleObjectiveAgent.main(null);
		            //SafetyFirstMOAgent.main(null);
		            //SideEffectLinearWeightedAgent.main(null);
		            
		            //lex-PA 
		            //double primaryRewardThreshold = 1000; // use high value here to get lex-pa (for tlo-p or tlo-pa use the per envt thresholds below)
		            
		        	// For UnbreakableBottles
	            	//primaryRewardThreshold = -50; // sets threshold on the acceptable minimum level of performance on the primary reward
//	            	double minPrimaryReward = -1000; // the lowest reward obtainable
//	            	double maxPrimaryReward = 44;	// the highest reward obtainable
	            // For BreakableBottles
	            	double primaryRewardThreshold = -500; // sets threshold on the acceptable minimum level of performance on the primary reward
	            	double minPrimaryReward = -1000; // the lowest reward obtainable
	            	double maxPrimaryReward = 44;	// the highest reward obtainable   
	            // For Sokoban and Doors
	            	//double primaryRewardThreshold = -500; // sets threshold on the acceptable minimum level of performance on the primary reward
	            	//double minPrimaryReward = -1000; // the lowest reward obtainable
	            	//double maxPrimaryReward = 50;	// the highest reward obtainable

		            
		            if (agent_to_run=="TLO^P") {
			            //TLO-P
			            double safetyThreshold = 1000; //-0.1; //use high value if you want to 'switch off' thresholding (ie to get TLO-P rather than TLO-PA)
			            SatisficingMOAgent.main(primaryRewardThreshold,safetyThreshold,minPrimaryReward, maxPrimaryReward,null); //TLO-P
		            }else if (agent_to_run=="TLO^A"){
		            	//run a SafetyFirstMOAgent
		            	SafetyFirstMOAgent.main(null);
		            }else if(agent_to_run=="TLO^{PA}") {
			            //TLO-PA
			        	double safetyThreshold = -0.1; //use high value if you want to 'switch off' thresholding (ie to get TLO-P rather than TLO-PA)
			        	SatisficingMOAgent.main(primaryRewardThreshold,safetyThreshold,minPrimaryReward, maxPrimaryReward,null); //TLO-P
		            }else if (agent_to_run=="lex^P") {
		            	throw new RuntimeException("not implemented");
		            }else if (agent_to_run=="lex^A") {
		            	throw new RuntimeException("not implemented");
		            }
		            
		            
		            //LearningRelativeReachabilityAgent.main(null);
		            
		            
		            
		            //OTHERS THAT MAY BE OF INTEREST
		            //TLO_Agent.main(null);
		            
		            //agent.agent_message("start-debugging");

		            
		          }
		        };
		        
			agent.start();
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	 	   // launch environment in its own thread
			Thread envt = new Thread(){
		          public void run(){
		            System.out.println("Started envt thread");
		            
		            //From Vamplew et al. (2021):
		            BreakableBottlesSideEffectsV2.main(null);
		            //UnbreakableBottlesSideEffectsV2.main(null);
		            //SokobanSideEffects.main(null);
		            //Doors.main(null);
		            
		            //MAIN DEFAULT TEST ENV:
//		            String[] gdstArgs = {"15","4","1","3","0.0","0.0",""+GeneralisedDeepSeaTreasureEnv.CONCAVE,"471"};
//		            GeneralisedDeepSeaTreasureEnv.main(gdstArgs);
//		            //OTHERS:
		            
		            //DeepSeaTreasureEnv.main(null);
		            //DeepSeaTreasureEnv_TimeFirst.main(null);
		            //DeepSeaTreasureMixed.main(null);
		          }
		        };
		     envt.start();
	 	   // launch experiment in its own thread
			Thread experiment = new Thread(){
		          public void run(){
		            System.out.println("Started experiment thread");
		            //EXPERIMENT PETER SAYS IS RELEVANT
		            SideEffectExperimentWithExcelOutput.main(null);
		            
		            //default demo file
		            //DemoExperiment.main(null);
		            

		            
		          }
		        };
		    experiment.start();
		    
//		    for(int i=0;i<10000;i++)
//		    {
//		    	try {
//					Thread.sleep(1000);
//					
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//		    	System.out.println("running...");
//
//		    	if(!experiment.isAlive())
//		    	{
//		    		agent.interrupt();
//		    		envt.interrupt();
//		    		break;
//		    	}
//		    }
		    
		    System.out.println("finished!");
		    
		    
		}
	
}
