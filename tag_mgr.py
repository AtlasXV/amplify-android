import os
tags = []
for tag in tags:
    if "patch" in tag:
        print(tag)
        continue
    os.system(f"git push origin --delete {tag}")
    
