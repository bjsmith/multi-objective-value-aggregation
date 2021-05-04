package tools.memory;

public class TestMemory {
	public static void main(String [] args){
		int mb = 1024*1024;
		
		//Getting the runtime reference from the system
		Runtime runtime = Runtime.getRuntime();
		
		System.out.println("##### Heap utilisation statistics [MB] #####");
		
		//Print used memory
		System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);
		
		//Print free memory
		System.out.println("Free Memory:" + runtime.freeMemory() / mb);
		
		//Print total available memory
		System.out.println("Total Memory:" + runtime.totalMemory() / mb);
		
		//Print Maximum available memory
		System.out.println("Total Memory:" + runtime.maxMemory() / mb);
		
	}

}
