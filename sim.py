import random

# Simulation parameters
num_simulations = 1000
max_rounds = 10

# Unit stats
unit_a_size = 6
unit_a_hit_on = 4  # Hits on 4+ (4, 5, 6)
unit_a_block_on = 5  # Blocks on 5+ (5, 6)

unit_b_size = 6
unit_b_hit_on = 5  # Hits on 4+ (4, 5, 6)
unit_b_block_on = 5  # Blocks on 5+ (5, 6)

# Function to roll dice and count successes
def roll_dice(num_dice, success_threshold):
    return sum(1 for _ in range(num_dice) if random.randint(1, 6) >= success_threshold)

# Function to simulate one combat
def simulate_combat():
    unit_a_remaining = unit_a_size
    unit_b_remaining = unit_b_size

    for round_num in range(1, max_rounds + 1):
        unit_a_hits = roll_dice(unit_a_remaining, unit_a_hit_on)
        mod = 1 if unit_b_remaining == unit_b_size else 2
        unit_b_hits = roll_dice(unit_b_remaining * mod, unit_b_hit_on)

        unit_b_blocks = roll_dice(unit_a_hits, unit_b_block_on)
        unit_b_extra_hits = unit_b_blocks  # B's special rule: successful block causes extra hit
        unit_b_damage_taken = unit_a_hits
        unit_b_remaining -= max(0, unit_b_damage_taken)

        # Unit B attacks
        
        unit_a_blocks = roll_dice(unit_b_hits, unit_a_block_on)
        unit_a_damage_taken = unit_b_hits + unit_b_extra_hits - unit_a_blocks
        unit_a_remaining -= max(0, unit_a_damage_taken)

        # Check if one unit is eliminated
        if unit_a_remaining <= 0 and unit_b_remaining > 0:
            return "Quill", round_num
        elif unit_b_remaining <= 0 and unit_a_remaining > 0:
            return "Goblin", round_num
        elif unit_a_remaining <= 0 and unit_b_remaining <= 0:
            return "Draw", round_num

    return "Draw", max_rounds

# Run simulations
results = {"Goblin": 0, "Quill": 0, "Draw": 0}
rounds_to_win = []

for _ in range(num_simulations):
    winner, rounds = simulate_combat()
    results[winner] += 1
    rounds_to_win.append(rounds)

# Display results
total_simulations = sum(results.values())
for outcome, count in results.items():
    percentage = (count / total_simulations) * 100
    print(f"{outcome}: {count} wins ({percentage:.2f}%)")

# Calculate and display the average number of rounds to determine a winner
average_rounds = sum(rounds_to_win) / len(rounds_to_win)
print(f"\nAverage rounds per combat: {average_rounds:.2f}")