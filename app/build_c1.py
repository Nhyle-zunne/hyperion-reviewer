import os
import csv

# Python script to parse the full 835 questions dataset and write oic_nw_c1.csv

c1_lines = [
(1, "If there are small cumulus clouds in the morning in summer, what kind of cloud cover would you expect and is the reasonable to forecast later in the day?", "CB cloud.", "Haze", "Clear skies.", "ST and drizzle", "A"),
(2, "What do you call a gyroscope error that affects the heading system due to the rotation of the earth?", "Apparent drift", "System error", "Heading error", "Transport error", "A"),
(3, "In what area on the earth surface does the compass decreases its accuracy?", "75 degree latitude", "85 degree latitude", "60 degree latitude", "80 degree latitude", "A"),
(4, "What is your idea of a Cold Front?", "It is the line of discontinuity at the surface or a upper level along which an advancing cold air mass is undercutting a warmer air mass.", "It is the area of a depression occupied by cold air at the surface and comprises the whole depression after the occluding process has been completed", "It is a period of low temperatures below the average for the season and locality which last 24 hours or longer particularly during the cold season", "It is the the sudden line of demarcation separating the Gulf Stream from the Labrador current.", "A"),
(5, "How would you categorize the courses and the bearings which are laid down on the chart?", "All the courses and all the bearings which are laid down down on the chart are true.", "All the courses and all the bearings which are laid down down on the chart are corrected for compass error.", "All the courses and all the bearings which are laid down down on the chart are corrected for deviation.", "All the causes and all the bearings which are laid down down on the chart are relatively corrected.", "A"),
(6, "Base on what you know, would you explain how the jet stream affect weather conditions around the world?", "The strong wind current of the jet stream pushes weather systems around the world", "The narrow meandering current of high speed winds near the tropopause redirect weather system around the world", "The strong wind current of the jet stream prevents weather systems from moving around the world", "The weak wind current of the jet stream redirects weather systems around the world", "A"),
(7, "At 1200H, you fix your position and change course to 315T. At 1225H, you again fix your position, and its 0.9 mile southwest of your DR position. Find the drift:", "2.1 knots", "2.3 knots", "1.9 knots", "1.7 knots", "A"),
(8, "Polaris always appears due north in the sky to a precision better than how many degree?", "one degree", "quarter of a degree", "tenth of a degree", "half degree", "A"),
(9, "What is the color of the Starboard hand lateral marks used in IALA Region B?", "Red", "Green and white vertical stripes", "Black with one or more broad horizontal green bands", "Green", "A"),
(10, "What conclusion can you draw if the moon and sun align with the Earth?", "The tides will be stronger because the attraction of the moon and sun is combine", "The moon's gravitational pull will be twice as strong as the suns", "Their pulls partially cancel each other out, so the tides are weaker", "The moon's gravitational attraction has more effect on tides than the sun's", "A"),
]

print("Script template ready")
