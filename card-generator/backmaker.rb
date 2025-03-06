#!/usr/bin/ruby -w
require 'squib'

# Config

FILE_NAME = 'Bannerbeasts Roller - Units.csv'
MAX_CARD_COUNT = 90

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

FACTION_TEXT = 16 * SCALE
UNIT_TEXT = 28 * SCALE
STAT_TEXT = 24 * SCALE

SIDEBAR_W = CARD_WIDTH / 4
BAR_W = 2 * SIDEBAR_W / 7
SINGLE_BAR_SPACER_W = (SIDEBAR_W - BAR_W) / 2 

FACTION_ICON_SIZE = BAR_W * 2.5
FACTION_ICON_MARGIN = (SIDEBAR_W - FACTION_ICON_SIZE) / 2
FACTION_ICON_Y = PAD + (CARD_HEIGHT - FACTION_ICON_SIZE) / 2
FACTION_ICON_LEFT = PAD + FACTION_ICON_MARGIN
FACTION_ICON_RIGHT = FULL_CARD_WIDTH - (PAD + FACTION_ICON_MARGIN + FACTION_ICON_SIZE)

CENTRAL_W = CARD_WIDTH - (SIDEBAR_W * 2)
CENTRAL_X = PAD + SIDEBAR_W

TITLE_BAR_H = CARD_HEIGHT / 3
TITLE_BAR_Y = CARD_HEIGHT / 6
UNIT_TEXT_X = PAD + SIDEBAR_W

CONTENT_BOX_SIZE = CENTRAL_W / 3
CONTENT_BAR_Y = TITLE_BAR_Y + TITLE_BAR_H

ICON_SPACER = (CONTENT_BOX_SIZE / 2)
ICON_MARGIN = ICON_SPACER * 0.1
ICON_SIZE = ICON_SPACER - (ICON_MARGIN * 2)
ICON_X_PAD = (CONTENT_BOX_SIZE - ICON_SIZE) / 2

FIRST_STAT_BOX_X = CENTRAL_X + (0 * CONTENT_BOX_SIZE)
SECOND_STAT_BOX_X = CENTRAL_X + (1 * CONTENT_BOX_SIZE)
THIRD_STAT_BOX_X = CENTRAL_X + (2 * CONTENT_BOX_SIZE)

# Main
puts "Start";

data = Squib.csv file: FILE_NAME

t1_range = data['Tier'].each_with_index.select{ |t, i| t == 1}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}
t2_range = data['Tier'].each_with_index.select{ |t, i| t == 2}.map {|t, i| i}.select{|i| i < MAX_CARD_COUNT}

factions = data['Faction Icon'].map {|t| t != nil && t != '.png' ? './assets/factions/' + t.to_s : ''}

Squib::Deck.new(cards: MAX_CARD_COUNT, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT) do
  background range: t1_range, color: data['L1 Colour']
  background range: t2_range, color: data['L2 Colour']

  rect x: PAD + SINGLE_BAR_SPACER_W, y: 0, width: BAR_W, height: FULL_CARD_HEIGHT, stroke_width: 0, fill_color: data['L2 Colour'], range: t1_range
  rect x: PAD + SINGLE_BAR_SPACER_W, y: 0, width: BAR_W, height: FULL_CARD_HEIGHT, stroke_width: 0, fill_color: data['L1 Colour'], range: t2_range
  rect x: FULL_CARD_WIDTH - (PAD + SINGLE_BAR_SPACER_W + BAR_W), y: 0, width: BAR_W, height: FULL_CARD_HEIGHT, stroke_width: 0, fill_color: data['L2 Colour'], range: t1_range
  rect x: FULL_CARD_WIDTH - (PAD + SINGLE_BAR_SPACER_W + BAR_W), y: 0, width: BAR_W, height: FULL_CARD_HEIGHT, stroke_width: 0, fill_color: data['L1 Colour'], range: t2_range

  text str: data['Faction'], range: t1_range, x: UNIT_TEXT_X, y: TITLE_BAR_Y, width: CENTRAL_W, height: TITLE_BAR_H / 3, align: 'center', valign: 'middle', font_size: FACTION_TEXT, font: 'Atkinson Hyperlegible Bold', color: data['L1 Text Colour']   
  text str: data['Faction'], range: t2_range, x: UNIT_TEXT_X, y: TITLE_BAR_Y, width: CENTRAL_W, height: TITLE_BAR_H / 3, align: 'center', valign: 'middle', font_size: FACTION_TEXT, font: 'Atkinson Hyperlegible Bold', color: data['L2 Text Colour']   
  text str: data['Class'], range: t1_range, x: UNIT_TEXT_X, y: TITLE_BAR_Y, width: CENTRAL_W, height: TITLE_BAR_H, align: 'center', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold', color: data['L1 Text Colour']   
  text str: data['Class'], range: t2_range, x: UNIT_TEXT_X, y: TITLE_BAR_Y, width: CENTRAL_W, height: TITLE_BAR_H, align: 'center', valign: 'middle', font_size: UNIT_TEXT, font: 'Atkinson Hyperlegible Bold', color: data['L2 Text Colour']   

  png file: factions, x: FACTION_ICON_LEFT, y: FACTION_ICON_Y, width: FACTION_ICON_SIZE, height: FACTION_ICON_SIZE
  png file: factions, x: FACTION_ICON_RIGHT, y: FACTION_ICON_Y, width: FACTION_ICON_SIZE, height: FACTION_ICON_SIZE
  
  png file: './assets/black/count.png', range: t1_range, x: FIRST_STAT_BOX_X + ICON_X_PAD + ICON_MARGIN, y: CONTENT_BAR_Y + ICON_MARGIN, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/move.png', range: t1_range, x: SECOND_STAT_BOX_X + ICON_X_PAD + ICON_MARGIN, y: CONTENT_BAR_Y + ICON_MARGIN, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/black/block.png', range: t1_range, x: THIRD_STAT_BOX_X + ICON_X_PAD + ICON_MARGIN, y: CONTENT_BAR_Y + ICON_MARGIN, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/white/count.png', range: t2_range, x: FIRST_STAT_BOX_X + ICON_X_PAD + ICON_MARGIN, y: CONTENT_BAR_Y + ICON_MARGIN, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/white/move.png', range: t2_range, x: SECOND_STAT_BOX_X + ICON_X_PAD + ICON_MARGIN, y: CONTENT_BAR_Y + ICON_MARGIN, width: ICON_SIZE, height: ICON_SIZE
  png file: './assets/white/block.png', range: t2_range, x: THIRD_STAT_BOX_X + ICON_X_PAD + ICON_MARGIN, y: CONTENT_BAR_Y + ICON_MARGIN, width: ICON_SIZE, height: ICON_SIZE
  text str: data['Number'], range: t1_range, color: data['L1 Text Colour'], x: FIRST_STAT_BOX_X + ICON_X_PAD, y: CONTENT_BAR_Y + ICON_SPACER + ICON_MARGIN, width: ICON_SPACER, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: STAT_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Min Speed'], range: t1_range, color: data['L1 Text Colour'], x: SECOND_STAT_BOX_X + ICON_X_PAD, y: CONTENT_BAR_Y + ICON_SPACER + ICON_MARGIN, width: ICON_SPACER, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: STAT_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Base Block'], range: t1_range, color: data['L1 Text Colour'], x: THIRD_STAT_BOX_X + ICON_X_PAD, y: CONTENT_BAR_Y + ICON_SPACER + ICON_MARGIN, width: ICON_SPACER, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: STAT_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Number'], range: t2_range, color: data['L2 Text Colour'], x: FIRST_STAT_BOX_X + ICON_X_PAD, y: CONTENT_BAR_Y + ICON_SPACER + ICON_MARGIN, width: ICON_SPACER, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: STAT_TEXT, font: 'Atkinson Hyperlegible Bold'  
  text str: data['Min Speed'], range: t2_range, color: data['L2 Text Colour'], x: SECOND_STAT_BOX_X + ICON_X_PAD, y: CONTENT_BAR_Y + ICON_SPACER + ICON_MARGIN, width: ICON_SPACER, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: STAT_TEXT, font: 'Atkinson Hyperlegible Bold'    
  text str: data['Base Block'], range: t2_range, color: data['L2 Text Colour'], x: THIRD_STAT_BOX_X + ICON_X_PAD, y: CONTENT_BAR_Y + ICON_SPACER + ICON_MARGIN, width: ICON_SPACER, height: ICON_SIZE, align: 'center', valign: 'middle', font_size: STAT_TEXT, font: 'Atkinson Hyperlegible Bold'  
  
  save_png dir: '_backs', prefix: data['Unit'], count_format: '' 
  save_sheet dir: '_sprues_tt', rows:5, columns: 3, suffix: '_backs' 
  rect x: 0, y: 0, width: FULL_CARD_WIDTH, height: FULL_CARD_HEIGHT, stroke_width: SCALE, stroke_color: 'black'
  save_sheet dir: '_sprues_print', rows:2, columns: 2, suffix: '_backs', rotate: true, rtl: true
end

puts "Done";