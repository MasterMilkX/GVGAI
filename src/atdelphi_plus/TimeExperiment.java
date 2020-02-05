package atdelphi_plus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class TimeExperiment {

	//linear program to test the generation, mutation, and creation of MAPElites

	//location of games
	static String gamesPath = "examples/gridphysics/";
	static String physicsGamesPath = "examples/contphysics/";
	static String generateLevelPath = "src/atdelphi_plus/";
	

	//all public games (from LevelGenerator.java)
	static String games[] = new String[] { "aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", // 0-4
			"beltmanager", "blacksmoke", "boloadventures", "bomber", "bomberman", // 5-9
			"boulderchase", "boulderdash", "brainman", "butterflies", "cakybaky", // 10-14
			"camelRace", "catapults", "chainreaction", "chase", "chipschallenge", // 15-19
			"clusters", "colourescape", "chopper", "cookmepasta", "cops", // 20-24
			"crossfire", "defem", "defender", "digdug", "dungeon", // 25-29
			"eighthpassenger", "eggomania", "enemycitadel", "escape", "factorymanager", // 30-34
			"firecaster", "fireman", "firestorms", "freeway", "frogs", // 35-39
			"garbagecollector", "gymkhana", "hungrybirds", "iceandfire", "ikaruga", // 40-44
			"infection", "intersection", "islands", "jaws", "killBillVol1", // 45-49
			"labyrinth", "labyrinthdual", "lasers", "lasers2", "lemmings", // 50-54
			"missilecommand", "modality", "overload", "pacman", "painter", // 55-59
			"pokemon", "plants", "plaqueattack", "portals", "raceBet", // 60-64
			"raceBet2", "realportals", "realsokoban", "rivers", "roadfighter", // 65-69
			"roguelike", "run", "seaquest", "sheriff", "shipwreck", // 70-74
			"sokoban", "solarfox", "superman", "surround", "survivezombies", // 75-79
			"tercio", "thecitadel", "thesnowman", "waitforbreakfast", "watergame", // 80-84
			"waves", "whackamole", "wildgunman", "witnessprotection", "wrapsokoban", // 85-89
			"zelda", "zenpuzzle"}; //90, 91
	
	
	//the tutorial interactions to look for (this will be set in CMEMapElites when it is created)
	//in the form: key = [interaction, sprite2, sprite1]; value = index
	static ArrayList<String[]> tutInteractionDict = new ArrayList<String[]>();
	
	
	// Other settings - these will become parameters in a seperate file
	static Random seed = new Random();			//randomization seed to start from
	static int gameIdx = 90;							//index of the game to use	[ZELDA]
	static String gameName = games[gameIdx];
	//String recordLevelFile = generateLevelPath + games[gameIdx] + "_glvl.txt";
	static String gameLoc = gamesPath + games[gameIdx] + ".txt";
	
	static String aiRunner = "agents.adrienctx.Agent";
	
	static int popNum = 100;
	static int expNum = 10;
	
	
	public static void main(String[] args) throws IOException{
		/*
		 * EXPERIMENT:
		 * TIME TO RANDOMLY INITIALIZE AND GENERATE CHROMOSOMES
		 *
		
		System.out.println("###  EXPERIMENT: TIME TO RANDOMLY INITIALIZE CHROMOSOMES  ###");
		System.out.println("###     Population: " + popNum + " | Trials: " + expNum + "     ###");
		*/
		
		//initialize mapelites
		MapElites map = new MapElites(gameName, gameLoc, seed, 0.5, "src/atdelphi_plus/generatedLevels/", "src/atdelphi_plus/", 70, 1.0/10.0);
				
		double sumTime = 0;
		
		/*
		for(int i=0;i<expNum;i++) {
			//System.out.println("Exp: " + i);
			//initialize the popNum # of random chromosomes
			long miniTime1 = System.nanoTime();
			map.randomChromosomes(popNum, null);
			long miniTime2 = System.nanoTime();
			sumTime += ((miniTime2-miniTime1)/1000000.0);
		}
		sumTime /= expNum;
		
		System.out.println("\n\nAverage time to randomly generate " + popNum + " chromosomes: " + sumTime +" ms\n\n");
		*/
		
		/*
		 * EXPERIMENT:
		 * TIME TO RUN Chromosomes
		 */
		
		System.out.println("###  EXPERIMENT: TIME TO RUN CHROMOSOME LEVELS  ###");
		System.out.println("###     Population: " + popNum + " | Trials: " + expNum + "     ###");
		
		sumTime = 0;
		
		for(int i=0;i<expNum;i++) {
			//System.out.println("Exp: " + i);
			//initialize the popNum # of random chromosomes
			Chromosome[] c = map.randomChromosomes(popNum, null);
			long miniTime1 = System.nanoTime();
			for(int j=0;j<popNum;j++) {
				c[j].calculateResults("agents.adrienctx.Agent", null, 0,0.0,10);
			}
			long miniTime2 = System.nanoTime();
			sumTime += ((miniTime2-miniTime1)/1000000.0);
		}
		sumTime /= expNum;
		
		System.out.println("\n\nAverage time to randomly generate " + popNum + " chromosomes: " + sumTime +" ms\n\n");
		
		//System.out.println("\nRandomly initializing " + popNum + " chromosomes took: " + ((miniTime2 - miniTime1)/1000000.0) + " ms\n"); 
				
	}
}
