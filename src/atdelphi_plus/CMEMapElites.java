package atdelphi_plus;

//Program by Megan "Milk" Charity
//frankensteined from CMEMapElites MarioICDL by amidos


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CMEMapElites {
	private String _gameName;
	private double _coinFlip;

	private HashMap<String, Chromosome> _map = new HashMap<String, Chromosome>();		//as a test use just the chromosome as the value
	//private HashMap<String, CMECell> _map = new HashMap<String, CMECell>();

	private ArrayList<String[]> tutInteractionDict = new ArrayList<String[]>();



	public CMEMapElites(String gn, String gl, Random seed, double coinFlip, String genFolder, String tutFolder, int idealTime, double compareThresh) {
		this._gameName = gn;
		this._coinFlip = coinFlip;

		ParseTutorialRules(tutFolder);
		Chromosome.SetStaticVar(seed, gn, gl, genFolder, tutInteractionDict, idealTime, compareThresh);
	}

	//returns a batch of randomly created chromosomes
	public Chromosome[] randomChromosomes(int batchSize, String placeholder) {
		//System.out.println("MAP: Randomly generating " + batchSize +" chromosomes...");
		Chromosome[] randos = new Chromosome[batchSize];
		for(int i=0;i<batchSize;i++) {
			randos[i] = new Chromosome();
			randos[i].randomInit(placeholder);
		}
		return randos;
	}

	//assigns the new set of chromosomes to the map elites hash if their fitness scores are better than the saved chromosomes 
	public void assignChromosomes(Chromosome[] csomes) {
		for(Chromosome c : csomes) {
			int[] raw_dimen = c._dimensions;
			String dimen = dimensionsString(raw_dimen);

			//this dimensionality hasn't been saved to the map yet - so add it automatically
			if(!_map.containsKey(dimen)) {
				System.out.println("Added new cell: " + dimen);
				_map.put(dimen, c);
			}else {
				Chromosome set_c = _map.get(dimen);
				//replace the current chromosome if the new one is better
				if(set_c.compareTo(c) < 0) {
					System.out.println(dimen + ": replacing " + set_c._constraints + "/" + set_c._fitness + " --> " + c._constraints + "/" + c._fitness);
					_map.replace(dimen, c);
				}
				//otherwise increase the current elite's age
				else {
					_map.get(dimen).incrementAge();
				}
			}
		}
	}

	//retuns the current mapping of chromosomes
	public Chromosome[] getCells() {
		Chromosome[] cells = new Chromosome[_map.size()];
		int index = 0;
		for(Entry<String,Chromosome> pair : this._map.entrySet()) {
			cells[index] = pair.getValue();
			index += 1;
		}
		return cells;
	}

	//generates the next batch of chromosomes from the elite cells and mutates them
	public Chromosome[] makeNextGeneration(int batchSize, double randomPerc, String placeholder) {
		int randomAmt = (int)Math.round(batchSize*randomPerc);
		int eliteAmt = batchSize-randomAmt;
		
		//System.out.println("Elite levels: " + eliteAmt + "/" + batchSize + " | Random levels: " + randomAmt + "/" + batchSize);
		
		
		//generate the population from the elite map cells
		Chromosome[] nextGen = new Chromosome[batchSize];
		Chromosome[] eliteCells = getCells();

		for(int b=0;b<eliteAmt;b++) {
			//pick a random elite chromosome
			Chromosome randElite = eliteCells[new Random().nextInt(eliteCells.length)];
			Chromosome mutChromo = randElite.clone();

			//mutate it
			mutChromo.mutate(this._coinFlip);

			//add it to the next generation
			nextGen[b] = mutChromo;
		}
		
		//generate the random levels
		if(randomAmt > 0) {
			Chromosome[] randGen = this.randomChromosomes(randomAmt, placeholder);
			for(int r=0;r<randomAmt;r++) {
				nextGen[eliteAmt+r] = randGen[r];
			}
		}
		

		return nextGen;
	}
	
	//prints the stats for the generation given in the form:
	// "gen #", "max fitness of gen", "# feasible chromosomes", "# infeasible chromosomes"
	public String printGenStats(int genNum, Chromosome[] generation) {
		String str = "";
		
		//add generation number
		str += genNum + ", ";
		
		//calculate the maximum fitness
		double maxFit = getGenMaxFitness(generation);
		str += maxFit + ", ";
		
		//count up the feasible and infeasbile chromosomes
		int[] feasCt = countFeasibleInfeasible(generation);
		str += feasCt[0] + ", " + feasCt[1];
		
		return str;
	}
	
	//returns the average fitness of the MAP
	public double getMapAvgFitness() {
		Chromosome[] cset = getCells();
		double avg = 0;
		for(Chromosome c : cset) {
			avg += c.getFitness();
		}
		return avg/cset.length;
	}
	
	//gets the maximum fitness amount of a generation of chromosomes
	public double getGenMaxFitness(Chromosome[] generation) {
		double maxFit = 0;
		for(Chromosome c : generation) {
			if(c.getFitness() > maxFit)
				maxFit = c.getFitness();
		}
		return maxFit;
	}
	
	//returns the number of feasible and infeasible chromosomes of a generation
	// format: [# feasible, # infeasbile]
	public int[] countFeasibleInfeasible(Chromosome[] generation) {
		int[] ct = {0, 0};
		
		for(Chromosome c:generation) {
			if(c._constraints >= Chromosome.compareThreshold)
				ct[0]++;
			else
				ct[1]++;
		}
		
		return ct;
	}

	//returns the dimensions binary vector as a string (i.e. [0,1,0,1] => 0101)
	private String dimensionsString(int[] d) {
		String s = "";
		for(int i=0;i<d.length;i++) {
			s += d[i];
		}
		return s;
	}

	
	/*
	 * OLD VERSION
	 * {inputs : [], outputs : [], action : ""}
	 * 
	//sets the game interaction set (rules) for the dimensionality
	private void ParseTutorialRules(String tutFolder) {
		//assume that the rules will come from the game's specific json file
		//as a test we will use one custom made for zelda
		//however, for the real simulation - assume we can call a function from AtDelphi (original) 
		//	that will provide these rules
		//the format of the JSON file is:
		//		{inputs (sprite2) : [], outputs (sprite1) : [], action : ""}

		String gameRuleJSON = tutFolder + _gameName + "_tut.json";		//in this scenario it is in the same folder
		try {
			//read the file
			BufferedReader jsonRead = new BufferedReader(new FileReader(gameRuleJSON));
			System.out.println("game rules file: " + gameRuleJSON);

			//parse each line (assuming 1 object per line)
			String line = jsonRead.readLine();
			while(line != null) {
				//get the input sprite list, output sprite list, and action value
				JSONObject obj = (JSONObject) new JSONParser().parse(line);
				JSONArray inputs = (JSONArray)obj.get("input");
				JSONArray outputs = (JSONArray)obj.get("output");
				String action = obj.get("action").toString();

				//add the set to the dictionary
				for(int a=0;a<inputs.size();a++) {
					for(int b=0;b<outputs.size();b++) {
						String[] key = {action, inputs.get(a).toString(), outputs.get(b).toString()};
						tutInteractionDict.add(key);
					}
				}

				line = jsonRead.readLine();
			}
			//close the file
			jsonRead.close();         
		}
		catch(FileNotFoundException e) {
			System.out.println("ERROR: Unable to open file '" + gameRuleJSON + "'");    
			System.exit(0);
		}
		catch(IOException e) {
			System.out.println("IO EXCEPTION");
			e.printStackTrace();
		}catch (ParseException e) {
			System.out.println("PARSE EXCEPTION");
			e.printStackTrace();
		}
	}
	*/
	
	
	

	//sets the game interaction set (rules) for the dimensionality
		/*
		 * OLD VERSION
		 * {"condition":"","action":"","sprite2":"","sprite1":""}
		 * 
		 
		private void ParseTutorialRules(String tutFolder) {
			//assume that the rules will come from the game's specific json file
			//as a test we will use one custom made for zelda
			//however, for the real simulation - assume we can call a function from AtDelphi (original) 
			//	that will provide these rules
			//the format of the JSON file is:
			//		{"condition":"n\/a","action":"KillSprite","sprite2":"monsterQuick","sprite1":"nokey"},
			
			String gameRuleJSON = tutFolder + _gameName + "_tut.json";	
			try {
		        //read the file
				String contents = new String(Files.readAllBytes(Paths.get(gameRuleJSON)));
				
				//clean up the set
				
				//assume the input was surrounded by [ and ]
				contents = contents.substring(1, contents.length()-1);
				contents.trim();
				//System.out.println(contents);
				
				//clean up syntax
				contents = contents.replace("},", "}\n");
				contents = contents.replace("\n\n", "\n");			//in case there already was \n
				//contents = contents.replace("n\\/a", "Collision");	//mike said he would fix?
			
				String[] actions = contents.split("\n");
				for(String line : actions) {
					line.trim();
					JSONObject obj = (JSONObject) new JSONParser().parse(line);
					Object condition = obj.get("condition");
					Object action = obj.get("action");
					Object sprite2 = obj.get("sprite2");
					Object sprite1 = obj.get("sprite1");
					
					
					//look for condition, action, sprite2, and sprite1
					String[] key = {(action != null ? action.toString() : ""), 
							(sprite2 != null ? sprite2.toString() : ""),
							(sprite1 != null ? sprite1.toString() : ""), 
							(condition != null ? condition.toString() : "")};
					tutInteractionDict.add(key);
					//System.out.println(Arrays.deepToString(key));
				}
				
		    }
		    catch(FileNotFoundException e) {
		        System.out.println("ERROR: Unable to open file '" + gameRuleJSON + "'");    
		        System.exit(0);
		    }
		    catch(IOException e) {
		    	System.out.println("IO EXCEPTION");
		        e.printStackTrace();
		    }
			catch (ParseException e) {
		    	System.out.println("PARSE EXCEPTION");
				e.printStackTrace();
			}
			
		}
	*/
	
	
	private void ParseTutorialRules(String tutFolder) {
		//assume that the rules will come from the game's specific json file
		//as a test we will use one custom made for zelda
		//however, for the real simulation - assume we can call a function from AtDelphi (original) 
		//	that will provide these rules
		//the format of the JSON file is:
		//		[{"condition":"n\/a","action":"KillSprite","sprite2":"monsterQuick","sprite1":"nokey"},{...}]
		
		String gameRuleJSON = tutFolder + "critical_mechanics_" +  _gameName + ".json";	
		
		try {
	        //read the file
			String contents = new String(Files.readAllBytes(Paths.get(gameRuleJSON)));
			
			//iterate through the json array items
			JSONArray arr = (JSONArray) new JSONParser().parse(contents);
			for(int i=0;i<arr.size();i++) {
				JSONObject obj = (JSONObject) arr.get(i);
				Object condition = obj.get("condition");
				Object action = obj.get("action");
				Object sprite2 = obj.get("sprite2");
				Object sprite1 = obj.get("sprite1");
				
				//skip the win condition
				if(action != null && (action.toString().contentEquals("Win") || action.toString().contentEquals("Lose")))
					continue;
				
				//look for condition, action, sprite2, and sprite1
				String[] key = {(action != null ? action.toString() : ""), 
						(sprite2 != null ? sprite2.toString() : ""),
						(sprite1 != null ? sprite1.toString() : ""), 
						(condition != null ? condition.toString() : "")};
				tutInteractionDict.add(key);
				//System.out.println(Arrays.deepToString(key));
			}
			
	    }
	    catch(FileNotFoundException e) {
	        System.out.println("ERROR: Unable to open file '" + gameRuleJSON + "'");    
	        System.exit(0);
	    }
	    catch(IOException e) {
	    	System.out.println("IO EXCEPTION");
	        e.printStackTrace();
	    }
		catch (ParseException e) {
	    	System.out.println("PARSE EXCEPTION");
			e.printStackTrace();
		}
		
	}


	//debug to print the imported rules
	public void printRules() {
		for(String[] s : tutInteractionDict) 
			System.out.println("[" + s[0] + ", " + s[1] + ", " + s[2] + "]");
	}

	//writes the map to a single string (overrides toString() function)
	public String toString() {
		String str = "";
		//write the game name and # of elite cells first
		str += "GAME: " + this._gameName + "\n";
		str += "TOTAL CELLS: " + this._map.size() + "\n";
		str += "\n\n";

		//print the map to the file
		Set<String> keys = this._map.keySet();
		for(String k : keys) {
			Chromosome l = this._map.get(k);

			str += ("Dimensions: [" + k + "]\n");
			str += ("Age: " + l.get_age());
			str += ("\nConstraints: " + l.getConstraints());
			str += ("\nFitness: " + l.getFitness());
			str += "\nLevel: \n";
			str += (l.toString());

			str += "\n\n";
		} 

		return str;
	}

	//prints each dimension chromosome to their own file
	public void deepExport(String exportPath) throws Exception {

		Set<String> keys = this._map.keySet();

		//individually writes every level dimension, age, constraints, fitness, and text level
		for(String k : keys) {
			String wholePath = exportPath + "/" + this._gameName + "_" + k + ".txt";
			BufferedWriter bw = new BufferedWriter(new FileWriter(wholePath));

			Chromosome l = this._map.get(k);
			String str = "";

			str += ("Dimensions: [" + k + "]\n");
			str += ("Age: " + l.get_age());
			str += ("\nConstraints: " + l.getConstraints());
			str += ("\nFitness: " + l.getFitness());
			str += "\nLevel: \n";
			str += (l.toString());

			str += "\n\n";


			bw.write(str);
			bw.close();
		}
	}
	
	//exports the current map so it can be imported later
	//checkptPath should be the form:
	//	`[checkPtDir]/[gameName]_[iterationNum]/
	public void checkpointExport(String checkPtPath) throws IOException {
		Set<String> keys = this._map.keySet();
		
		//individually writes every level dimension, age, constraints, fitness, and text level
		for(String k : keys) {
			String wholePath = checkPtPath + this._gameName + "_" + k + ".txt";
			BufferedWriter bw = new BufferedWriter(new FileWriter(wholePath));

			Chromosome l = this._map.get(k);
			String str = "";

			//metadata section
			str += l.get_age() + "\n";					//age
			str += (l._hasBorder ? "1" : "0")+ "\n";	//border
			str += l.getConstraints() + "\n";			//constraints
			str += l.getFitness()+"\n";						//fitnesss
			str += this.dimensionsString(l._dimensions);		//dimension

			//text level section
			str += "\n\n";
			str += l.toString();

			bw.write(str);
			bw.close();
		}
	}
	
	//imports the checkpoint elite chromosomes to the map
	//returns the iteration number to start from
	public void checkpointImport(String checkPtPath, int iteration) throws IOException {
		//open the folder full of files
		String wholePath = checkPtPath+this._gameName+"_"+iteration+"/";
		File folder = new File(wholePath);
		
		//if the directory doesn't exist - exit
		if(!folder.exists()) {
			System.out.println("[ERROR] Cannot find directory: " + wholePath);
			return;
		}
		
		//read in the files otherwise
		String[] files = folder.list();
		for(String f : files) {
			//read in the contents
			String[] contents = new String(Files.readAllBytes(Paths.get(wholePath+f))).split("\n\n");
			String metaData = contents[0];
			String level = contents[1];
			
			//create a new chromosome
			Chromosome c = new Chromosome();
			c.rewriteFromCheckpoint(metaData, level);
			
			//get the dimension (assuming the map is empty upon initialization)
			_map.put(this.dimensionsString(c._dimensions), c);
		}
	}

}
