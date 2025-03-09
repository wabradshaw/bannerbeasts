require 'csv'

# Define input CSV file path
csv_file = 'Bannerbeasts Roller - Units.csv'
txt_file = 'output.txt'

data = []

CSV.foreach(csv_file, headers: true, col_sep: ",") do |row|
  powers = row["Powers"].to_s.split(';').map { |p| "<b>#{p.strip}</b>" }.join("\n\n")
  
  entry = "#{row["Faction"]} - #{row["Name"]} - L#{row["Tier"]}"
  entry += " C:#{row["Cost"]}" if row["Tier"].to_i == 1
  entry += " - #{row["Class"]}\n"
  entry += "N#{row["Number"]} S#{row["Movement"]} H#{row["HP"]} A#{row["Attacks"]} M#{row["Melee Hit"]} R#{row["Ranged Hit"]} B#{row["Block"]}\n"
  entry += "#{powers}\n"
  entry += "-------\n"
  
  data << entry
end

# Write to TXT file
File.open(txt_file, 'w') do |f|
  f.write(data.join("\n"))
end

puts "Text output saved to #{txt_file}"
