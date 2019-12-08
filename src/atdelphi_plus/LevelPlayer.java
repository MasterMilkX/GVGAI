package atdelphi_plus;

import java.io.IOException;
import java.util.Random;

import tools.Utils;
import tracks.ArcadeMachine;

public class LevelPlayer {
	//linear program to test the generation, mutation, and creation of MAPElites

		//location of games
		static String gamesPath = "examples/gridphysics/";
		static String physicsGamesPath = "examples/contphysics/";
		static String generateLevelPath = "src/atdelphi_plus/";
		

		//all public games (from LevelGenerator.java)
		
		
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

		//Game settings
		static int seed = new Random().nextInt();

		// Game and level to play
		static int gameIdx = 90;							//index of the game to use	[ZELDA]
		static String gameName = games[gameIdx];
		static String gameLoc = gamesPath + games[gameIdx] + ".txt";
		static String myLevel = "src/atdelphi_plus/generatedLevels/customPlayLevel.txt";			//where to get the level
		
		
		public static void main(String[] args) throws IOException {
			ArcadeMachine.playOneGame(gameLoc, myLevel, null, seed);
		}
		
}
