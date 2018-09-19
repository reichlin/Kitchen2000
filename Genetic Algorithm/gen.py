import random
############
# This is the generator file for generating output files
###########
MAX_O = 11          # Maximum operations
MAX_N = 5           # Maximum number of agents
MAX_RECIPES =  5    # Maximum number of jobs (recipes)
MAX_COST = 3        # Maximum value in cost matrix
MAX_DURATION = 20   # Max time duration that orders are allowed to come in


o = random.randrange(10, MAX_O) # number of operations
n = random.randrange(3, MAX_N)
num_recipes = random.randrange(3, MAX_RECIPES)
recipes = []

def gen_cost_matrix(n,o):
    '''
    Generate time consumption per each agent
    '''
    matrix = []
    for i in range(n):
        matrix.append(random.choices(range(1,MAX_COST),k=o))

    return matrix

def gen_recipe(recipe_length):
    '''
    Generate recipes based on O
    '''
    recipe = random.sample(range(o),k=recipe_length)
    recipe.sort()
    #random.shuffle(recipe)
    return recipe


print("time_matrix %d %d" % (n , o))
matrix = gen_cost_matrix(n, o)
for m in matrix:
    time_matrix = " ".join(str(i) for i in m)
    print(time_matrix)

print("num_recipes %d" % num_recipes)
for i in range(num_recipes):
    recipe_length = random.randrange(5, o)
    r = gen_recipe(recipe_length)
    print(" ".join(str(i) for i in r))


print("time_vector %d" % MAX_DURATION)
time_vector = list(range(num_recipes))
time_vector.extend([-1 for i in range(MAX_DURATION-num_recipes)])
random.shuffle(time_vector)
print("".join((str(i) if i > -1 else " ") for i in time_vector).lstrip())