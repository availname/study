# HTML 学习笔记

这份文件专门记录 HTML 的详细知识、语法和案例。

## 1. HTML 文档结构

### 作用

HTML 文档结构规定了一个网页文件的基本组成。浏览器按照这套结构读取和显示页面。

### 语法

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>页面标题</title>
</head>
<body>
  页面内容
</body>
</html>
```

### 语法讲解

- `<!DOCTYPE html>`：声明这是 HTML5 文档。
- `<html>`：包住整个 HTML 页面。
- `lang="zh-CN"`：说明页面主要使用简体中文。
- `<head>`：放页面配置，通常不显示在页面主体中。
- `<meta charset="UTF-8" />`：设置字符编码，防止中文乱码。
- `<title>`：设置浏览器标签页显示的标题。
- `<body>`：放页面中真正显示的内容。

### 案例

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>我的网页</title>
</head>
<body>
  <h1>你好，HTML</h1>
</body>
</html>
```

## 2. 标题标签：`h1` 和 `h2`

### 作用

标题标签表示内容的层级和主题。`h1` 是页面最主要的标题，`h2` 是下面的二级主题。

### 语法

```html
<h1>一级标题</h1>
<h2>二级标题</h2>
```

### 语法讲解

- 标题标签的范围是 `h1` 到 `h6`。
- 数字越小，标题层级越高。
- `h1` 通常表示页面主标题，一个页面一般使用一个主要的 `h1`。
- `h2` 可以有多个，用来表示不同的内容区块。
- 标签名称表达的是内容层级，不只是文字大小。

### 案例

```html
<h1>HTML 学习笔记</h1>
<h2>HTML 页面结构</h2>
<h2>HTML 常用标签</h2>
```

## 3. 段落标签：`p`

### 作用

`p` 表示一段文字。浏览器通常会把不同的 `p` 显示为不同的段落。

### 语法

```html
<p>这里是一段文字。</p>
```

### 语法讲解

- 文字内容写在开始标签和结束标签之间。
- 一个 `p` 表示一个完整的文字段落。
- 不要使用很多空格或换行来模拟段落，应该使用多个 `p`。
- `p` 主要表示文字段落，不适合包住整组复杂页面结构。

### 案例

```html
<p>HTML 用标签描述网页结构。</p>
<p>浏览器读取 HTML 后，会把它显示成网页。</p>
```

## 4. 普通容器：`div`

### 作用

`div` 是一个普通的块级容器，用来包住、分组或组织其他内容。

### 语法

```html
<div>
  内容
</div>
```

### 语法讲解

- `div` 本身没有特殊的业务含义。
- 它适合用来做普通分组和页面布局。
- `div` 可以嵌套其他 HTML 标签。
- `div` 的视觉效果通常需要 CSS 才会明显改变。
- 只是为了分组或布局时，使用 `div` 就可以。

### 案例

```html
<div>
  <h2>个人信息</h2>
  <p>姓名：小明</p>
</div>
```

## 5. 内容区域：`section`

### 作用

`section` 表示页面中有明确主题的一块内容区域。

### 语法

```html
<section>
  <h2>区域标题</h2>
  <p>区域内容。</p>
</section>
```

### 语法讲解

- `section` 和 `div` 都可以包住内容。
- 它们在默认显示效果和基本布局功能上没有本质区别。
- `section` 额外表达了“这是一组有主题的内容”。
- 一个 `section` 通常应该有自己的标题。
- 有明确主题时使用 `section`，只是普通容器时使用 `div`。

### 为什么会有 `section`

网页不只是给浏览器显示，也需要让开发者、搜索引擎和辅助阅读工具理解页面结构。`section` 可以说明这一块内容属于哪个主题，减少页面中没有语义的 `div`。

### 案例

```html
<section>
  <h2>学习计划</h2>
  <p>今天学习 HTML 标签。</p>
</section>
```

## 6. 无序列表：`ul`

### 作用

`ul` 表示没有先后顺序的列表。浏览器默认会在列表项前显示项目符号。

### 语法

```html
<ul>
  <li>第一项</li>
  <li>第二项</li>
</ul>
```

### 语法讲解

- `ul` 是列表容器。
- `li` 是列表中的一项。
- `li` 通常放在 `ul` 或 `ol` 里面。
- `ul` 适合表示没有顺序要求的内容，例如兴趣、功能、商品特点。

### 案例

```html
<h2>我的兴趣</h2>
<ul>
  <li>阅读</li>
  <li>编程</li>
  <li>运动</li>
</ul>
```

## 7. 有序列表：`ol`

### 作用

`ol` 表示有顺序的列表。浏览器默认会自动显示数字编号。

### 语法

```html
<ol>
  <li>第一步</li>
  <li>第二步</li>
</ol>
```

### 语法讲解

- `ol` 是有序列表容器。
- `li` 是有序列表中的一项。
- 列表顺序由 HTML 结构表达，不能只靠手写数字。
- `ol` 适合表示操作步骤、排名或有先后关系的内容。

### 案例

```html
<h2>学习步骤</h2>
<ol>
  <li>打开 HTML 文件</li>
  <li>修改标签内容</li>
  <li>保存并刷新页面</li>
</ol>
```

## 8. 列表项目：`li`

### 作用

`li` 表示列表中的一项内容。

### 语法

```html
<ul>
  <li>列表项目</li>
</ul>
```

### 语法讲解

- `li` 一般不能单独表示一个完整列表。
- 它通常作为 `ul` 或 `ol` 的子元素。
- `ul` 和 `ol` 决定列表类型，`li` 提供每一项内容。
- `li` 里面还可以嵌套文字、链接或其他简单内容。

### 案例

```html
<ol>
  <li><a href="https://www.example.com">打开网页</a></li>
  <li>阅读网页内容</li>
</ol>
```

## 9. 链接标签：`a`

### 作用

`a` 表示一个可以跳转的链接，可以跳到其他网页、当前页面的某个位置或下载资源。

### 语法

```html
<a href="https://www.example.com">打开网页</a>
```

### 语法讲解

- `href` 表示跳转地址。
- 标签中间的文字是页面上显示并可以点击的内容。
- 链接地址可以是完整网址，也可以是本地文件路径。
- `target="_blank"` 可以尝试在新标签页打开链接，但外部链接还应注意安全属性。
- 链接适合“跳转到某处”，按钮更适合“执行一个操作”。

### 案例

```html
<a href="https://www.baidu.com">打开百度</a>
<a href="./html.html">打开当前学习文件</a>
```

## 10. 图片标签：`img`

### 作用

`img` 用来在页面中显示图片。

### 语法

```html
<img src="photo.jpg" alt="一张照片" />
```

### 语法讲解

- `src` 是图片地址，可以是网络地址或本地路径。
- `alt` 是图片说明。图片加载失败时，浏览器可能显示它；屏幕阅读器也会读取它。
- `img` 是空元素，没有自己的结束标签。
- 图片文件并不写进标签里面，HTML 只通过 `src` 指向图片。
- 图片能否显示取决于地址是否正确，以及网络或本地文件是否存在。

### 案例

```html
<img
  src="https://placehold.co/320x180?text=HTML+IMG"
  alt="HTML 图片标签示例"
  width="320"
/> 
```

## 11. 按钮标签：`button`

### 作用

`button` 表示一个可以被用户操作的按钮，例如保存、提交、删除或打开。

### 语法

```html
<button type="button">点击我</button>
```

### 语法讲解

- `type="button"`：普通按钮，不提交表单。
- `type="submit"`：提交所在的表单。
- `type="reset"`：重置所在表单的输入内容。
- 按钮本身不会自动执行 JavaScript；需要 JavaScript 事件或 Vue 的 `@click`。
- 按钮放在 `form` 中时，最好明确写 `type`，避免出现意外提交。

### 案例

```html
<button type="button">普通按钮</button>
<button type="submit">提交按钮</button>
```

## 12. 输入标签：`input`

### 作用

`input` 用来接收用户输入或让用户选择数据。

### 语法

```html
<input type="text" name="username" />
```

### 语法讲解

- `type="text"`：普通单行文本。
- `type="password"`：密码输入。
- `type="email"`：邮箱输入。
- `type="number"`：数字输入。
- `type="checkbox"`：复选框。
- `type="radio"`：单选框。
- `type="file"`：选择文件。
- `placeholder`：输入为空时的提示，不是正式数据。
- `name`：提交表单时的字段名。
- `id`：当前页面中的标识，也可以和 `label` 建立关联。
- `required`：要求必须填写。
- `input` 是空元素，不能在开始标签和结束标签之间写文字。

### 案例

```html
<label for="username">姓名：</label>
<input
  id="username"
  name="username"
  type="text"
  placeholder="请输入姓名"
  required
/> 
```

## 13. 标签说明：`label`

### 作用

`label` 用来说明输入控件的用途，并把说明文字和输入控件关联起来。

### 语法

```html
<label for="username">姓名：</label>
<input id="username" type="text" />
```

### 语法讲解

- `for` 的值必须等于目标输入控件的 `id`。
- 点击 label 文字时，对应输入框会获得焦点。
- 单选框和复选框配合 label 使用时，点击文字也可以选择控件。
- `label` 是字段说明，不能完全被 `placeholder` 代替，因为 placeholder 输入后会消失。
- 表单中的输入控件尽量配一个明确的 label。

### 案例

```html
<label for="email">邮箱：</label>
<input id="email" name="email" type="email" />
```

## 14. 表单标签：`form`

### 作用

`form` 把一组相关输入组织起来，并在提交时将数据发送到指定地址。

### 语法

```html
<form action="/login" method="post">
  <!-- 输入控件和提交按钮 -->
</form>
```

### 语法讲解

- `action`：提交地址。
- `method="get"`：数据通常放在地址后面，适合搜索等查询。
- `method="post"`：数据通常放在请求正文中，常用于提交资料。
- 输入控件要有 `name`，才有明确的提交字段名。
- `form` 只负责收集和提交，不负责把数据保存进数据库。
- 表单中的按钮最好明确写 `type="submit"` 或 `type="button"`。

### 案例

```html
<form action="#" method="get">
  <label for="keyword">关键词：</label>
  <input id="keyword" name="keyword" type="text" />
  <button type="submit">搜索</button>
</form>
```

## 15. HTML 属性和 `class`

### 作用

HTML 属性用于给标签提供额外信息。`class` 是其中一种常见属性，用于给元素分类。

### 语法

```html
<标签名 属性名="属性值">内容</标签名>
```

```html
<p class="important">重要内容</p>
```

### 语法讲解

- 属性写在开始标签中，例如 `href`、`src`、`type`。
- `class` 可以给多个标签使用。
- `class` 本身不会改变样式，也不会执行动作。
- CSS 可以通过 `class` 设置样式，JavaScript 可以通过 `class` 找到元素。
- `id` 通常表示一个唯一元素，`class` 可以表示一组元素。

### 案例

```html
<div id="header" class="box">页面标题</div>
<div class="box">页面内容</div>
```

这里两个元素都使用了 `box`，而 `header` 只标记第一个元素。

### `id` 和 `class` 的相同点

- 都是 HTML 属性，写在开始标签中。
- 都可以帮助 CSS 或 JavaScript 找到元素。
- 它们本身不会自动产生样式或动作。

### `id` 和 `class` 的不同点

| 属性 | 主要作用 | 使用数量 |
| --- | --- | --- |
| `id` | 标记一个具体元素 | 页面中通常唯一 |
| `class` | 给一组元素分类 | 可以被多个元素使用 |

```html
<input id="username" class="input-box" />
<input id="email" class="input-box" />
```

这里两个输入框都有 `class="input-box"`，但 `id` 不一样。

### `id` 和 `label` 的联动

`label` 的 `for` 要和输入控件的 `id` 相同：

```html
<label for="username">姓名：</label>
<input id="username" name="username" type="text" />
```

这样点击“姓名”文字时，输入框会获得焦点。这个关联还方便屏幕阅读器理解输入框的名称。

### `select` 和 `id`、`class` 的关系

`select` 是 HTML 标签，`id` 和 `class` 是 HTML 属性，它们不是同一种东西：

```html
<select id="level" class="select-box" name="level">
  <option value="basic">基础</option>
</select>
```

- `select`：创建下拉选择框。
- `id`：唯一标记这个下拉框，也可以和 `label` 关联。
- `class`：给下拉框分类，后面可以统一设置样式。
- `name`：提交表单时使用的字段名。

## 16. 表格：`table`、`caption`、`tr`、`th`、`td`

### 作用

这些标签用来表示有行和列的数据，例如成绩表、商品表和学习进度表。

### 语法

```html
<table border="1">
  <caption>学生成绩</caption>
  <tr>
    <th>姓名</th>
    <th>成绩</th>
  </tr>
  <tr>
    <td>小明</td>
    <td>90</td>
  </tr>
</table>
```

### 语法讲解

- `table`：整张表格。
- `caption`：表格标题。
- `tr`：表格中的一行，英文是 `table row`。
- `th`：表头单元格，英文是 `table header`。
- `td`：普通数据单元格，英文是 `table data`。
- `th` 和 `td` 都写在 `tr` 里面。
- `th` 说明列的含义，`td` 写具体数据。
- `border="1"` 可以暂时显示边框，后面学习 CSS 后再用 CSS 设置边框。

### 为什么会有这些标签

表格标签专门描述行列数据，让浏览器和辅助阅读工具理解表头、行和数据之间的关系。它不应该用来制作整个页面布局。

### 综合案例

表格案例已经写入 [html.html](./html.html)。

## 17. 多行文本框：`textarea`

### 作用

`textarea` 用来输入多行文字，例如留言、备注和反馈。

### 语法

```html
<label for="feedback">学习感受：</label>
<textarea
  id="feedback"
  name="feedback"
  rows="4"
  cols="30"
  placeholder="请输入学习感受"
></textarea>
```

### 语法讲解

- `textarea` 有开始标签和结束标签。
- `rows`：默认显示的行数。
- `cols`：默认显示的列宽。
- `placeholder`：没有输入内容时的提示文字。
- `id`：和 `label` 的 `for` 对应。
- `name`：提交表单时的字段名。
- `maxlength`：最多输入多少个字符。
- `minlength`：至少输入多少个字符。
- `required`：必须填写。
- `readonly`：只读，不能修改。
- `disabled`：禁用，通常不会提交它的值。
- `autofocus`：页面打开后自动获得焦点。
- 和 `input` 不同，`textarea` 适合输入多行内容。
- `textarea` 中间的文字会成为默认内容：

```html
<textarea>默认内容</textarea>
```

常用属性示例：

```html
<textarea
  name="message"
  rows="5"
  cols="40"
  maxlength="200"
  placeholder="请输入留言"
  required
></textarea>
```

### 综合案例

`textarea` 已经加入 [html.html](./html.html) 的“学习反馈”表单中。

## 18. 下拉选择：`select` 和 `option`

### 作用

`select` 创建下拉选择框，`option` 表示下拉框中的一个选项。

### 语法

```html
<label for="level">学习程度：</label>
<select id="level" name="level">
  <option value="beginner">刚开始学习</option>
  <option value="basic">已经学过基础标签</option>
</select>
```

### 语法讲解

- `select`：下拉选择框的容器。
- `option`：下拉列表中的一个选项。
- `value`：选项真正提交给后端的值。
- `option` 中间的文字：用户在页面上看到的文字。
- `id`：和 `label` 的 `for` 关联。
- `name`：表单提交时的字段名。
- `selected`：设置默认选中的选项。
- `disabled`：禁用某个选项，用户不能选择。
- `select` 通常用于选一个固定选项，例如分类、状态和地区。
- 选项很多时，下拉框可以节省页面空间。

### 显示文字和提交值

```html
<option value="basic">已经学过基础标签</option>
```

用户看到的是：

```text
已经学过基础标签
```

表单提交的值是：

```text
basic
```

### 默认选中和禁用选项

```html
<select name="city">
  <option value="">请选择城市</option>
  <option value="beijing" selected>北京</option>
  <option value="shanghai">上海</option>
  <option value="unknown" disabled>暂不可选择</option>
</select>
```

### 综合案例

下拉选择已经加入 [html.html](./html.html) 的“学习反馈”表单中。

## 19. 页面语义结构：`header`、`nav`、`main`、`footer`

### 作用

这 4 个标签用来说明页面不同区域的用途：

```text
header  页面或区域的头部
nav     导航链接区域
main    页面主要内容
footer  页面或区域的底部
```

### 语法

```html
<header>
  <h1>页面标题</h1>
  <nav>
    <a href="#content">主要内容</a>
  </nav>
</header>

<main id="content">
  页面主要内容
</main>

<footer>
  页面底部信息
</footer>
```

### 语法讲解

- `header`：放页面标题、介绍或导航等头部内容。
- `nav`：放页面的主要导航链接。
- `main`：放页面最主要的内容，一个页面通常只有一个 `main`。
- `footer`：放版权、联系方式或页面底部信息。
- 这些标签和 `div` 一样可以包住内容，但它们额外表达了区域的语义。
- 它们默认不会自动产生颜色、边框或布局。

### 为什么会有这些标签

以前页面经常由很多 `div` 组成，浏览器能显示，但不容易看出哪个区域是头部、导航、主体或底部。语义标签让开发者、搜索引擎和辅助阅读工具更容易理解页面结构。

### `id` 和导航跳转

```html
<nav>
  <a href="#progress">学习进度</a>
</nav>

<section id="progress">学习进度内容</section>
```

`href="#progress"` 会跳到 `id="progress"` 的元素。

### 综合案例

这 4 个标签已经加入 [html.html](./html.html)。

## 20. 独立内容和补充内容：`article`、`aside`

### 作用

```text
article  可以独立理解的一篇内容
aside    对主要内容的补充说明
```

### 语法

```html
<article>
  <h2>文章标题</h2>
  <p>文章的主要内容。</p>
</article>

<aside>
  <h2>相关提示</h2>
  <p>这里是对主要内容的补充说明。</p>
</aside>
```

### 语法讲解

- `article`：表示一篇可以脱离当前页面单独理解的内容，例如文章、新闻、评论或帖子。
- `aside`：表示与主要内容有关但不是核心内容的补充信息，例如提示、相关链接或侧边说明。
- 两个标签都可以包含标题、段落、图片和链接。
- 它们默认不会自动显示在页面侧边，也不会自动产生边框或布局。
- 页面怎么排列需要 CSS；标签主要负责说明内容的意义。

### 和 `section` 的区别

- `section`：一组有主题的内容区域。
- `article`：一篇可以独立存在的完整内容。
- `aside`：主要内容之外的补充内容。

### 综合案例

```html
<main>
  <article>
    <h2>HTML 学习文章</h2>
    <p>HTML 使用标签描述网页结构。</p>
  </article>

  <aside>
    <h2>学习提示</h2>
    <p>页面布局要使用 CSS，语义标签主要用来表达结构。</p>
  </aside>
</main>
```

这两个标签已经加入 [html.html](./html.html)。

## 21. 行内容器：`span`

### 作用

`span` 用来单独标记一小段行内内容，方便以后设置样式或绑定操作。

### 语法

```html
<p>我的名字是：<span>小明</span></p>
```

### 语法讲解

- `span` 是行内容器，默认不会独占一行。
- `span` 本身不会自动改变颜色、大小或执行动作。
- 可以给 `span` 添加 `class`，以后用 CSS 设置样式。
- JavaScript 或 Vue 也可以找到并操作 `span`。
- `span` 适合包住句子中的一个词、数字或短内容。

### 和 `div` 的区别

```text
div   块级容器，通常独占一行
span  行内容器，通常不会自动换行
```

### 案例

```html
<p>
  学习状态：<span class="status">已完成</span>
</p>
```

## 22. 重要文字：`strong`

### 作用

`strong` 表示内容很重要，需要用户重点注意。

### 语法

```html
<strong>重要提示</strong>
```

### 语法讲解

- 浏览器默认通常把 `strong` 显示为粗体。
- 它的重点是“内容重要”，不只是让文字变粗。
- 屏幕阅读器可能会用更明显的方式读出重要内容。
- 如果只是想改变粗细，后面可以使用 CSS。

### 案例

```html
<p><strong>提交表单前请检查内容。</strong></p>
```

## 23. 强调文字：`em`

### 作用

`em` 表示需要强调或特别注意的文字。

### 语法

```html
<em>请认真阅读</em>
```

### 语法讲解

- 浏览器默认通常把 `em` 显示为斜体。
- 它的重点是表达语气上的强调，不只是让文字倾斜。
- `em` 可以嵌套，嵌套层级越深表示强调程度更强。
- 如果只是想改变文字样式，后面可以使用 CSS。

### 案例

```html
<p>这里是 <em>特别需要注意</em> 的内容。</p>
```

## 24. 换行标签：`br`

### 作用

`br` 在同一个内容区域中强制换一行。

### 语法

```html
第一行<br />
第二行
```

### 语法讲解

- `br` 是空元素，没有结束标签。
- 它只负责换行，不负责增加段落语义。
- 普通文章分段应该使用多个 `p`，不要大量使用 `br`。
- 地址、诗歌或确实需要固定换行的短内容可以使用 `br`。

### 案例

```html
<p>
  第一行文字<br />
  第二行文字
</p>
```

## 25. 主题分隔线：`hr`

### 作用

`hr` 表示内容之间的主题变化或分隔。

### 语法

```html
<hr />
```

### 语法讲解

- `hr` 是空元素，没有结束标签。
- 浏览器默认通常显示一条横线。
- 它表达的是内容主题发生变化，不只是装饰线。
- 线的颜色、粗细和样式后面可以用 CSS 修改。

### 案例

```html
<p>第一部分内容</p>
<hr />
<p>第二部分内容</p>
```

## 26. 视频标签：`video`

### 作用

`video` 用来在网页中播放视频。

### 语法

```html
<video controls>
  <source src="lesson.mp4" type="video/mp4" />
  浏览器不支持 video 标签。
</video>
```

### 语法讲解

- `video` 是视频播放器容器。
- `controls` 显示播放、暂停和音量等控制按钮。
- `src` 可以直接写视频地址，也可以使用 `source` 提供地址。
- `type` 说明媒体格式，例如 `video/mp4`。
- 中间的文字是浏览器不支持 video 时显示的备用内容。
- 没有有效的视频地址时，标签本身不能播放视频。

### 常用属性

- `controls`：显示控制栏。
- `autoplay`：自动播放，通常会受到浏览器限制。
- `muted`：静音。
- `loop`：循环播放。
- `poster`：视频播放前显示的封面图片。

### 案例

```html
<video controls width="320" poster="cover.jpg">
  <source src="lesson.mp4" type="video/mp4" />
</video>
```

## 27. 音频标签：`audio`

### 作用

`audio` 用来在网页中播放音频，例如音乐、录音和提示音。

### 语法

```html
<audio controls>
  <source src="lesson.mp3" type="audio/mpeg" />
  浏览器不支持 audio 标签。
</audio>
```

### 语法讲解

- `audio` 是音频播放器容器。
- `controls` 显示播放、暂停和音量控制。
- `src` 或 `source` 指向音频文件。
- `type="audio/mpeg"` 表示常见的 MP3 音频格式。
- 没有有效的音频地址时，标签本身不能播放声音。

### 常用属性

- `controls`：显示控制栏。
- `autoplay`：自动播放，通常会受到浏览器限制。
- `muted`：静音。
- `loop`：循环播放。

### 案例

```html
<audio controls loop>
  <source src="lesson.mp3" type="audio/mpeg" />
</audio>
```

## 综合案例

完整综合案例已经写入 [html.html](./html.html)，包含目前学过的：

- HTML 文档结构
- 标题和段落
- `div` 和 `section`
- `ul`、`ol`、`li` 列表
- `a` 链接
- `img` 图片
- `form`、`label`、`input`、`button` 表单
- `header`、`nav`、`main`、`footer`
- `article`、`aside`
- `span`
- `strong`、`em`
- `br`、`hr`
- `video`、`audio`
- HTML 属性和 `class`
