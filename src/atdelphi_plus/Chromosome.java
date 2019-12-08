/*
 * Program by Megan "Milk" Charity 
 * GVGAI-compatible version of Chromosome.java from MarioAIExperiment 
 * Creates a chromosome for use with MapElites
 */

package atdelphi_plus;

import java.io.BufferedReader; 
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tools.IO;
import tracks.ArcadeMachine;
import tracks.levelGeneration.LevelGenMachine;

public class Chromosome implements Comparable<Chromosome>{

	/********************
	 * STATIC VARIABLES *
	 ********************/

	//custom level generator directly based off randomLevelGenerator.java 
	//  differences is the constant size of the level
	static private String chromoLevelGenerator = "atdelphi_plus.ChromosomeLevelGenerator";

	//open the json file that the run just exported the interactions to
	static private String outputInteractionJSON = "src/atdelphi_plus/generatedLevels/interactions_%.json";

	//the default level generator saves the text output to a file - so just write it and read it back in
	static private String placeholderLoc = "src/atdelphi_plus/generatedLevels/placeholder.txt";

	//taken directly from Chromosome.java [MarioAI]
	static protected Random _rnd;
	//protected int _appendingSize;		//size is dependent on the game itself

	//extended variables
	static protected String _gameName;
	static protected String _gamePath;
	static protected String[] _allChar;
	static protected ArrayList<String[]> _rules;
	static protected int idealTime;
	static protected double compareThreshold;

	/********************
	 * OBJECT VARIABLES *
	 ********************/


	//taken directly from Chromosome.java [MarioAI]		
	protected double _constraints;	
	protected double _fitness;
	protected int[] _dimensions;		//binary vector for the interactions that occured for this chromosome
	private int _age;					//age of the current chromosome

	//extended variables
	protected String _textLevel;
	protected boolean _hasBorder;


	//sets the static variables for the Chromsome class - shared between all chromosomes
	public static void SetStaticVar(Random seed, String gn, String gp, String genFolder, ArrayList<String[]> r, int it, double ct) {
		Chromosome._rnd = seed;
		Chromosome._gameName = gn;
		Chromosome._gamePath = gp;
		Chromosome._rules = r;
		Chromosome._allChar = getMapChar();
		Chromosome.outputInteractionJSON = genFolder + "interactions_%.json";
		Chromosome.idealTime = it;
		Chromosome.compareThreshold = ct;

	}


	//constructor for random initialization
	public Chromosome() {
		this._constraints = 0;
		this._fitness = 0;
		this._dimensions = null;
		this._age = 0;

		this._textLevel = "";
		this._hasBorder = false;
	}

	//constructor for cloning and mutation
	public Chromosome(String level, boolean hasborder) {
		this._constraints = 0;
		this._fitness = 0;
		this._dimensions = null;
		this._age = 0;

		this._textLevel = level;
		this._hasBorder = hasborder;
	}


	//random level initialization function using LevelGenMachine.java and ChromosomeLevelGenerator (AtDelphi+ exclusive class)
	public void randomInit(String placeholder) {
		this._textLevel = "";

		//default to the class variable
		if(placeholder == null) {
			placeholder = Chromosome.placeholderLoc;
		}

		try {
			//run the default level gen and write to the placeholder file
			LevelGenMachine.generateOneLevel(Chromosome._gamePath, chromoLevelGenerator, placeholder);
			this._textLevel = parseLevel(fullLevel(placeholder));
			this._hasBorder = level_has_border();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//stripped from LevelGenMachine's loadGeneratedFile() method
	private String parseLevel(String fullLevel) {
		String level = "";

		String[] lines = fullLevel.split("\n");
		int mode = 0;
		for(String line: lines) {
			if(line.equals("LevelDescription")) {
				mode = 1;
			}else if(mode == 1){
				level += (line + "\n");
			}
		}

		return level;
	}

	//returns a list of characters used for the map (map character key)
	private static String[] getMapChar() {
		String[] lines = new IO().readFile(Chromosome._gamePath);

		String charList = "";
		int mode = 0;
		for(String line: lines) {
			line = line.trim();
			if(line.equals("LevelMapping")) {
				mode = 1;
				continue;
			}else if(line.contentEquals("InteractionSet")) {
				mode = 0;
				continue;
			}
			else if(mode == 1 && line.length() > 0) {
				String l = line.trim();
				charList += l.charAt(0);
			}
		}
		charList += " ";
		return charList.split("");
	}

	/* 
	 * check if the level has a border
	 */
	private boolean level_has_border() {
		String[] lines = _textLevel.split("\n");

		//if there is a border, the first character in the level should be the start of the border
		String bordChar = Character.toString(lines[0].charAt(0));		

		//check first and last line for border character (ceiling and floor)
		if(lines[0].replace(bordChar, "").length() > 0)		//if there are any characters leftover, it is not a border
			return false;

		//check the first and last character of each line (walls)
		for(int i=1;i<lines.length-1;i++) {
			String line = lines[i];
			if(line.charAt(0) != bordChar.charAt(0) || line.charAt(line.length()-1) != bordChar.charAt(0))
				return false;
		}

		//passed - has a border
		return true;
	}




	//chromosome level runner
	public void calculateResults(String aiAgent, String outFile, int id, double entropyProb, int nothingCt) throws IOException {
		if(outFile == null)
			outFile = Chromosome.placeholderLoc;
		copyLevelToFile(outFile);
		System.out.println("Playing game...");
		String col_json = Chromosome.outputInteractionJSON.replaceFirst("%", ("_col_"+id));
		String play_json = Chromosome.outputInteractionJSON.replaceFirst("%", ("_key_"+id));

		long time1 = System.nanoTime();
		double[] results = ArcadeMachine.runOneGame(Chromosome._gamePath, outFile, false, aiAgent, null, Chromosome._rnd.nextInt(), 0, new String[]{col_json, play_json});
		long time2 = System.nanoTime();
		
		System.out.println(" -- Game time elapsed: " + ((time2-time1)/1000000.0) + " ms -- ");
		
		/*
		for(double d : results) {
			System.out.println(d);
		}
		 */

		this._age++;	 				//increment the age (the chromosome is older now that it has been run)
		
		//SET THE CONSTRAINTS
		double rawConstraints = getRunConstraints(results); 		//get the constraints (win or lose) / idealTime
		double killConstraints = try_nothing(nothingCt, outFile);	//get the proportion of times killed doing nothing
			System.out.println("Raw = " + rawConstraints + " | Kill = " + killConstraints);
		this._constraints = rawConstraints*killConstraints;			//set the constraints to the run constraints * do_nothing constraints
		
		//SET THE FITNESS
		double entropy = calculateFitnessEntropy();
		double der_entropy = calculateFitnessDerivativeEntropy();
			System.out.println("Entropy = " + entropy + " | Derivative Entropy = " + der_entropy);
		this._fitness = (entropy*(1.0-entropyProb) + (der_entropy*entropyProb));		//set the fitness (tile entropy)
		
		//SET THE DIMENSIONS
		calculateDimensions(id);						//set the dimensions (from AtDelFi rule set)

	}


	/*
	 * sets the constraints of the chromosome from the results of a run
	 * sh-boom
	 */
	private double getRunConstraints(double[] results) {
		//just uses the win condition
		//this._constraints = results[0];		

		//constraints = (win / timeToWin) + ((1-win) * 0.25 / timeToSurvive)
		//this._constraints = (results[0] / results[2]) + (((1-results[0]) * 0.25) / results[2]);

		//constraints = (win / (timeToWin dist from ideal time)) + ((1-win) * 0.25 / (timeToSurvive dist from ideal time))
		int idealTime = Chromosome.idealTime;
		return (results[0] / (Math.abs(idealTime - results[2])+1)) + (((1-results[0]) * 0.25) / (Math.abs(idealTime - results[2])+1));
	}

	//gets the entropy of unique tiles for the level to be used to calculate fitness
	private double calculateFitnessEntropy() {
		char[] charLevel = this._textLevel.toCharArray();
		int[] charCt = new int[Chromosome._allChar.length];

		//make a new char set from allChar[] because Java is a problematic whiny little shit
		char[] achar = new char[Chromosome._allChar.length];
		for(int c=0;c<Chromosome._allChar.length;c++) {
			achar[c] = Chromosome._allChar[c].charAt(0);
		}

		//initialize charCt to 0
		for(int i=0;i<charCt.length;i++) {
			charCt[i] = 0;
		}

		//count for each tile
		int allCt = 0;
		for(int a=0;a<charLevel.length;a++) {
			int index = indexOf(achar, charLevel[a]);
			if(index >= 0) {
				charCt[index]++;
				allCt++;
			}

		}	
		
		//number of characters
		//System.out.println("allCt: " + allCt);

		//calculate the probabilities for the entropy
		double[] probs = new double[achar.length];
		for(int b=0;b<achar.length;b++) {
			probs[b] = (double)((double)charCt[b] / (double)allCt);
			//System.out.println("CHAR: " + achar[b]);
			//System.out.println("ct: " + charCt[b]);
			//System.out.println("probability: " + probs[b]);
			//System.out.println("");
		}

		//calculate entropy (-sum(plog2(p)))
		double entropy = 0;
		for(int b=0;b<probs.length;b++) {
			if(probs[b] > 0) {
				//System.out.println("e: "+(-1*(probs[b] * Math.log10(probs[b]))));
				entropy += (-1*(probs[b] * Math.log10(probs[b])));
			}
				
		}

		return 1.0 - entropy;
	}
	
	
	private double calculateFitnessDerivativeEntropy() {
		//get row+column number 
		char[] charLevel = this._textLevel.toCharArray();
		String[] lineLevel = this._textLevel.split("\n");
		int rowNum = lineLevel.length;
		int colNum = lineLevel[0].length()+1;
		
		//setup 2d representation
		char[][] charLevel2d;
		//bordered level
		if(this._hasBorder) {			
			charLevel2d = new char[rowNum-2][colNum-3];		//inside of the tiles
			int index=0;
			for(int i=0;i<charLevel.length;i++) {
				char c = charLevel[i];
				if(c == '\n')						//ignore new line
					continue;
				if(i<(colNum))						//ignore top row
					continue;
				if(i>=(charLevel.length-(colNum)))	//ignore bottom row
					continue;
				if(i%colNum==0)						//ignore left wall
					continue;			
				if((i+2)%colNum==0)					//ignore right wall
					continue;
				
				//add character to the matrix
				charLevel2d[index/(colNum-3)][index%(colNum-3)] = c;
				index++;
				
			}
		}
		//unbordered level
		else {
			charLevel2d = new char[rowNum][colNum-1];		//all tiles
			int index=0;
			for(int i=0;i<charLevel.length;i++) {
				char c = charLevel[i];
				if(c == '\n')						//ignore new line
					continue;
				
				//add character to the matrix
				charLevel2d[index/(colNum-1)][index%(colNum-1)] = c;
				index++;
				
			}
		}
		
		//calculate neighbor scores
		int tileRowNum = charLevel2d.length;
		int tileColNum = charLevel2d[0].length;
		int[][] scoreMatrix = new int[tileRowNum][tileColNum];
		
		for(int i=0;i<tileRowNum;i++) {
			for(int j=0;j<tileColNum;j++) {
				scoreMatrix[i][j] = getDifference(charLevel2d[i][j],getNeighbors(i,j,charLevel2d));
			}
		}
		
		//count up all neighbor scores (4 directions -> 5 possible scores)
		int[] dir = new int[5]; 
		for(int i=0;i<5;i++) {
			dir[i] = 0;
		}
		for(int i=0;i<tileRowNum;i++) {
			for(int j=0;j<tileColNum;j++) {
				dir[scoreMatrix[i][j]]++;
			}
		}
		
//		//DEBUG - check score matrix
//		for(int i=0;i<tileRowNum;i++) {
//			for(int j=0;j<tileColNum;j++) {
//				System.out.print(scoreMatrix[i][j]);
//			}
//			System.out.println("");
//		}
		
		//take the entropy of the 4 directions
		int total = tileRowNum*tileColNum;
		double entropy = 0.0;
		for(int i=0;i<5;i++) {
			double prob = ((double)dir[i] / (double)total);
			//System.out.println(dir[i] + "/" + total + " = " + prob);
			if(prob > 0)
				entropy += -(prob*Math.log10(prob));
		}
		
		//return the final derivative entropy
		return 1.0 - entropy;
	}
	
	//returns the surrounding tiles of a specific tile index in a 2d level
	private char[] getNeighbors(int row, int col, char[][] level) {
		ArrayList<Character> neighbors = new ArrayList<Character>();
		
		if(row > 0) {	//top
			neighbors.add(level[row-1][col]);
		}
		if(row < level.length-1) {	//bottom
			neighbors.add(level[row+1][col]);
		}
		if(col > 0) {	//left
			neighbors.add(level[row][col-1]);
		}
		if(col < level[0].length-1) {
			neighbors.add(level[row][col+1]);
		}
		
		char[] neighbors2 = new char[neighbors.size()];
		for(int i=0;i<neighbors.size();i++) {
			neighbors2[i] = neighbors.get(i);
		}
		return neighbors2;
	}
	
	//returns the number of differing tiles from neighboring tiles
	private int getDifference(char tile, char[] neighbors) {
		int d=0;
		for(char c : neighbors) {
			if(c != tile) {
				d++;
			}
		}
		return d;
	}


	//calculates level dimensions based on the game's interaction ruleset
	//and results in a binary vector for whether the rule was triggered during the agent's run of the level
	//DEMO: use Zelda rules (player killed enemy? [0-2] player picked up key? [3])
	private void calculateDimensions(int id) {
		//System.out.println("calculating dimensions...");

		//create a new dimension set based on the size of _rules and set all values to 0
		this._dimensions = new int[_rules.size()];
		for(int d=0;d<this._dimensions.length;d++) {
			this._dimensions[d] = 0;
		}


		try {
			//setup the files
			String col_json = Chromosome.outputInteractionJSON.replaceFirst("%", ("_col_"+id));

			///////    COLLISION INTERACTIONS   ///////
			
			BufferedReader colInterRead = new BufferedReader(new FileReader(col_json));
			//parse each line (assuming 1 object per line)
			String line = colInterRead.readLine();
			while(line != null) {
				//System.out.println(line);
				JSONObject obj = (JSONObject)new JSONParser().parse(line);
				String action = obj.get("interaction").toString();
				String sprite2 = obj.get("sprite2").toString();
				String sprite1 = obj.get("sprite1").toString();

				String[] tryKey = {action, sprite2, sprite1, "Collides"};

				//System.out.println(Arrays.deepToString(tryKey));

				//confirm the interaction in the dimension space
				int ruleIndex = hasRule(tryKey);
				if(ruleIndex >= 0) {
					_dimensions[ruleIndex] = 1;
				}
				line = colInterRead.readLine();
			}
			colInterRead.close();


		}catch(FileNotFoundException e) {
			System.out.println("Unable to open file '" + Chromosome.outputInteractionJSON.replaceFirst("%", ("_col_"+id)) + "'");                
		}
		catch(IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		
		try {
			//setup the files
			String play_json = Chromosome.outputInteractionJSON.replaceFirst("%", ("_key_"+id));

			
			///////    PLAYER INTERACTIONS   ///////
			
			 //read the file
			String contents = new String(Files.readAllBytes(Paths.get(play_json)));
			JSONArray arr = (JSONArray) new JSONParser().parse(contents);
			
			//iterate through the array items (if they exist)
			for(int i=0;i<arr.size();i++) {
				JSONObject obj = (JSONObject)arr.get(i);
				String action = obj.get("action").toString();
				String sprite1 = obj.get("sprite1").toString();

				//String[] tryKey = {action, "", sprite1, "Player Input"};
				String[] tryKey = {"Spawn", "", sprite1, "Press Space"};		//for now default "ACTION_USE" to "Press Space"
				
				//System.out.println(Arrays.deepToString(tryKey));

				//confirm the interaction in the dimension space
				int ruleIndex = hasRule(tryKey);
				if(ruleIndex >= 0) {
					_dimensions[ruleIndex] = 1;
				}
			}
			
			
			/*
			//parse each line (assuming 1 object per line)
			String line2 = playInterRead.readLine();
			while(line2 != null) {
				//System.out.println(line2);
				JSONObject obj = (JSONObject)new JSONParser().parse(line2);
				String action = obj.get("action").toString();
				String sprite1 = obj.get("sprite1").toString();

				String[] tryKey = {action, "", sprite1, "Player Input"};

				//System.out.println(Arrays.deepToString(tryKey));

				//confirm the interaction in the dimension space
				int ruleIndex = hasRule(tryKey);
				if(ruleIndex >= 0) {
					_dimensions[ruleIndex] = 1;
				}
				line2 = playInterRead.readLine();
			}
			playInterRead.close();
			*/
			
			
		}catch(FileNotFoundException e) {
			System.out.println("Unable to open file '" + Chromosome.outputInteractionJSON.replaceFirst("%", ("_key_"+id)));                
		}
		catch(IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}


	//checks if a rule was enacted during the agent's run (parse through the JSON file)
	private int hasRule(String[] interaction) {
		//iterates through the whole set to see if the arrays match
		for(int r=0;r<_rules.size();r++) {
			String[] rule = _rules.get(r);

			//if not even the same length then skip
			if(rule.length != interaction.length)
				continue;

			boolean matchRule = true;
			for(int i=0;i<interaction.length;i++) {
				if(!rule[i].equals(interaction[i]))	//if a mismatch - then definitely not the same rule
					matchRule = false;
			}

			if(matchRule)
				return r;
		}
		return -1;
	}

	//tries running the doNothing() agent and returns true if it lived and false if it died
	private double try_nothing(int testNum, String outFile) throws IOException {
		/* just assume the outfile remains the same from when it was previously run
		if(outFile == null)
			outFile = Chromosome.placeholderLoc;
		copyLevelToFile(outFile);
		*/
		System.out.println("Testing doNothing " + testNum + " times...");
		
		//test doNothing() x amount of times - return false if it dies at any time
		int killCount = 0;
		for(int i=0;i<testNum;i++) {
			double[] results = ArcadeMachine.runOneGame(Chromosome._gamePath, outFile, false, "agents.doNothing.Agent", null, Chromosome._rnd.nextInt(), 0);
			if(results[2] < Chromosome.idealTime) {
				killCount++;
			}
		}
		return (double)(testNum-killCount)/(double)testNum;
	}
	
	
	
	
	
	//mutates a random tile (within the border if applicable) based on a "coin flip" (given probability between 0-1)
	public void mutate(double coinFlip) {
		double f = 0.0;
		//int ct = 0;

		//if it meets the coin flip, then pick a tile and mutate
		do {
			String[] rows = this._textLevel.split("\n");
			int r = 0;
			int c = 0;

			//if no border - use the whole game space
			if(!this._hasBorder) {
				r = new Random().nextInt(rows.length);
				c = new Random().nextInt(rows[r].length());
			}
			//if there is a border offset by 1
			else {
				r = new Random().nextInt(rows.length-2)+1;
				c = new Random().nextInt(rows[r].length()-2)+1;
			}

			int n = new Random().nextInt(Chromosome._allChar.length);

			//replace the character at the random tile with another character
			String replaceRow = rows[r].substring(0, c) + Chromosome._allChar[n] + rows[r].substring(c+1);
			rows[r] = replaceRow;
			this._textLevel = String.join("\n", rows);

			f = Math.random();
			//ct++;
		}while(f < coinFlip);

		//System.out.println("Mutated " + ct + " times");

		//remove duplicate avatars
		boolean hasAvatar = false;
		char[] charLevel = this._textLevel.toCharArray();
		for(int a = 0; a < charLevel.length; a++) {
			char levChar = charLevel[a];
			if(levChar == 'A') {
				if(!hasAvatar) {
					hasAvatar = true;
					continue;
				}else {
					//keep replacing the character until it's not an A anymore
					do{
						int n = new Random().nextInt(Chromosome._allChar.length);
						charLevel[a] = Chromosome._allChar[n].charAt(0);				
					}while (charLevel[a] == 'A');
				}
			}
		}

		//if no avatar at all - replace an empty space (or another tile if one doesn't exist) with it
		if(!hasAvatar) {
			int randPt = new Random().nextInt(charLevel.length);
			while(this._textLevel.contains(" ") && charLevel[randPt] != ' ') {
				randPt = new Random().nextInt(charLevel.length);
			}

			charLevel[randPt] = 'A';
		}

		this._textLevel = new String(charLevel);

		//otherwise finish
		return;
	}




	//file-based chromosome initialization function (uncalculated)
	//public void fileInit(int age, boolean hasBorder, String extLevel) {
	public void fileInit(String fileContents) {
		String[] fileStuff = fileContents.split("\n");

		this._age = Integer.parseInt(fileStuff[0]);
		this._hasBorder = (fileStuff[1] == "0" ? false : true);
		this._textLevel = "";
		for(int i=2;i<fileStuff.length;i++) {
			this._textLevel += (fileStuff[i] + "\n");
		}
		this._textLevel.trim();
	}

	//overwrites the results from an already calculated chromosome of a child process
	public void saveResults(String fileContents) {
		String[] fileStuff = fileContents.split("\n");

		this._age = Integer.parseInt(fileStuff[0]);
		this._hasBorder = (fileStuff[1] == "0" ? false : true);
		this._constraints = Double.parseDouble(fileStuff[2]);
		this._fitness = Double.parseDouble(fileStuff[3]);
		String[] d = fileStuff[4].split("");
		this._dimensions = new int[d.length];
		for(int i=0;i<d.length;i++) {
			this._dimensions[i] = Integer.parseInt(d[i]);
		}
	}

	
	//rewrites the chromosome (from a save point exported by the map)
	public void rewriteFromCheckpoint(String chromoMeta, String chromoLevel) {
		this.saveResults(chromoMeta);
		this._textLevel = chromoLevel;
	}


	//returns the full level
	private String fullLevel(String ph) {
		String[] lines = new IO().readFile(ph);
		return String.join("\n", lines);
	}

	/*
	 * Copies the textLevel to the placeholder file
	 * (for use with LevelGenMachine)
	 */
	public void copyLevelToFile(String file) throws IOException {
		if(file == null)
			file = Chromosome.placeholderLoc;

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(this._textLevel);
		//writer.write(this._fullTextLevel); 

		writer.close();
	}


	//clone chromosome function
	public Chromosome clone() {
		return new Chromosome(this._textLevel, this._hasBorder);
	}

	//override class toString() function
	public String toString() {
		return _textLevel;
	}

	//creates an input file format for the level (for use with parallelization)
	public String toInputFile() {
		String output = "";
		output += this._age + "\n";
		output += (this._hasBorder ? "1\n" : "0\n");
		output += (this.toString());
		return output;
	}

	//creates an output file format for the level (for use with parallelization)
	public String toOutputFile() {
		String output = "";
		output += (this._age) + "\n";
		output += (this._hasBorder ? "1\n" : "0\n");
		output += (this._constraints) + "\n";
		output += (this._fitness) + "\n";
		for(int i=0;i<this._dimensions.length;i++) {output += ("" + this._dimensions[i]);} output += "\n";
		//output += (this.toString());
		return output;

	}

	//creates a checkpoint file format for the level (exports entire information about the chromosome)
	public String toCheckPointFile() {
		String topOutput = toOutputFile();
		String txtLevel = "\n\n"+toString();
		String totalOutput = topOutput + txtLevel;
		return totalOutput;
	}
	
	
	
	/**
	 * compares the constraints and fitness of 2 chromosomes
	 * taken directly from Chromosome.java [MarioAI]
	 * 
	 * @param o the compared Chromosome object
	 */
	@Override
	public int compareTo(Chromosome o) {
		//double threshold = 1.0/10.0;		//within 10 ticks of ideal time
		double threshold = Chromosome.compareThreshold;
		
		if ((this._constraints >= threshold) && (o._constraints >= threshold)) {
			return (int) Math.signum(this._fitness - o._fitness);
		}
		return (int) Math.signum(this._constraints - o._constraints);
	}

	//////////  GETTER FUNCTIONS  ///////////
	public int get_age() {
		return _age;
	}

	public double getConstraints() {
		return this._constraints;
	}

	public double getFitness() {
		return this._fitness;
	}

	public int[] getDimensions() {
		return this._dimensions;
	}

	///////////  SETTER FUNCTIONS  //////////
	public void incrementAge() {
		this._age++;
	}
	

	///////////   HELPER FUNCTIONS   ////////////
	//gets the index of a character in an array (helper function)
	public int indexOf(char[] arr, char elem) {
		for(int i=0;i<arr.length;i++) {
			if(arr[i] == elem)
				return i;
		}
		return -1;
	}

	//log base 2 converter (helper function)
	public double log2(double x)
	{
		return (Math.log(x) / Math.log(2.0));
	}
}