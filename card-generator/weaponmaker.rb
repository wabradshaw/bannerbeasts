#!/usr/bin/ruby -w
require 'squib'

# Config

FILE_NAME = 'Bannerbeasts Roller - Weapons.csv'
MAX_CARD_COUNT = 153

MM_TOTAL_CARD_WIDTH = 88.8
MM_TOTAL_CARD_HEIGHT = 63.5 

SCALE = 2 # 1-> 300DPI, 2-> 600DPI
DPI = 300
DPMM = DPI * SCALE / 25.4

PAD = 0 #Tabletop
#PAD = 36 * SCALE #Printer
DOUBLE_PAD = 2*PAD

CARD_WIDTH = MM_TOTAL_CARD_WIDTH * DPMM
CARD_HEIGHT = MM_TOTAL_CARD_HEIGHT * DPMM

FULL_CARD_WIDTH = CARD_WIDTH + DOUBLE_PAD
FULL_CARD_HEIGHT = CARD_HEIGHT + DOUBLE_PAD

FACTION_TEXT = 6 * SCALE
UNIT_TEXT = 14 * SCALE
BAR_TEXT = 8 * SCALE
NUMBER_TEXT = 6 * SCALE
POWERS_TEXT = 10 * SCALE

STROKE = 8 * SCALE

COMMON = '#C14B00';
UNCOMMON = '#737373';
RARE = '#0019FF';

BAR_BOX_H = CARD_WIDTH / 10

FACTION_TEXT_PAD = 10 * SCALE
UNIT_TEXT_PAD = 25 * SCALE
TITLE_H = BAR_BOX_H
FACTION_TEXT_X = PAD + FACTION_TEXT_PAD
UNIT_TEXT_X = PAD + UNIT_TEXT_PAD

BODY_H = CARD_HEIGHT - (TITLE_H + STROKE + STROKE + PAD + PAD)
IMAGE_BLOCK = BODY_H / 3 
IMAGE_SIZE = IMAGE_BLOCK * 0.8
IMAGE_PAD = (IMAGE_BLOCK - IMAGE_SIZE) / 2
IMAGE_X = PAD + STROKE + IMAGE_PAD
IMAGE_Y = TITLE_H + STROKE + IMAGE_SIZE + IMAGE_PAD

POWERS_X = PAD + STROKE + IMAGE_BLOCK
POWERS_Y = PAD + STROKE + TITLE_H
POWERS_W = CARD_WIDTH - (IMAGE_BLOCK + STROKE + STROKE)
POWERS_H = BODY_H

SPELL_X = IMAGE_X
SPELL_Y = IMAGE_Y + IMAGE_BLOCK
SPELL_W = IMAGE_SIZE
SPELL_H = IMAGE_BLOCK

COST_SIZE = TITLE_H * 0.8
COST_PAD = TITLE_H * 0.1
COST_X_PAD = FULL_CARD_WIDTH - (PAD + COST_PAD + COST_SIZE)
COST_Y_PAD = PAD + COST_PAD

# Main
puts "Start";

data = Squib.csv file: FILE_NAME

factions = ['Gobbo', 'Ratkin', 'Silk Court', 'Boneborn', 'Redspines', 'Coppersand']

# Map each faction to its respective id prefix
faction_ids = {
  'Gobbo'      => '01',
  'Ratkin'     => '02',
  'Boneborn'   => '03',
  'Silk Court' => '04',
  'Redspines'  => '05',
  'Coppersand' => '06'
}

# Initialize a new hash of arrays for expanded data
expanded_data = Hash.new { |hash, key| hash[key] = [] }

# Iterate through each row in the DataFrame
data.nrows.times do |i|
  row = data.row(i)  # Get the current row as a hash

  # Determine the factions this row should apply to
  target_factions = row['Faction'] == 'Any' ? factions : [row['Faction']]

  # Duplicate the row for each applicable faction
  target_factions.each do |fact|
    data.columns.each do |col|
      if col == 'Id' && row['Faction'] == 'Any'
        # Prepend faction prefix ONLY if the faction was "Any"
        expanded_data[col] << "#{faction_ids[fact]}-#{row[col]}"
      else
        expanded_data[col] << row[col]
      end
    end
    # Add new 'Fact' column with the current faction
    expanded_data['Fact'] << fact
  end
end

unsorted_data = Squib::DataFrame.new(expanded_data)

sorted_rows = unsorted_data.nrows.times.map { |i| unsorted_data.row(i) }
                                 .sort_by { |row| row['Id'].to_s }  # Ensure sorting treats all Ids as strings

# Initialize a new hash for sorted data
sorted_data = Hash.new { |hash, key| hash[key] = [] }

# Rebuild the DataFrame with sorted rows
sorted_rows.each do |row|
  unsorted_data.columns.each { |col| sorted_data[col] << row[col] }
end

# Convert back to Squib::DataFrame
full_data = Squib::DataFrame.new(sorted_data)

names = full_data['Name'].zip(full_data['Tier']).map { |a| a.join(' - L') }
powers = full_data['Power'].map {|t| t != nil ? t : ''}
casting = full_data['Casting Value'].map {|t| t != nil ? t : ''}

back_images = full_data['Fact'].map{ |t| t != nil ? './assets/weapons/backs/' + t.to_s + '-weapon-backs.png' : '' }

Squib::Deck.new(cards: MAX_CARD_COUNT, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT) do
  background color: 'white'

  line x1: 0, x2: FULL_CARD_WIDTH, y1: PAD + TITLE_H, y2: PAD + TITLE_H, stroke_width: STROKE, stroke_color: 'black'

  text str: full_data['Fact'], color: 'black', x: FACTION_TEXT_X, y: PAD, height: TITLE_H / 3, align: 'left', valign: 'top', font_size: FACTION_TEXT, font: 'Atkinson Hyperlegible Bold'   
  text str: names, color: 'black', x: UNIT_TEXT_X, y: PAD, height: TITLE_H, align: 'left', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold'   

  text str: powers, color: 'black', x: POWERS_X, y: POWERS_Y, width: POWERS_W, height: POWERS_H, align: 'left', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible', markup: true

  png file: full_data['Image'], x: IMAGE_X, y: IMAGE_Y, width: IMAGE_SIZE, height: IMAGE_SIZE 

  text str: casting, color: 'black', x: SPELL_X, y: SPELL_Y, width: SPELL_W, height: SPELL_H, align: 'center', valign: 'top', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold', markup: true

  circle x: COST_X_PAD + COST_SIZE/2, y: COST_Y_PAD + COST_SIZE/2, radius: COST_SIZE * 0.5, stroke_width: STROKE, stroke_color: 'black', fill_color: 'white'
  text str: full_data['Cost'], x: COST_X_PAD, y: COST_Y_PAD, height: COST_SIZE, width: COST_SIZE, color: 'black', align: 'center', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible'   

  save_png dir: '_weapons', prefix: full_data['Id'], count_format: ''
  save_sheet dir: '_sprues_tt', prefix: 'weapons_', rows:5, columns: 4
  rect x: 0, y: 0, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT, stroke_width: SCALE, stroke_color: 'black'
  save_sheet dir: '_sprues_print', prefix: 'weapons_', rows:3, columns: 3
end

Squib::Deck.new(cards: MAX_CARD_COUNT, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT) do
  background color: 'white'

  png file: back_images, x: PAD, y: PAD, width: CARD_WIDTH, height: CARD_HEIGHT

  save_sheet dir: '_sprues_tt', prefix: 'weapons_', suffix: '_backs', rows:5, columns: 4
  rect x: 0, y: 0, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT, stroke_width: SCALE, stroke_color: 'black'
  save_sheet dir: '_sprues_print', prefix: 'weapons_', suffix: '_backs', rows:3, columns: 3, rtl: true
end

puts "Done";