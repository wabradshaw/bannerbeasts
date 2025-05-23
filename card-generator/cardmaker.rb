#!/usr/bin/ruby -w
require 'squib'

# Config

FILE_NAME = 'Bannerbeasts Roller - Units.csv'
MAX_CARD_COUNT = 160

MM_TOTAL_CARD_WIDTH = 125 
MM_TOTAL_CARD_HEIGHT = 78

SCALE = 1 # 1-> 300DPI, 2-> 600DPI
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
SMALL_BAR_TEXT = 9 * SCALE
POWERS_TEXT = 10 * SCALE
WEAPON_TEXT = 8 * SCALE

RADIUS = 80 * SCALE
STROKE = 8 * SCALE
HALF_STROKE = STROKE / 2
HP_STROKE = STROKE / 3

COMMON = '#C14B00';
UNCOMMON = '#737373';
RARE = '#3467FF';

BAR_BOX_W = CARD_HEIGHT / 6
BAR_BOX_H = CARD_HEIGHT / 7
BAR_END_X = PAD + BAR_BOX_W + STROKE

ICON_SIZE = BAR_BOX_W * 0.45
ICON_X_PAD = PAD + STROKE
ICON_Y_PAD = PAD + ((BAR_BOX_H - ICON_SIZE) / 2)

FACTION_ICON_SIZE = BAR_BOX_W * 0.8
FACTION_ICON_X_PAD = ICON_X_PAD + (BAR_BOX_W * 0.1)
FACTION_ICON_Y_PAD = ICON_Y_PAD - ((FACTION_ICON_SIZE - ICON_SIZE) / 2)

HEALTH_LINE_W = BAR_BOX_W
UNIT_COUNT_WIDTH = HEALTH_LINE_W / 2
HEALTH_BAR_PAD = PAD + UNIT_COUNT_WIDTH

ONE_BAR_PAD = FULL_CARD_WIDTH - (PAD + HEALTH_LINE_W)

FACTION_TEXT_PAD = 20 * SCALE
UNIT_TEXT_PAD = 50 * SCALE
TITLE_BAR_H = BAR_BOX_W
FACTION_TEXT_X = BAR_END_X + FACTION_TEXT_PAD
UNIT_TEXT_X = BAR_END_X + UNIT_TEXT_PAD

WEAPON_SIZE = 48 * SCALE
WEAPON_COUNT = 7
WEAPONS_X = ONE_BAR_PAD - (STROKE + (WEAPON_COUNT * WEAPON_SIZE))
WEAPONS_Y = PAD + TITLE_BAR_H - (WEAPON_SIZE + STROKE)

COST_SIZE = FACTION_ICON_SIZE * 0.6
COST_RADIUS = COST_SIZE / 2
COST_PAD = COST_SIZE * 0.5
COST_X = BAR_BOX_W + STROKE + COST_PAD
COST_Y = TITLE_BAR_H - COST_RADIUS 
EQUIP_X = WEAPONS_X - COST_SIZE

POWERS_PAD = FACTION_TEXT_PAD
POWERS_X = BAR_END_X + STROKE + POWERS_PAD
POWERS_Y = PAD + TITLE_BAR_H + STROKE
POWERS_W = ONE_BAR_PAD - (POWERS_X + POWERS_PAD)
POWERS_H = CARD_HEIGHT - (POWERS_Y + STROKE / 2)

# Main
puts "Start";

data = Squib.csv file: FILE_NAME

t1_range = data['Tier'].each_with_index.select{ |t, i| t == 1}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}
t3_range = data['Tier'].each_with_index.select{ |t, i| t == 3}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}
equip_range = data['Equips'].each_with_index.select{ |t, i| t != nil}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}

factions = data['Faction Icon'].map {|t| t != nil && t != '.png' ? './assets/factions/' + t.to_s : ''}
summaries = data['Faction'].zip(data['Class']).map { |a| a.join(' - ') }

simple_attacks_range = data['Attacks'].each_with_index.select{ |t, i| t.to_s.length <= 2}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}
complex_attacks_range = data['Attacks'].each_with_index.select{ |t, i| t.to_s.length > 2}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}

melees = data['Melee Hit'].map {|t| t != nil && t != '-' ? t.to_s + '+' : '-'}
ranges = data['Ranged Hit'].map {|t| t != nil && t != '-' ? t.to_s + '+' : '-'}
blocks = data['Block'].map {|t| t != nil && t != '-' ? t.to_s + '+' : '-'}

spells = data['Spells'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/spell.png' : ''}
spears = data['Spears'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/spear.png' : ''}
axes = data['Axes'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/axe.png' : ''}
clubs = data['Clubs'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/blunt.png' : ''}
offhand = data['Offhand'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/offhand.png' : ''}
shields = data['Shields'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/shield.png' : ''}
ranged = data['Ranged'].map {|t| t != nil ? './assets/weapons/' + t.to_s + '/ranged.png' : ''}

powers = data['Powers'].map {|t| t != nil ? t : ''}

Squib::Deck.new(cards: MAX_CARD_COUNT, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT) do
  background color: 'white'

  text str: summaries, color: 'black', x: FACTION_TEXT_X, y: PAD, height: TITLE_BAR_H / 3, align: 'left', valign: 'top', font_size: FACTION_TEXT, font: 'Atkinson Hyperlegible Bold'   
  text str: data['Name'], color: 'black', x: UNIT_TEXT_X, y: PAD, height: TITLE_BAR_H, align: 'left', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold'   

  png file: spells, x: WEAPONS_X + (0 * WEAPON_SIZE), y: WEAPONS_Y - WEAPON_SIZE, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: spears, x: WEAPONS_X + (1 * WEAPON_SIZE), y: WEAPONS_Y - WEAPON_SIZE, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: axes, x: WEAPONS_X + (2 * WEAPON_SIZE), y: WEAPONS_Y - WEAPON_SIZE, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: clubs, x: WEAPONS_X + (3 * WEAPON_SIZE), y: WEAPONS_Y - WEAPON_SIZE, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: offhand, x: WEAPONS_X + (4 * WEAPON_SIZE), y: WEAPONS_Y - WEAPON_SIZE, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: shields, x: WEAPONS_X + (5 * WEAPON_SIZE), y: WEAPONS_Y - WEAPON_SIZE, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: ranged, x: WEAPONS_X + (6 * WEAPON_SIZE), y: WEAPONS_Y - WEAPON_SIZE, width: WEAPON_SIZE, height: WEAPON_SIZE

  text str: data['Spells'], x: WEAPONS_X + (0 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE, align: 'center', valign: 'middle', font_size: WEAPON_TEXT, font: 'Atkinson Hyperlegible'  
  text str: data['Spears'], x: WEAPONS_X + (1 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE, align: 'center', valign: 'middle', font_size: WEAPON_TEXT, font: 'Atkinson Hyperlegible'  
  text str: data['Axes'], x: WEAPONS_X + (2 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE, align: 'center', valign: 'middle', font_size: WEAPON_TEXT, font: 'Atkinson Hyperlegible'  
  text str: data['Clubs'], x: WEAPONS_X + (3 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE, align: 'center', valign: 'middle', font_size: WEAPON_TEXT, font: 'Atkinson Hyperlegible'  
  text str: data['Offhand'], x: WEAPONS_X + (4 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE, align: 'center', valign: 'middle', font_size: WEAPON_TEXT, font: 'Atkinson Hyperlegible'  
  text str: data['Shields'], x: WEAPONS_X + (5 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE, align: 'center', valign: 'middle', font_size: WEAPON_TEXT, font: 'Atkinson Hyperlegible'  
  text str: data['Ranged'], x: WEAPONS_X + (6 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE, align: 'center', valign: 'middle', font_size: WEAPON_TEXT, font: 'Atkinson Hyperlegible'  

  text str: powers, color: 'black', x: POWERS_X, y: POWERS_Y, width: POWERS_W, height: POWERS_H, align: 'left', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible', markup: true

  png file: factions, x: FACTION_ICON_X_PAD, y: FACTION_ICON_Y_PAD, width: FACTION_ICON_SIZE, height: FACTION_ICON_SIZE
  
  png file: './assets/black/move.png', x: ICON_X_PAD, y: ICON_Y_PAD + (1 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/heart.png', x: ICON_X_PAD, y: ICON_Y_PAD + (2 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/attacks.png', x: ICON_X_PAD, y: ICON_Y_PAD + (3 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/melee.png', x: ICON_X_PAD, y: ICON_Y_PAD + (4 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/ranged.png', x: ICON_X_PAD, y: ICON_Y_PAD + (5 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/block.png', x: ICON_X_PAD, y: ICON_Y_PAD + (6 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE

  text str: data['Movement'], color: data['Move Colour'], x: ICON_X_PAD + ICON_SIZE, y: ICON_Y_PAD + (1 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['HP'], color: data['HP Colour'], x: ICON_X_PAD + ICON_SIZE, y: ICON_Y_PAD + (2 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Attacks'], range: simple_attacks_range, color: data['Attacks Colour'], x: ICON_X_PAD + ICON_SIZE, y: ICON_Y_PAD + (3 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Attacks'], range: complex_attacks_range, color: data['Attacks Colour'], x: ICON_X_PAD + ICON_SIZE, y: ICON_Y_PAD + (3 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: SMALL_BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: melees, color: data['Melee Colour'], x: ICON_X_PAD + ICON_SIZE, y: ICON_Y_PAD + (4 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: ranges, color: data['Ranged Colour'], x: ICON_X_PAD + ICON_SIZE, y: ICON_Y_PAD + (5 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: blocks, color: data['Block Colour'], x: ICON_X_PAD + ICON_SIZE, y: ICON_Y_PAD + (6 * BAR_BOX_H), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'    

  data['Number'][0...MAX_CARD_COUNT].each_with_index do |number, index|
    unit_height = FULL_CARD_HEIGHT / number
    hp = data['HP'][index] || 1
    hp_height = unit_height / hp

    number.times do |i|
      
      y_position = i * unit_height
      text_width = hp > 1 ? UNIT_COUNT_WIDTH : HEALTH_LINE_W

      strength = (number - i) * 100 / number
      if strength <= 25
        colour = 'red'
      elsif strength <= 50
        colour = 'yellow'
      else
        colour = 'green'
      end

      bar_start = FULL_CARD_WIDTH - (PAD + HEALTH_LINE_W)
      rect x: bar_start, width: HEALTH_LINE_W + PAD, y: y_position, height: unit_height, stroke_width: HALF_STROKE, stroke_color: 'black', fill_color: colour, range: index

      text str: (number - i).to_s, color: 'black', x: bar_start, y: y_position, width: text_width, height: unit_height, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold', range: index

      if hp > 1
        hp.times do |j|          
          hp_y_position = y_position + (j * hp_height)
          if j > 0
            line x1: bar_start + UNIT_COUNT_WIDTH, x2: bar_start + HEALTH_LINE_W, y1: hp_y_position, y2: hp_y_position, stroke_width: HP_STROKE, stroke_color: 'black', range: index, dash: HP_STROKE.to_s + " " + HP_STROKE.to_s
          end
          text str: (hp - j).to_s, color: 'black', x: bar_start + HEALTH_BAR_PAD, y: hp_y_position, width: text_width, height: hp_height, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible', range: index
        end
      end
    end
  end

  line x1: BAR_END_X, x2: BAR_END_X, y1: 0, y2: FULL_CARD_HEIGHT, stroke_width: STROKE, stroke_color: 'black'
  line x1: ONE_BAR_PAD, x2: ONE_BAR_PAD, y1: 0, y2: FULL_CARD_HEIGHT, stroke_width: STROKE, stroke_color: 'black'
  line x1: BAR_END_X, x2: ONE_BAR_PAD, y1: TITLE_BAR_H, y2: TITLE_BAR_H, stroke_width: STROKE, stroke_color: 'black'
  
  circle range: t1_range, x: COST_X + COST_RADIUS, y: COST_Y + COST_RADIUS, radius: COST_RADIUS, stroke_width: STROKE, stroke_color: 'black', fill_color: COMMON  
  text  range: t1_range, str: data['Cost'], color: 'black', x: COST_X, y: COST_Y, width: COST_SIZE, height: COST_SIZE, align: 'center', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible Bold'
  circle range: equip_range, x: EQUIP_X + COST_RADIUS, y: COST_Y + COST_RADIUS, radius: COST_RADIUS, stroke_width: STROKE, stroke_color: 'black', fill_color: RARE
  text  range: equip_range, str: data['Equips'], color: 'black', x: EQUIP_X, y: COST_Y, width: COST_SIZE, height: COST_SIZE, align: 'center', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible Bold'

  save_png dir: '_cards', prefix: data['Unit'], count_format: ''
  save_sheet dir: '_sprues_tt', rows:5, columns: 3 
  rect x: 0, y: 0, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT, stroke_width: SCALE, stroke_color: 'black'
  save_sheet dir: '_sprues_print', rows:2, columns: 2, rotate: true
end

puts "Done";