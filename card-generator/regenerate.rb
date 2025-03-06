#!/usr/bin/ruby -w
require 'fileutils'

# Define the folders to clear
folders = ["_backs", "_cards", "_sprues_print", "_sprues_tt", "_weapons"]

# Delete contents of each folder (but not the folders themselves)
folders.each do |folder|
  if Dir.exist?(folder)
    puts "Clearing contents of #{folder}..."
    Dir.foreach(folder) do |file|
      next if file == '.' || file == '..'  # Skip special directory entries
      path = File.join(folder, file)
      FileUtils.rm_rf(path)  # Remove files and directories inside
    end
  else
    puts "Folder #{folder} does not exist, skipping..."
  end
end

# List of Ruby scripts to execute
ruby_scripts = ["backmaker.rb", "cardmaker.rb", "weaponmaker.rb"]

ruby_scripts.each do |script|
  if File.exist?(script)
    puts "Running #{script}..."
    system("ruby", script)  # Executes the Ruby script
  else
    puts "Script #{script} not found, skipping..."
  end
end

puts "Process completed."