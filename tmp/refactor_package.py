import os
import glob

old_pkg = "com.example"
new_pkg = "com.nhyle.hyperionmaritime"

old_dir = "app/src/main/java/com/example"
new_dir = "app/src/main/java/com/nhyle/hyperionmaritime"

# 1. Move main java directory
if os.path.exists(old_dir):
    os.makedirs(new_dir, exist_ok=True)
    # Move contents recursively or use shutil
    import shutil
    for item in os.listdir(old_dir):
        s = os.path.join(old_dir, item)
        d = os.path.join(new_dir, item)
        if os.path.isdir(s):
            if os.path.exists(d):
                shutil.rmtree(d)
            shutil.move(s, d)
        else:
            shutil.move(s, d)
    shutil.rmtree(old_dir)

# Same for test java
old_test_dir = "app/src/test/java/com/example"
new_test_dir = "app/src/test/java/com/nhyle/hyperionmaritime"
if os.path.exists(old_test_dir):
    os.makedirs(new_test_dir, exist_ok=True)
    for item in os.listdir(old_test_dir):
        s = os.path.join(old_test_dir, item)
        d = os.path.join(new_test_dir, item)
        if os.path.isdir(s):
            if os.path.exists(d):
                shutil.rmtree(d)
            shutil.move(s, d)
        else:
            shutil.move(s, d)
    shutil.rmtree(old_test_dir)

# Same for androidTest java
old_androidTest_dir = "app/src/androidTest/java/com/example"
new_androidTest_dir = "app/src/androidTest/java/com/nhyle/hyperionmaritime"
if os.path.exists(old_androidTest_dir):
    os.makedirs(new_androidTest_dir, exist_ok=True)
    for item in os.listdir(old_androidTest_dir):
        s = os.path.join(old_androidTest_dir, item)
        d = os.path.join(new_androidTest_dir, item)
        if os.path.isdir(s):
            if os.path.exists(d):
                shutil.rmtree(d)
            shutil.move(s, d)
        else:
            shutil.move(s, d)
    shutil.rmtree(old_androidTest_dir)

# 2. Replace package and imports in all .kt files
for root, dirs, files in os.walk("app"):
    for file in files:
        if file.endswith(".kt") or file.endswith(".kts") or file.endswith(".xml"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            new_content = content.replace("package com.example", "package com.nhyle.hyperionmaritime")
            new_content = new_content.replace("import com.example.", "import com.nhyle.hyperionmaritime.")
            new_content = new_content.replace("com.example.MainActivity", "com.nhyle.hyperionmaritime.MainActivity")
            
            if new_content != content:
                with open(path, "w", encoding="utf-8") as f:
                f.write(new_content)
                print(f"Updated {path}")

print("Refactoring complete!")
