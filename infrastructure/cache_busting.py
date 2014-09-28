#!/usr/bin/python

from bs4 import BeautifulSoup
import hashlib
from argparse import ArgumentParser

parser = ArgumentParser()
parser.add_argument("index")
parser.add_argument("resources", nargs="+")
opts = parser.parse_args()

soup = BeautifulSoup(open(opts.index).read())
scripts = soup.find_all("script")
styles = soup.find_all("link")


def process_js(resource_name, hash):
    for script in scripts:
        if resource_name in script.get("src", ""):
            break
    else:
        raise Exception("js %s is not found in index!" % resource_name)

    script_src_split = script["src"].split("?hash=")

    if len(script_src_split) == 1:
        script_src_split.append(hash)
    elif not script_src_split[-1].endswith(hash):
        script_src_split[-1] = hash

    script["src"] = "?hash=".join(script_src_split)


def process_css(resouce_name, hash):
    for style in styles:
        if resource_name in style["href"]:
            break
    else:
        raise Exception("css %s is not found in index!" % resource_name)

    style_href_split = style["href"].split("?hash=")

    if len(style_href_split) == 1:
        style_href_split.append(hash)
    elif not style_href_split[-1].endswith(hash):
        style_href_split[-1] = hash

    style["href"] = "?hash=".join(style_href_split)


for resource in opts.resources:
    hash = hashlib.sha224(open(resource).read()).hexdigest()
    # assuming all the files have different names
    resource_name = resource.split("/")[-1]

    if resource_name.endswith(".js"):
        process_js(resource_name, hash)
    elif resource_name.endswith(".css"):
        process_css(resource_name, hash)


html = soup.prettify(soup.original_encoding)
open(opts.index, "w+").write(html)
