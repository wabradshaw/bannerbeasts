#!/usr/bin/ruby -w
require 'squib'

# Config

FILE_NAME = 'Bannerbeasts Roller - Units.csv'
MAX_CARD_COUNT = 28

MM_TOTAL_CARD_WIDTH = 63.5 
MM_TOTAL_CARD_HEIGHT = 88.8

SCALE = 2 # 1-> 300DPI, 2-> 600DPI
DPI = 300
DPMM = DPI * SCALE / 25.4

#PAD = 0 #Tabletop
PAD = 36 * SCALE #Printer
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

RADIUS = 80 * SCALE
STROKE = 8 * SCALE

COMMON = '#C14B00';
UNCOMMON = '#737373';
RARE = '#0019FF';

BAR_BOX_H = CARD_HEIGHT / 10
BAR_BOX_W = BAR_BOX_H
BAR_X = FULL_CARD_WIDTH - (PAD + BAR_BOX_W)

ICON_SIZE = BAR_BOX_W / 2
ICON_X_PAD = BAR_X + STROKE
ICON_Y_PAD = PAD + ((BAR_BOX_H - ICON_SIZE) / 2)

FACTION_ICON_SIZE = BAR_BOX_H
FACTION_ICON_ROUNDER = (FACTION_ICON_SIZE * 2) + PAD

FACTION_TEXT_PAD = 10 * SCALE
UNIT_TEXT_PAD = 25 * SCALE
TITLE_H = BAR_BOX_H
FACTION_TEXT_X = PAD + FACTION_TEXT_PAD
UNIT_TEXT_X = PAD + UNIT_TEXT_PAD

WEAPON_SIZE = 32 * SCALE
WEAPON_COUNT = 7
WEAPONS_X = BAR_X - (WEAPON_COUNT * WEAPON_SIZE)
WEAPONS_Y = PAD + TITLE_H - (WEAPON_SIZE + STROKE)

POWERS_X = PAD + STROKE
POWERS_Y = PAD + STROKE + TITLE_H
POWERS_W = CARD_WIDTH - (BAR_BOX_W + STROKE + STROKE)
POWERS_H = CARD_HEIGHT - (TITLE_H + STROKE + STROKE)

COST_SIZE = BAR_BOX_H * 0.5
HALF_COST = COST_SIZE / 2
COST_GAP = (TITLE_H/2) - (WEAPON_SIZE + HALF_COST)
COST_X = FULL_CARD_WIDTH - (PAD + COST_GAP + COST_SIZE)
COST_Y = PAD + COST_GAP

# Main
puts "Start";

data = Squib.csv file: FILE_NAME

costs = data['C1'].zip(data['C2']).map { |a| a.join('|') }
numbers = data['N1'].zip(data['N2']).map { |a| a.join('|') }
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

  line x1: BAR_X, x2: BAR_X, y1: 0, y2: FULL_CARD_HEIGHT, stroke_width: STROKE, stroke_color: 'black'
  line x1: 0, x2: BAR_X, y1: PAD + TITLE_H, y2: PAD + TITLE_H, stroke_width: STROKE, stroke_color: 'black'

  text str: data['Faction'], color: 'black', x: FACTION_TEXT_X, y: PAD, height: TITLE_H / 3, align: 'left', valign: 'top', font_size: FACTION_TEXT, font: 'Atkinson Hyperlegible Bold'   
  text str: data['Name'], color: 'black', x: UNIT_TEXT_X, y: PAD, height: TITLE_H, align: 'left', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold'   

  png file: spears, x: WEAPONS_X + (0 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: axes, x: WEAPONS_X + (1 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: clubs, x: WEAPONS_X + (2 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: offhand, x: WEAPONS_X + (3 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: shields, x: WEAPONS_X + (4 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE
  png file: ranged, x: WEAPONS_X + (5 * WEAPON_SIZE), y: WEAPONS_Y, width: WEAPON_SIZE, height: WEAPON_SIZE

  text str: powers, color: 'black', x: POWERS_X, y: POWERS_Y, width: POWERS_W, height: POWERS_H, align: 'left', valign: 'middle', font_size: POWERS_TEXT, font: 'Atkinson Hyperlegible', markup: true

  circle x: ICON_X_PAD + ICON_SIZE/2, y: ICON_Y_PAD + ICON_SIZE/2, radius: ICON_SIZE*0.45, stroke_width: STROKE * 0.5, stroke_color: 'black', fill_color: UNCOMMON
  text str: data['Tier'], color: 'black', x: ICON_X_PAD, y: ICON_Y_PAD, width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: NUMBER_TEXT, font: 'Atkinson Hyperlegible Bold'

  png file: './assets/black/count.png', x: ICON_X_PAD, y: ICON_Y_PAD + (1 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/move.png', x: ICON_X_PAD, y: ICON_Y_PAD + (2 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/rank.png', x: ICON_X_PAD, y: ICON_Y_PAD + (3 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/attacks.png', x: ICON_X_PAD, y: ICON_Y_PAD + (4 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/melee.png', x: ICON_X_PAD, y: ICON_Y_PAD + (5 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/ranged.png', x: ICON_X_PAD, y: ICON_Y_PAD + (6 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/heart.png', x: ICON_X_PAD, y: ICON_Y_PAD + (7 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/block.png', x: ICON_X_PAD, y: ICON_Y_PAD + (8 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/dodge.png', x: ICON_X_PAD, y: ICON_Y_PAD + (9 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE

  text str: costs, color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (0 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: numbers, color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (1 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: NUMBER_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Movement'], color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (2 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Front Line'], color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (3 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Attacks'], color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (4 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: melees, color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (5 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: ranges, color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (6 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['HP'], color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (7 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: blocks, color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (8 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: evasions, color: 'black', x: ICON_X_PAD + ICON_SIZE + STROKE, y: ICON_Y_PAD + (9 * BAR_BOX_W), width: ICON_SIZE, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: BAR_TEXT, font: 'Atkinson Hyperlegible Bold'  

  save_png dir: '_summaries', prefix: data['Unit'], count_format: ''
  save_sheet dir: '_sprues', prefix: 'summaries_', rows:3, columns: 3
end

puts "Done";