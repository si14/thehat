import os
from bs4 import BeautifulSoup
import hashlib

script_path = os.path.abspath(__file__)
project_path = os.path.dirname(os.path.dirname(script_path))
js_path = os.path.join(project_path, "resources", "public", "js", "compiled",
                       "thehat.js")
index_path = os.path.join(project_path, "resources", "public",
                          "index_dev.html")


js_hash = hashlib.sha224(open(js_path).read()).hexdigest()
soup = BeautifulSoup(open(index_path).read())
scripts = soup.find_all("script")
for script in scripts:
    if script["src"].startswith("js/compiled/thehat.js"):
        break
else:
    raise Exception("script is not found in index!")

script_src_split = script["src"].split("?hash=")

if len(script_src_split) == 1:
    script_src_split.append(js_hash)
elif not script_src_split[-1].endswith(js_hash):
    script_src_split[-1] = js_hash

script["src"] = "?hash=".join(script_src_split)
html = soup.prettify(soup.original_encoding)
open(index_path, "w+").write(html)
