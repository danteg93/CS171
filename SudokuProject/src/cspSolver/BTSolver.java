package cspSolver;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sudoku.Converter;
import sudoku.SudokuFile;
/**
 * Backtracking solver. 
 *
 */
public class BTSolver implements Runnable{

	//===============================================================================
	// Properties
	//===============================================================================

	private ConstraintNetwork network;
	private static Trail trail = Trail.getTrail();
	private boolean hasSolution = false;
	private SudokuFile sudokuGrid;

	private int numAssignments;
	private int numBacktracks;
	private long startTime;
	private long endTime;
	
	public enum VariableSelectionHeuristic { None, MinimumRemainingValue, Degree, MRVDH};
	public enum ValueSelectionHeuristic    { None, LeastConstrainingValue };
	public enum ConsistencyCheck		   { None, ForwardChecking, ArcConsistency };
	
	private VariableSelectionHeuristic varHeuristics;
	private ValueSelectionHeuristic valHeuristics;
	private ConsistencyCheck cChecks;

	//===============================================================================
	// Constructors
	//===============================================================================

	public BTSolver(SudokuFile sf)
	{
		this.network = Converter.SudokuFileToConstraintNetwork(sf);
		this.sudokuGrid = sf;
		numAssignments = 0;
		numBacktracks = 0;
	}

	//===============================================================================
	// Modifiers
	//===============================================================================
	
	public void setVariableSelectionHeuristic(VariableSelectionHeuristic vsh)
	{
		this.varHeuristics = vsh;
	}
	
	public void setValueSelectionHeuristic(ValueSelectionHeuristic vsh)
	{
		this.valHeuristics = vsh;
	}
	
	public void setConsistencyChecks(ConsistencyCheck cc)
	{
		this.cChecks = cc;
	}
	//===============================================================================
	// Accessors
	//===============================================================================

	/** 
	 * @return true if a solution has been found, false otherwise. 
	 */
	public boolean hasSolution()
	{
		return hasSolution;
	}

	/**
	 * @return solution if a solution has been found, otherwise returns the unsolved puzzle.
	 */
	public SudokuFile getSolution()
	{
		return sudokuGrid;
	}

	public void printSolverStats()
	{
		System.out.println("Time taken:" + (endTime-startTime) + " ms");
		System.out.println("Number of assignments: " + numAssignments);
		System.out.println("Number of backtracks: " + numBacktracks);
	}

	public String getSolverStats(long preprocessingStart, long preprocessingDone){
		StringBuilder sb = new StringBuilder();
		sb.append("TOTAL_START=" + preprocessingStart + "\n");
		sb.append("PREPROCESSING_START=" + preprocessingStart + "\n");
		sb.append("PREPROCESSING_DONE=" + preprocessingDone + "\n");
		sb.append("SEARCH_START=" + startTime + "\n");
		sb.append("SEARCH_DONE=" + endTime + "\n");
		sb.append("SOLUTION_TIME=" + ((preprocessingDone - preprocessingStart) + getTimeTaken()) + "\n");
		if (hasSolution) {
			sb.append("STATUS=success\n");
			sb.append("SOLUTION=" + sudokuGrid.getOneLineSolution() + "\n");			
		} else {
			sb.append("STATUS=timeout\n");
			sb.append("SOLUTION=" + sudokuGrid.getOneLineFailure() + "\n");
		}
		sb.append("COUNT_NODES=" + numAssignments + "\n");
		sb.append("COUNT_DEADENDS=" + numBacktracks + "\n");
		return sb.toString();
	}

	/**
	 * 
	 * @return time required for the solver to attain in seconds
	 */
	public long getTimeTaken()
	{
		return endTime-startTime;
	}

	public int getNumAssignments()
	{
		return numAssignments;
	}

	public int getNumBacktracks()
	{
		return numBacktracks;
	}

	public ConstraintNetwork getNetwork()
	{
		return network;
	}

	//===============================================================================
	// Helper Methods
	//===============================================================================

	/**
	 * Checks whether the changes from the last time this method was called are consistent. 
	 * @return true if consistent, false otherwise
	 */
	private boolean checkConsistency()
	{
		boolean isConsistent = false;
		switch(cChecks)
		{
		case None: 				isConsistent = assignmentsCheck();
		break;
		case ForwardChecking: 	isConsistent = forwardChecking();
		break;
		case ArcConsistency: 	isConsistent = arcConsistency();
		break;
		default: 				isConsistent = assignmentsCheck();
		break;
		}
		return isConsistent;
	}
	
	/**
	 * default consistency check. Ensures no two variables are assigned to the same value.
	 * @return true if consistent, false otherwise. 
	 */
	private boolean assignmentsCheck()
	{
		for(Variable v : network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther : network.getNeighborsOfVariable(v))
				{
					if (v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement forward checking. 
	 */
	private boolean forwardChecking()
	{
		for(Variable v : network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther : network.getNeighborsOfVariable(v))
				{
					if(v.getAssignment() == vOther.getAssignment()){
						return false;
					}
					vOther.removeValueFromDomain(v.getAssignment());
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement Maintaining Arc Consistency.
	 */
	private boolean arcConsistency()
	{
		return false;
	}
	
	/**
	 * Selects the next variable to check.
	 * @return next variable to check. null if there are no more variables to check. 
	 */
	private Variable selectNextVariable()
	{
		Variable next = null;
		switch(varHeuristics)
		{

		case None:                  next = getfirstUnassignedVariable();
		break;
		case MinimumRemainingValue: next = getMRV();
		break;
		case Degree:				next = getDegree();
		break;
		case MRVDH:                 next = getMRVWithDH();
		break;
		default:					next = getfirstUnassignedVariable();
		break;
		}
		return next;
	}
	
	/**
	 * default next variable selection heuristic. Selects the first unassigned variable. 
	 * @return first unassigned variable. null if no variables are unassigned. 
	 */
	private Variable getfirstUnassignedVariable()
	{
		for(Variable v : network.getVariables())
		{
			if(!v.isAssigned())
			{
				return v;
			}
		}
		return null;
	}

	/**
	 * TODO: Implement MRV heuristic
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned. 
	 */
	private Variable getMRV()
	{
		List<Variable> tempNetwork = network.getVariables();
		int minRemainingValues = Integer.MAX_VALUE;
		Variable tempVar = tempNetwork.get(0);
		for(Variable v: tempNetwork){
			if(!v.isAssigned() && v.size() < minRemainingValues)
			{
				minRemainingValues = v.size();
				tempVar = v;
			}
		}
		if(minRemainingValues != Integer.MAX_VALUE){
			return tempVar;
		}
		return null;
	}
	
	/**
	 * TODO: Implement Degree heuristic
	 * @return variable constrained by the most unassigned variables, null if all variables are assigned.
	 */
	private Variable getDegree()
	{
		List<Variable> tempNetwork = network.getVariables();
		boolean firstTime = true;
		Variable tempVar = null;
		
		for(Variable v: tempNetwork){
			if(!v.isAssigned()){
				if(firstTime){
					firstTime = false;
					tempVar = v;
				}
				else{
					if(getDegree(v) >= getDegree(tempVar)){
						tempVar = v;
					}
				}
			}
		}
		return tempVar;
	}
	/**
	 *  This function gets the degree of a variable
	 */
	private int getDegree(Variable v){
		int degree = 0;
		for(Variable w: network.getNeighborsOfVariable(v)){
			if(!w.isAssigned()){
				degree++;
			}
		}
		return degree;
	}
	/**
	 * TODO: Implement MRV WITH DH
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned DH as tie breaker.
	 */
	private Variable getMRVWithDH()
	{
		boolean firstTime = true;
		List<Variable> tempNetwork = network.getVariables();
		Variable tempVar = null;
		
		for(Variable v: tempNetwork){
			if(!v.isAssigned()){
				if(firstTime){
					firstTime = false;
					tempVar = v;
				}
				else{
					if(v.size() < tempVar.size()){
						tempVar = v;		
					}
					else if(v.size() == tempVar.size()){
						if(getDegree(v) >= getDegree(tempVar)){
							tempVar = v;
						}
					
					}
				}
			}
		}
		return tempVar;
	}
	
	/**
	 * Value Selection Heuristics. Orders the values in the domain of the variable 
	 * passed as a parameter and returns them as a list.
	 * @return List of values in the domain of a variable in a specified order. 
	 */
	public List<Integer> getNextValues(Variable v)
	{
		List<Integer> orderedValues;
		switch(valHeuristics)
		{
		case None: 						orderedValues = getValuesInOrder(v);
		break;
		case LeastConstrainingValue: 	orderedValues = getValuesLCVOrder(v);
		break;
		default:						orderedValues = getValuesInOrder(v);
		break;
		}
		return orderedValues;
	}
	
	/**
	 * Default value ordering. 
	 * @param v Variable whose values need to be ordered
	 * @return values ordered by lowest to highest. 
	 */
	public List<Integer> getValuesInOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	
	/**
	 * TODO: LCV heuristic
	 */
	public List<Integer> getValuesLCVOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		final Variable tempv = v;
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				Integer timesi1InDomainOfCells = 0;
				Integer timesi2InDomainOfCells = 0;
				for(Variable w: network.getNeighborsOfVariable(tempv)){
					if(!w.isAssigned()){
						List<Integer> neighborDomain = w.getDomain().getValues();
						if(neighborDomain.contains(i1)){
							timesi1InDomainOfCells++;
						}
						else if(neighborDomain.contains(i2)){
							timesi2InDomainOfCells++;
						}
					}
				}
				return timesi1InDomainOfCells.compareTo(timesi2InDomainOfCells);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	/**
	 * Called when solver finds a solution
	 */
	private void success()
	{
		hasSolution = true;
		sudokuGrid = Converter.ConstraintNetworkToSudokuFile(network, sudokuGrid.getN(), sudokuGrid.getP(), sudokuGrid.getQ());
	}

	//===============================================================================
	// Solver
	//===============================================================================

	/**
	 * Method to start the solver
	 */
	public void solve()
	{
		startTime = System.currentTimeMillis();
		try {
			solve(0);
		}catch (VariableSelectionException e)
		{
			System.out.println("error with variable selection heuristic.");
		}
		endTime = System.currentTimeMillis();
		Trail.clearTrail();
	}

	/**
	 * Solver
	 * @param level How deep the solver is in its recursion. 
	 * @throws VariableSelectionException 
	 */

	private void solve(int level) throws VariableSelectionException
	{
		if(!Thread.currentThread().isInterrupted())

		{//Check if assignment is completed
			if(hasSolution)
			{
				return;
			}

			//Select unassigned variable
			Variable v = selectNextVariable();		
			//check if the assignment is complete
			if(v == null)
			{
				for(Variable var : network.getVariables())
				{
					if(!var.isAssigned())
					{
						throw new VariableSelectionException("Something happened with the variable selection heuristic");
					}
				}
				success();
				return;
			}
			
			//System.out.println(v.getName());
			//loop through the values of the variable being checked LCV

			
			for(Integer i : getNextValues(v))
			{
				trail.placeBreadCrumb();

				//check a value
				v.updateDomain(new Domain(i));
				numAssignments++;
				boolean isConsistent = checkConsistency();
				
				//move to the next assignment
				if(isConsistent)
				{		
					solve(level + 1);
				}

				//if this assignment failed at any stage, backtrack
				if(!hasSolution)
				{
					trail.undo();
					numBacktracks++;
				}
				
				else
				{
					return;
				}
			}	
		}	
	}

	@Override
	public void run() {
		solve();
	}
}
