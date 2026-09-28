import csv
import os

rows_p1 = [
(1,"If there are small cumulus clouds in the morning in summer, what kind of cloud cover would you expect and is the reasonable to forecast later in the day?","CB cloud.","Haze","Clear skies.","ST and drizzle","A"),
(2,"What do you call a gyroscope error that affects the heading system due to the rotation of the earth?","Apparent drift","System error","Heading error","Transport error","A"),
(3,"In what area on the earth surface does the compass decreases its accuracy?","75 degree latitude","85 degree latitude","60 degree latitude","80 degree latitude","A"),
(4,"What is your idea of a Cold Front?","It is the line of discontinuity at the surface or a upper level along which an advancing cold air mass is undercutting a warmer air mass.","It is the area of a depression occupied by cold air at the surface and comprises the whole depression after the occluding process has been completed","It is a period of low temperatures below the average for the season and locality which last 24 hours or longer particularly during the cold season","It is the the sudden line of demarcation separating the Gulf Stream from the Labrador current.","A"),
(5,"How would you categorize the courses and the bearings which are laid down on the chart?","All the courses and all the bearings which are laid down down on the chart are true.","All the courses and all the bearings which are laid down down on the chart are corrected for compass error.","All the courses and all the bearings which are laid down down on the chart are corrected for deviation.","All the causes and all the bearings which are laid down down on the chart are relatively corrected.","A"),
(6,"Base on what you know, would you explain how the jet stream affect weather conditions around the world?","The strong wind current of the jet stream pushes weather systems around the world","The narrow meandering current of high speed winds near the tropopause redirect weather system around the world","The strong wind current of the jet stream prevents weather systems from moving around the world","The weak wind current of the jet stream redirects weather systems around the world","A"),
(7,"At 1200H, you fix your position and change course to 315T. At 1225H, you again fix your position, and its 0.9 mile southwest of your DR position. Find the drift:","2.1 knots","2.3 knots","1.9 knots","1.7 knots","A"),
(8,"Polaris always appears due north in the sky to a precision better than how many degree?","one degree","quarter of a degree","tenth of a degree","half degree","A"),
(9,"What is the color of the Starboard hand lateral marks used in IALA Region B?","Red","Green and white vertical stripes","Black with one or more broad horizontal green bands","Green","A"),
(10,"What conclusion can you draw if the moon and sun align with the Earth?","The tides will be stronger because the attraction of the moon and sun is combine","The moon's gravitational pull will be twice as strong as the suns","Their pulls partially cancel each other out, so the tides are weaker","The moon's gravitational attraction has more effect on tides than the sun's","A"),
(11,"Which of the following statements regarding local weather is NOT a considerable importance in the approaches to a harbour and when coasting in tropical waters?","The consideration of fast and quick route while navigating in coastal water and in tropical water","The local weather due to hills and valley account for much of the weather throughout the year.","The radiation from the earth at night.","The daily cycle of weather owing to the cycle to the changing altitude of the sun.","A"),
(12,"What is the error introduced into the satellite signal in the ephemeris data?","Epsilon error","Group time delay error","Clock time error","Ephemeris delay error","A"),
(13,"13 Before a ship is expected to be in sight of a coastline, how would a navigator ensure that the chart for the area is safe for navigation?","The chart for the area have been corrected as to the latest notice to mariners receive onboard.","The chart for the area have been corrected as to the position of all landmarks, lighthouses, buoys etc.","The chart for the area have been the latest edition published under the Admiralty.","The chart for the area is the largest scale chart published under the Admiralty and is safe for navigation.","A"),
(14,"It is the angular distance of a celestial body north or south of the celestial equator measured along the hour circle of the body.","Declination","Zenith","Amplitude","Azimuth","A"),
(15,"What is the deviation of a compass if the Compass heading is 312 and the Magnetic heading is 297?","15W","15E","30W","30E","A"),
(16,"Several types of pressure distribution may be associated with radiation fog, which feature of an all have one in common?","A slack pressure gradients.","Closely spaced isobars","A rapidly falling pressure","A tight pressure gradients","A"),
(17,"You determine your vessel's position by taking a range and bearing to a buoy. What is the term you used for your plotted position?","Estimated position","Estimated reckoned position","Estimated fix position","Estimated running fix","A"),
(18,"When should a navigator rely on the position of floating aids to navigation?","Only when fixed aids are not available","Only when floating aids are available","Only when fixed aids are available","Only when channel buoys are available","A"),
(19,"How would you compare the precision of the vessel's position during a coastal passage?","It is an accurate position fix taken from conspicuous landmark.","It is an accurate position fix taken from two known objects.","It is an accurate position fix taken from coast","It is an accurate running fix taken from conspicuous landmark.","A"),
(20,"What is the variation of a compass if the True heading is 215 and the Magnetic heading is 210?","5E","5W","10E","10W","A"),
]

out_file = "/app/applet/app/src/main/assets/questions/deck/oic_nw_c1.csv"
os.makedirs(os.path.dirname(out_file), exist_ok=True)
with open(out_file, "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(["No.", "Question", "A", "B", "C", "D", "Correct Answer"])
    for r in rows_p1:
        writer.writerow(list(r))

print("Part 1 written")
