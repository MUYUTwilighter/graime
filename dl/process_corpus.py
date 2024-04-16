from pypinyin import lazy_pinyin
from json import dump
from tqdm import tqdm

LIMIT: int = 1000000

def ord_range(string: str) -> tuple[int, int]:
    bottom: int = 2 << 32
    top: int = 0
    for c in string:
        o: int = ord(c)
        bottom = min(o, bottom)
        top = max(o, top)
    return bottom, top


noted = []
with open("resources/large_corpus/2014_corpus.txt", mode='r', encoding='utf-8') as file:
    words: list[str] = file.read().split(' ')
    for word in tqdm(words[:LIMIT]):
        split_word = word.split('/')
        if len(split_word) < 2:
            continue
        word, type = split_word[0], split_word[1]
        if type == 'w':
            continue
        bottom, top = ord_range(word)
        if bottom < 0x4e00 or top > 0x9fa5:
            continue

        pinyin_list = lazy_pinyin(word)
        pinyin = ""
        for syllable in pinyin_list:
            pinyin += syllable + "'"
        pinyin = pinyin[:-1]
        noted.append([word, pinyin])

with open("resources/large_corpus/noted_corpus.txt", mode='w', encoding='utf-8') as file:
    dump(noted, file, ensure_ascii=False)
