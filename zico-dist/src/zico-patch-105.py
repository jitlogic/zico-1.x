__doc__ = """

This script migrates old user database into new format.

"""

import json, os

odata = {}

os.chdir("conf")

with open("users.json", "r") as f:
    idata = json.loads("".join(f.readlines()))
    for d in idata.values():
        if d.get("flags"):
            d["admin"] = True
        else:
            d["admin"] = False
        del d["flags"]
        odata[d["username"]] = d

os.rename("users.json", "users.json.orig")

with open("users.json", "w") as f:
    f.write(json.dumps(odata))

