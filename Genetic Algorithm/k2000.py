import copy
import random
INPUT       = 'inputt'

DNA_SIZE    = 0  # DNA of the genetic algorithm, subject to mutation
NUM_AGENTS  = 0  
NUM_JOBS    = 0

OPSEQ_X_CHANCE = 50 #Chance of operational sequence crossover
AGENT_X_CHANCE = 50 #Chance of agent assignment crossover

POP_SIZE    = 400  #initial population size
GENERATIONS = 2000 #evolution generations

def print_dna(dna):
    '''
    Support function to help visalize the job assignment and sequence
    '''
    for seq, agent in dna:
        print("job:%d o:%d a%d:%d" % (seq[0], seq[1], agent, cost_matrix[agent][seq[1]]))

def validate_dna(dna):
    '''
    Support function to validate a DNA so that it does not validate casuality hiarachy. 
    '''
    counter = [0] * NUM_JOBS
    for seq, agent in dna:
        job = seq[0]
        op = seq[1]
        if(counter[job] > op):
            return False
        counter[job] = op
    return True

def init_population(time_matrix, jobs):
    '''
    Building the initial population for GA
    Randomly build the DNA out of all available jobs and agents. The random DNA still follows 
        the operation sequence contraints. So o2 cannot happen before o1. Agents are randomly 
        assigned. Can be optimized to have the least efficient assignment to prevent falling 
        into a local minima.
    '''
  init_pop = []
  num_agents = len(time_matrix)
  for i in range(POP_SIZE): #initial population size
    dna = []
    num_jobs = len(jobs)
    counters = [0] * num_jobs 
    choices = list(range(num_jobs)) # all available choices
    for c in range(DNA_SIZE): 
        #random queue merge, while preserving queue fifo hiarachy 
        agent_num = random.randrange(num_agents)
        i = random.choice(choices)
        j = jobs[i][counters[i]] 
        dna.append(((i,j), agent_num))
        counters[i] = counters[i]+1
        if(counters[i] >= len(jobs[i])): #done
            choices.remove(i)
    init_pop.append(dna)
  #print(init_pop)
  return init_pop

def fitness(dna):
    '''
    Evaluates the time for current schedule, returns a fitness score aka runtime.
        Fitness function takes into account concurrent jobs
    '''
  agent_cost=[0]*NUM_AGENTS
  job_time=[0]*NUM_JOBS
   
  for o_seq, agent in dna:
    agent_cost[agent]=max(agent_cost[agent],job_time[o_seq[0]])+cost_matrix[agent][o_seq[1]]
    job_time[o_seq[0]]=agent_cost[agent]

  return max(agent_cost)

def agents_mutate(dna):
    '''
    Mutation function for randomly mutating agents assignment. 2% chance of occuring
    '''
  new_dna=[]
  mutation_chance = 100
  for c in list(range(DNA_SIZE)):
    if random.randint(1, mutation_chance) == 1:
      new_dna.append((dna[c][0],random.randint(0, NUM_AGENTS-1)))
    else:
        new_dna.append(dna[c])

  return new_dna

def agents_crossover(dna1, dna2):
    '''
    Simple agents crossover, randomly mixing agents from two parents DNA
    '''
  agent1=[]
  agent2=[]
  child1=[]
  child2=[]
  new_dna1=[]
  new_dna2=[]

  for seq, agent in dna1:
      agent1.append(agent)
  for seq, agent in dna2:
      agent2.append(agent)
  pos = int(random.random()*DNA_SIZE)
  child1=agent1[:pos]+agent2[pos:]
  child2=agent2[:pos]+agent1[pos:]
  for i in range(0,DNA_SIZE):
    new_dna1.append((dna1[i][0],child1[i]))
    new_dna2.append((dna2[i][0],child2[i]))
  return (new_dna1, new_dna2)

def opseq_crossover(dna1, dna2):
    '''
    Complex operation sequence crossover. Inspired by PSX crossover. Crossover two parents DNA's
        operations sequence while still preserve the order hiararchy. 
    '''

  x_seq1 = [];
  x_seq2 = [];
  x1 = []; # crossover list, 
  x2 = [];
  c1 = [None] * DNA_SIZE
  c2 = [None] * DNA_SIZE
  job_x = random.randrange(NUM_JOBS)
  for i in range(DNA_SIZE):
      # Randomly pick 1 or more jobs in the DNA to crossover
      #import ipdb; ipdb.set_trace()
      if(dna1[i][0][0] == job_x):
          x1.append(dna1[i][0])
          x_seq1.append(i)
      if(dna2[i][0][0] == job_x):
          x2.append(dna2[i][0])
          x_seq2.append(i)

  #step1 cross over P1's selected jobs & its sequence to children
  for i in x_seq1:  #prefill
      c1[i] = dna1[i];
  for i in x_seq2:
      c2[i] = dna2[i];

  #step2 cross over P2's sequence to children while preserving sequence
  i = 0
  for seq, agent in dna1:
      while i < len(c2) and c2[i]: # Skip prefilled 
        i += 1;
      if seq in x2:
          continue
      c2[i] = (seq, agent)
      i += 1;

  i = 0
  for seq, agent in dna2:
      while i < len(c1) and c1[i]: # Skip prefilled 
        i += 1;
      if seq in x1:
          continue
      c1[i] = (seq, agent)
      i += 1;

  #if not validate_dna(c1):
    #import ipdb; ipdb.set_trace();
  #if not validate_dna(c2):
    #import ipdb; ipdb.set_trace();
  return (c1, c2)

def parse_input():
    '''
    Input file adapter, reads from a common file for agent operation cost matrix, jobs
    '''
    import fileinput
    from collections import deque
    lines = deque([line for line in open(INPUT)])
    cost_matrix = []
    lines.popleft() #remove first line
    while 'num_recipes' not in lines[0]:
        line = lines.popleft()
        if "time_matrix" in line:
            continue
        row = [int(i) for i in line.split(' ')]
        cost_matrix.append(row)
    recipes = []
    lines.popleft() #remove first line
    
    total_ops = 0
    while 'time_vector' not in lines[0]:
        line = lines.popleft()
        row = [int(i) for i in line.split(' ')]
        recipes.append(row)
        total_ops += len(row)

    lines.popleft() #remove first line
    time_vector_str = lines.popleft() #remove first line
    time_vector_str = time_vector_str.rstrip()
    time_vector = [(int(i) if i != ' ' else -1) for i in list(time_vector_str)]
    global DNA_SIZE, NUM_AGENTS, NUM_JOBS
    DNA_SIZE = total_ops
    NUM_AGENTS = len(cost_matrix)
    NUM_JOBS = len(recipes)
    return (cost_matrix, recipes, time_vector)

(cost_matrix, recipes, time_vector) = parse_input()
population = init_population(cost_matrix, recipes)
best_dna=population[0]
best_fitness=fitness(best_dna)
# Main GA implementation
for generation in list(range(GENERATIONS)):
    weighted_population = []


    # binary tournament
    for individual in population:
        fitness_val = fitness(individual)
        pair = (individual, fitness_val)
        weighted_population.append(pair)

    population = []

    for _ in range(int(POP_SIZE/2)):
        # Selection of best DNA in population
        temp1 = random.choice (weighted_population)
        temp2 = random.choice (weighted_population)

        if (temp1[1]<temp2[1]):
            ind1 =temp1[0]
        else:
            ind1 =temp2[0]

        temp1 = random.choice (weighted_population)
        temp2 = random.choice (weighted_population)

        if (temp1[1]<temp2[1]):
            ind2 =temp1[0]
        else:
            ind2 =temp2[0]


        # Crossover selection with probability distribution
        total_chance = OPSEQ_X_CHANCE + AGENT_X_CHANCE
        choice = random.randrange(total_chance)  
        if choice < OPSEQ_X_CHANCE:
            #complex opseq crossover (POX)
            ind1, ind2 = opseq_crossover(ind1, ind2)
            population.append(ind1)
            population.append(ind2)
        if choice < OPSEQ_X_CHANCE+AGENT_X_CHANCE:
            # Simple agents cross over with a chance of mutation
            ind1, ind2 = agents_crossover(ind1, ind2)
            population.append(agents_mutate(ind1))
            population.append(agents_mutate(ind2))



    # selecting the best fit function
    fittest_dna = population[0]
    minimum_fitness = fitness(population[0])

    for individual in population:
        ind_fitness = fitness(individual)
        if ind_fitness <= minimum_fitness:
            fittest_dna= individual
            minimum_fitness = ind_fitness

    if(minimum_fitness<best_fitness):
        best_fitness=minimum_fitness
        best_dna=fittest_dna
print (best_dna)
print (best_fitness)
validate_dna(best_dna)
print_dna(best_dna)