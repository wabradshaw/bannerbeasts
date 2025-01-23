#!/usr/bin/ruby -w
require 'squib'

# Config

FILE_NAME = 'Bannerbeasts Roller - Units.csv'
MAX_CARD_COUNT = 29

MM_TOTAL_CARD_WIDTH = 125 
MM_TOTAL_CARD_HEIGHT = 78

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

FACTION_TEXT = 11 * SCALE
UNIT_TEXT = 18 * SCALE
TYPE_TEXT = 16 * SCALE
BAR_TEXT = 12 * SCALE
POWERS_TEXT = 10 * SCALE

RADIUS = 80 * SCALE
STROKE = 8 * SCALE

COMMON = '#C14B00';
UNCOMMON = '#737373';
RARE = '#0019FF';

BAR_BOX_W = CARD_WIDTH / 9
BAR_BOX_H = BAR_BOX_W
BAR_Y = FULL_CARD_HEIGHT - (PAD + BAR_BOX_H)

PORTRAIT_HEIGHT = CARD_HEIGHT - BAR_BOX_H
PORTRAIT_WIDTH = 4 * BAR_BOX_W

FACTION_ICON_SIZE = BAR_BOX_H
FACTION_ICON_ROUNDER = (FACTION_ICON_SIZE * 2) + PAD

ICON_SIZE = BAR_BOX_H / 2
ICON_X_PAD = PAD + ((BAR_BOX_W - ICON_SIZE) / 2)

FACTION_TEXT_PAD = 20 * SCALE
UNIT_TEXT_PAD = 50 * SCALE
TITLE_H = BAR_BOX_H
FACTION_TEXT_X = PAD + PORTRAIT_WIDTH + FACTION_TEXT_PAD
UNIT_TEXT_X = PAD + PORTRAIT_WIDTH + UNIT_TEXT_PAD

WEAPON_SIZE = 32 * SCALE
WEAPON_COUNT = 7
WEAPONS_X = FULL_CARD_WIDTH - (PAD + (WEAPON_COUNT * WEAPON_SIZE))
WEAPONS_Y = PAD + TITLE_H - (WEAPON_SIZE + STROKE)

POWERS_X = PAD + PORTRAIT_WIDTH + STROKE
POWERS_Y = PAD + STROKE + TITLE_H
POWERS_W = CARD_WIDTH - POWERS_X
POWERS_H = BAR_Y - POWERS_Y

COST_SIZE = BAR_BOX_H * 0.5
HALF_COST = COST_SIZE / 2
COST_GAP = (TITLE_H/2) - (WEAPON_SIZE + HALF_COST)
COST_X = FULL_CARD_WIDTH - (PAD + COST_GAP + COST_SIZE)
COST_Y = PAD + COST_GAP

# Main
puts "Start";

data = Squib.csv file: FILE_NAME

t1_range = data['Tier'].each_with_index.select{ |t, i| t == 1}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}
t2_range = data['Tier'].each_with_index.select{ |t, i| t == 2}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}

factions = data['Faction Icon'].map {|t| t != nil && t != '.png' ? './assets/factions/' + t.to_s : ''}

melees = data['Melee Hit'].map {|t| t != nil && t != '-' ? t.to_s + '+' : '-'}
ranges = data['Ranged Hit'].map {|t| t != nil && t != '-' ? t.to_s + '+' : '-'}
blocks = data['Block'].map {|t| t != nil && t != '-' ? t.to_s + '+' : '-'}
evasions = data['Evasion'].map {|t| t != nil && t != '-' ? t.to_s + '+' : '-'}

spears = data['Spears'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/spear.png' : ''}
axes = data['Axes'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/axe.png' : ''}
clubs = data['Clubs'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/club.png' : ''}
offhand = data['Offhand'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/offhand.png' : ''}
shields = data['Shields'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/shield.png' : ''}
ranged = data['Ranged'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/ranged.png' : ''}

powers = data['Powers'].map {|t| t != nil ? t : ''}

Squib::Deck.new(cards: MAX_CARD_COUNT, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT) do
  background color: 'white'

  line x1: PAD + PORTRAIT_WIDTH, x2: PAD + PORTRAIT_WIDTH, y1: 0, y2: BAR_Y, stroke_width: STROKE, stroke_color: 'black'
  line x1: 0, x2: FULL_CARD_WIDTH, y1: BAR_Y, y2: BAR_Y, stroke_width: STROKE, stroke_color: 'black'
  line x1: PAD + PORTRAIT_WIDTH, x2: FULL_CARD_WIDTH, y1: PAD + TITLE_H, y2:  PAD + TITLE_H, stroke_width: STROKE, stroke_color: 'black'

  png file: factions, x: PAD, y: PAD, width: FACTION_ICON_SIZE, height: FACTION_ICON_SIZE

  faction_text_arrays = text str: data['Faction'], color: 'black', x: FACTION_TEXT_X, y: PAD, height: TITLE_H / 3, align: 'left', valign: 'top', font_size: FACTION_TEXT, font: 'Atkinson Hyperlegible Bold'   
  unit_text_arrays = text str: data['Name'], color: 'black', x: UNIT_TEXT_X, y: PAD, height: TITLE_H, align: 'left', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold'   

  png file: spears, x: WEAPONS_X + (0 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: axes, x: WEAPONS_X + (1 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: clubs, x: WEAPONS_X + (2 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: offhand, x: WEAPONS_X + (3 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: shields, x: WEAPONS_X + (4 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: ranged, x: WEAPONS_X + (5 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE

  circle range: t1_range, x: COST_X + COST_SIZE/2, y: COST_Y + COST_SIZE/2, radius: COST_SIZE/2, stroke_width: STROKE, stroke_color: 'black', fill_color: COMMON
  circle range: t2_range, x: COST_X + COST_SIZE/2, y: COST_Y + COST_SIZE/2, radius: COST_SIZE/2, stroke_width: STROKE, stroke_color: 'black', fill_color: UNCOMMON
  text str: data['C1'], color: 'black', x: COST_X, y: COST_Y, width: COST_SIZE, height: COST_SIZE, align: 'center', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible Bold'

  text str: powers, color: 'black', x: POWERS_X, y: POWERS_Y, width: POWERS_W, height: POWERS_H, align: 'left', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible', markup: true

  png file: './assets/coloured/move.png', x: ICON_X_PAD + (0 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/count.png', x: ICON_X_PAD + (1 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/rank.png', x: ICON_X_PAD + (2 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/attacks.png', x: ICON_X_PAD + (3 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/melee.png', x: ICON_X_PAD + (4 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/ranged.png', x: ICON_X_PAD + (5 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/heart.png', x: ICON_X_PAD + (6 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/block.png', x: ICON_X_PAD + (7 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/dodge.png', x: ICON_X_PAD + (8 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE

  text str: data['Movement'], color: 'black', x: ICON_X_PAD + (0 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['N1'], color: 'black', x: ICON_X_PAD + (1 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Front Line'], color: 'black', x: ICON_X_PAD + (2 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Attacks'], color: 'black', x: ICON_X_PAD + (3 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: melees, color: 'black', x: ICON_X_PAD + (4 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: ranges, color: 'black', x: ICON_X_PAD + (5 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['HP'], color: 'black', x: ICON_X_PAD + (6 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: blocks, color: 'black', x: ICON_X_PAD + (7 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: evasions, color: 'black', x: ICON_X_PAD + (8 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  

  save_png dir: '_cards', prefix: data['Unit'], count_format: '', suffix: 1
  save_sheet dir: '_sprues', rows:6, cols: 5, suffix: 1 
end

Squib::Deck.new(cards: MAX_CARD_COUNT, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT) do
  background color: 'white'

  line x1: PAD + PORTRAIT_WIDTH, x2: PAD + PORTRAIT_WIDTH, y1: 0, y2: BAR_Y, stroke_width: STROKE, stroke_color: 'black'
  line x1: 0, x2: FULL_CARD_WIDTH, y1: BAR_Y, y2: BAR_Y, stroke_width: STROKE, stroke_color: 'black'
  line x1: PAD + PORTRAIT_WIDTH, x2: FULL_CARD_WIDTH, y1: PAD + TITLE_H, y2:  PAD + TITLE_H, stroke_width: STROKE, stroke_color: 'black'

  png file: factions, x: PAD, y: PAD, width: FACTION_ICON_SIZE, height: FACTION_ICON_SIZE

  faction_text_arrays = text str: data['Faction'], color: 'black', x: FACTION_TEXT_X, y: PAD, height: TITLE_H / 3, align: 'left', valign: 'top', font_size: FACTION_TEXT, font: 'Atkinson Hyperlegible Bold'   
  unit_text_arrays = text str: data['Name'], color: 'black', x: UNIT_TEXT_X, y: PAD, height: TITLE_H, align: 'left', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold'   

  png file: spears, x: WEAPONS_X + (0 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: axes, x: WEAPONS_X + (1 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: clubs, x: WEAPONS_X + (2 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: offhand, x: WEAPONS_X + (3 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: shields, x: WEAPONS_X + (4 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: ranged, x: WEAPONS_X + (5 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE

  circle range: t1_range, x: COST_X + COST_SIZE/2, y: COST_Y + COST_SIZE/2, radius: COST_SIZE/2, stroke_width: STROKE, stroke_color: 'black', fill_color: COMMON
  circle range: t2_range, x: COST_X + COST_SIZE/2, y: COST_Y + COST_SIZE/2, radius: COST_SIZE/2, stroke_width: STROKE, stroke_color: 'black', fill_color: UNCOMMON
  text str: data['C2'], color: 'black', x: COST_X, y: COST_Y, width: COST_SIZE, height: COST_SIZE, align: 'center', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible Bold'

  text str: powers, color: 'black', x: POWERS_X, y: POWERS_Y, width: POWERS_W, height: POWERS_H, align: 'left', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible', markup: true

  png file: './assets/coloured/move.png', x: ICON_X_PAD + (0 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/count.png', x: ICON_X_PAD + (1 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/rank.png', x: ICON_X_PAD + (2 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/attacks.png', x: ICON_X_PAD + (3 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/melee.png', x: ICON_X_PAD + (4 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/ranged.png', x: ICON_X_PAD + (5 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/heart.png', x: ICON_X_PAD + (6 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/block.png', x: ICON_X_PAD + (7 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/coloured/dodge.png', x: ICON_X_PAD + (8 * BAR_BOX_W), y: BAR_Y + STROKE, width: ICON_SIZE, height: ICON_SIZE

  text str: data['Movement'], color: 'black', x: ICON_X_PAD + (0 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['N2'], color: 'black', x: ICON_X_PAD + (1 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Front Line'], color: 'black', x: ICON_X_PAD + (2 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Attacks'], color: 'black', x: ICON_X_PAD + (3 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: melees, color: 'black', x: ICON_X_PAD + (4 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: ranges, color: 'black', x: ICON_X_PAD + (5 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['HP'], color: 'black', x: ICON_X_PAD + (6 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: blocks, color: 'black', x: ICON_X_PAD + (7 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: evasions, color: 'black', x: ICON_X_PAD + (8 * BAR_BOX_W), y: BAR_Y + ICON_SIZE, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  

  save_png dir: '_cards', prefix: data['Unit'], count_format: '', suffix: 2 
  save_sheet dir: '_sprues', rows:6, cols: 5, suffix: 2
end

puts "Done";