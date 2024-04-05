# Context Based Low-grain Pinyin IME

In context of Chinese Input Method Editor (or CN IME, pinyin IME),
due to the multiplexing of pinyin (the pronunciation markers in Chinese)
by Chinese characters, as well as the mainstream keyboard lay out does not support
the four Chinese tones directly and conveniently, the current CN IMEs can only "guess"
what the users are trying to type in.

Currently, popular IME would record users' input and selections, generates a dedicated
lexicon and "guess" personalized. However, for users who use PC often, especially Windows
users, they would easily found out that they prefer different candidate words in different
apps, web pages, etc. To meet the users' input nature, this project is going to create the
IME that can dynamically adapt to where the users' typing in as context via Dynamic Model
Adaptation tech, to provide more accurate words-guessing.

