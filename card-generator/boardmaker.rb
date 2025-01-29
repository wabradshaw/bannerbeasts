#!/usr/bin/ruby -w
require 'squib'

# Config

MAX_CARD_COUNT = 28

MM_TOTAL_CARD_WIDTH = 125 
MM_TOTAL_CARD_HEIGHT = 78

SCALE = 0.5 # 1-> 300DPI, 2-> 600DPI
DPI = 300
DPMM = DPI * SCALE / 25.4

CARD_WIDTH = MM_TOTAL_CARD_WIDTH * DPMM

PAGE_BORDER = CARD_WIDTH * 0.333 
STROKE = 24 * SCALE
PADDED_CELL = STROKE + CARD_WIDTH

BOARD_WIDTH_CELLS = 6
BOARD_HEIGHT_CELLS = 6
BOARD_WIDTH = BOARD_WIDTH_CELLS * PADDED_CELL + STROKE
BOARD_HEIGHT = BOARD_HEIGHT_CELLS * PADDED_CELL + STROKE

TILE_COLOUR = '#72d176'

# Main
puts "Start";
Squib::Deck.new(cards: 1, width: BOARD_WIDTH + PAGE_BORDER + PAGE_BORDER, height: BOARD_HEIGHT + PAGE_BORDER + PAGE_BORDER) do
  background color: 'white'

  rect x: PAGE_BORDER, width: BOARD_WIDTH, y: PAGE_BORDER, height: BOARD_HEIGHT, stroke_width: STROKE, stroke_color: 'black', fill_color: TILE_COLOUR

  (BOARD_WIDTH_CELLS - 1).times do |i|     
     position = PAGE_BORDER + ((i + 1) * PADDED_CELL)
     line x1: position, x2: position, y1: PAGE_BORDER, y2: PAGE_BORDER + BOARD_HEIGHT, stroke_width: STROKE, stroke_color: 'black'
  end

  (BOARD_HEIGHT_CELLS - 1).times do |i|     
    position = PAGE_BORDER + ((i + 1) * PADDED_CELL)
    line x1: PAGE_BORDER, x2: PAGE_BORDER + BOARD_WIDTH, y1: position, y2: position, stroke_width: STROKE, stroke_color: 'black'
 end

  save_png dir: '_boards', prefix: BOARD_WIDTH_CELLS.to_s + 'x' + BOARD_HEIGHT_CELLS.to_s, count_format: ''
end


puts "Done";