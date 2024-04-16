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

## 进度

### 1. 确认用户输入选词流程

- 根据拼音查询可用选词
- 模型给出选词评分
- 提供选词和拆分拼音单字
- 用户选词，反馈该选词行为给模型

### 2. 建立状态转移机制：`SceneTree`

- 场景描述规范：用户/应用描述字段/操作系统&平台/窗口路径名称
- 模型创建机制：root + siblings
- 场景转移机制：old + current，引入时间权重
- 反馈传播机制：root & current

### 3. 建立模型规范接口：`ScoreProducer`

- 确定名称规范：`identifier` + `POST_FIX`
- 确定创建机制：`ScoreProducer.REGISTRY` 或直接实例化实现对象
- 确定基础操作：文件IO（读取、保存、标记）；选词流程（评分、反馈）；模型间操作（融合、权重）

#### 建立词库模型规范接口：`LexiconObtainable`

- 定义词库标准：`{拼音:{选词:评分}}`
- 定义词库接口 `LexiconObtainable`：实现该接口则表示该模型的选词评分机制可以转化为静态的词库并获取

#### 示例模型：时间加权词库模型 `TimeWeightedDictionModel`

- 文件格式：JSON（UTF-8）；
- 词库存储：词库存储对应拼音的可能选词，以及各个选词的可能性评分与最近一次该选词被选中的时间戳，
  其中评分越大表示应该放在靠前的位置；
- 时间加权选词机制：在获取分数时，对分数进行时间加权操作，即对记录的分数乘以时间衰减系数；
  如果上一次该词被选中的时间距离现在很远则该系数越小，反之则越大；
- 时间加权反馈机制：进行反馈时，先对分数进行时间加权操作（避免用户的“神经词”操作对评分造成较大影响），
  然后再乘以一个增益系数，让该词的评分升高。
