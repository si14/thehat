# The Hat: let the understanding win

Rules:

1. Open [playthehat.com](http://playthehat.com)
2. Split into two more or less equal teams
3. Define some order between players
4. The first player picks the deck, starts the game and tries to explain the word shown
5. Each correctly explained word gives you a point, each discarded one sets you back a point
6. Switch teams after 30 seconds
7. If time runs out when you've already started to explain, both teams can try to guess
8. Iterate until one team gets 42 points

# POS tagger workflow

```
iconv -f WINDOWS-1251 -t UTF-8 book1.txt > book1u.txt
cat book1u.txt | awk '{ for(i=1; i <= NF; i++) {print $i } }' > book1u.split.txt
cat warandpeace/book1u.split.txt | cmd/tree-tagger-russian > warandpeace/book1u.split.tagged.txt
cat book1u.split.tagged.txt | grep Nc | awk -F'\t' '{print $3}' | sort | uniq -c | sort -n | less
...
cat books.split.tagged.txt | grep Nc | awk -F'\t' '{print $3}' | sort | uniq -c | sort -nr | awk -F' ' '{print $2}' > words_raw.txt
```

see also http://corpus.leeds.ac.uk/mocky/
