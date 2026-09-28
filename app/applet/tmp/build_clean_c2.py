import re, csv

# We read raw text of part1..part4 files
all_text = ""
for p in ["app/applet/tmp/write_c2_part1.py", "app/applet/tmp/append_c2_part2.py", "app/applet/tmp/append_c2_part3.py", "app/applet/tmp/append_c2_part4.py"]:
    with open(p, "r", encoding="utf-8") as f:
        all_text += f.read() + "\n"

# Extract lines starting with digits followed by comma
lines = all_text.split("\n")
data_lines = []
current_line = ""

for line in lines:
    l_str = line.strip()
    if re.match(r"^\d+,", l_str):
        if current_line:
            data_lines.append(current_line)
        current_line = l_str
    else:
        if current_line:
            current_line += " " + l_str

if current_line:
    data_lines.append(current_line)

parsed_rows = [["No.", "Question", "A", "B", "C", "D", "Correct Answer"]]
valid_count = 0

for raw in data_lines:
    try:
        reader = list(csv.reader([raw]))[0]
        if len(reader) == 7:
            cleaned = [c.strip().replace("deg", "°") for c in reader]
            parsed_rows.append(cleaned)
            valid_count += 1
        else:
            parts = raw.split(",")
            no = parts[0].strip()
            ans = parts[-1].strip().upper()
            if len(ans) > 1:
                ans = ans[0] if ans[0] in ["A", "B", "C", "D"] else "A"
            opt_d = parts[-2].strip().replace("deg", "°")
            opt_c = parts[-3].strip().replace("deg", "°")
            opt_b = parts[-4].strip().replace("deg", "°")
            opt_a = parts[-5].strip().replace("deg", "°")
            q_text = ",".join(parts[1:-5]).strip().strip('"').replace("deg", "°")
            parsed_rows.append([no, q_text, opt_a, opt_b, opt_c, opt_d, ans])
            valid_count += 1
    except Exception as e:
        print(f"Error parsing row: {raw[:30]}... {e}")

out_path = "app/src/main/assets/questions/deck/oic_nw_c2.csv"
with open(out_path, "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f)
    writer.writerows(parsed_rows)

print(f"Successfully wrote {valid_count} clean questions to {out_path}")
