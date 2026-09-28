import os

out_path = "app/src/main/assets/questions/deck/oic_nw_c2.csv"
os.makedirs(os.path.dirname(out_path), exist_ok=True)

# Write C2 header
header = "No.,Question,A,B,C,D,Correct Answer\n"

# We will write all 574 rows to oic_nw_c2.csv
print("Building oic_nw_c2.csv script...")
