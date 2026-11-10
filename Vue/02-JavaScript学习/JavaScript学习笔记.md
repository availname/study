# JavaScript 学习笔记

这份文件只记录 JavaScript 的详细知识、语法和案例。

## 知识点文件夹版笔记（已学）

下面的编号和 `JavaScript知识点示例` 中的文件夹一一对应。每个案例都同时给出 HTML 和 JavaScript，完整代码在对应文件夹的 `index.html` 中。

### 01. JavaScript 与 `script`

- 功能点：JavaScript 负责网页的数据处理和交互；`script` 标签让 HTML 页面执行 JavaScript。
- 解决的需求问题：只使用 HTML 时，页面只能展示固定结构，不能根据操作改变内容。
- 语法讲解：把 JavaScript 写在 `<script>...</script>` 中；页面解析到这个标签时会执行里面的代码。
- 案例（HTML）：`<p id="message">等待执行</p>`
- 案例（JavaScript）：`document.querySelector('#message').textContent = '已经执行'`
- 完整示例：[01-JavaScript与script](./JavaScript知识点示例/01-JavaScript与script/index.html)

### 02. 注释与 `console` 输出

- 功能点：注释说明代码；`console.log()`、`console.warn()`、`console.error()` 向浏览器控制台输出信息。
- 解决的需求问题：帮助自己和别人理解代码，也方便查看程序执行到哪里、数据是什么。
- 语法讲解：`//` 后面是单行注释；`console.log(内容)` 中的内容会输出到控制台。
- 案例（HTML）：`<p>按 F12 查看控制台</p>`
- 案例（JavaScript）：`console.log('普通信息')`
- 完整示例：[02-注释与console输出](./JavaScript知识点示例/02-注释与console输出/index.html)

### 03. `const` 与 `let` 变量

- 功能点：变量给数据取名字；`const` 声明不能重新赋值的变量，`let` 声明之后可以更新的变量。
- 解决的需求问题：程序需要保存课程名、点击次数、成绩等数据，并在后面重复使用。
- 语法讲解：`const courseName = 'JavaScript'`；`let count = 0`；更新使用 `count = count + 1`。
- 案例（HTML）：`<button id="study-button">学习一次</button>`
- 案例（JavaScript）：`let count = 0; count = count + 1`
- 完整示例：[03-const与let变量](./JavaScript知识点示例/03-const与let变量/index.html)

### 04. 字符串与数字

- 功能点：字符串保存文字，数字保存可以计算的数值。
- 解决的需求问题：姓名、提示语需要以文字保存；成绩、价格和数量需要以数字保存。
- 语法讲解：字符串写成 `'小明'` 或 `"小明"`；数字直接写成 `86`，不用引号。
- 案例（HTML）：`<p id="output"></p>`
- 案例（JavaScript）：`output.textContent = '成绩：' + 86`
- 完整示例：[04-字符串与数字](./JavaScript知识点示例/04-字符串与数字/index.html)

### 05. 布尔值与比较运算

- 功能点：布尔值只有 `true` 和 `false`；比较运算判断两个值的关系。
- 解决的需求问题：程序需要判断“是否及格”“是否相等”“是否大于某个数”。
- 语法讲解：`score >= 60` 会产生布尔值；常用比较符有 `>`、`<`、`>=`、`<=`、`===`、`!==`。
- 案例（HTML）：`<input id="score" type="number" />`
- 案例（JavaScript）：`const isPassed = Number(score.value) >= 60`
- 完整示例：[05-布尔值与比较运算](./JavaScript知识点示例/05-布尔值与比较运算/index.html)

### 06. `if` 与 `else` 条件

- 功能点：`if` 在条件成立时执行代码，`else` 在条件不成立时执行另一段代码。
- 解决的需求问题：不同数据需要得到不同处理，例如及格显示“及格”，否则显示“不及格”。
- 语法讲解：`if (条件) { 条件为真时执行 } else { 条件为假时执行 }`。
- 案例（HTML）：`<button id="judge-button">判断</button>`
- 案例（JavaScript）：`if (score >= 60) { output.textContent = '及格' } else { output.textContent = '不及格' }`
- 完整示例：[06-if与else条件](./JavaScript知识点示例/06-if与else条件/index.html)

### 07. 算术运算与变量更新

- 功能点：使用 `+`、`-`、`*`、`/`、`%` 计算，并把新结果重新保存到变量。
- 解决的需求问题：计算商品总价、数量变化、平均值和余数。
- 语法讲解：`const total = price * count`；`count = count + 1` 会读取旧值、加一、再存回去。
- 案例（HTML）：`<button id="buy-button">购买一件商品</button>`
- 案例（JavaScript）：`count = count + 1; const total = price * count`
- 完整示例：[07-算术运算与变量更新](./JavaScript知识点示例/07-算术运算与变量更新/index.html)

### 08. 对象与属性

- 功能点：对象把同一事物的多项数据组合在一起；属性是对象中的每一项数据。
- 解决的需求问题：一个学生同时有姓名和成绩，分散变量不便管理。
- 语法讲解：对象使用 `{}`；属性写成 `属性名: 值`；使用 `student.name` 读取属性。
- 案例（HTML）：`<button id="show-button">读取学生对象</button>`
- 案例（JavaScript）：`const student = { name: '小明', score: 86 }; output.textContent = student.name`
- 完整示例：[08-对象与属性](./JavaScript知识点示例/08-对象与属性/index.html)

### 09. `for` 循环

- 功能点：按照指定次数重复执行一段代码。
- 解决的需求问题：需要重复五次、遍历固定下标时，不必复制五份相同代码。
- 语法讲解：`for (初始化; 继续条件; 每次更新) { 重复执行的代码 }`。
- 案例（HTML）：`<ul id="output"></ul>`
- 案例（JavaScript）：`for (let index = 1; index <= 5; index = index + 1) { ... }`
- 完整示例：[09-for循环](./JavaScript知识点示例/09-for循环/index.html)

### 10. `while` 与 `do...while` 循环

- 功能点：条件为真时持续循环；`do...while` 会先执行一次再判断条件。
- 解决的需求问题：不知道确切循环次数，只知道停止条件时使用。
- 语法讲解：`while (条件) { ... }`；`do { ... } while (条件)`。
- 案例（HTML）：`<pre id="output"></pre>`
- 案例（JavaScript）：`while (count <= 3) { count = count + 1 }`
- 完整示例：[10-while与do-while循环](./JavaScript知识点示例/10-while与do-while循环/index.html)

### 11. `for...of`、`break` 与 `continue`

- 功能点：`for...of` 依次读取数组元素；`break` 结束整个循环；`continue` 跳过本次循环。
- 解决的需求问题：按顺序处理列表，并在找到目标后停止，或跳过不需要的数据。
- 语法讲解：`for (const item of items) { ... }`；遇到 `break` 直接离开，遇到 `continue` 进入下一次。
- 案例（HTML）：`<button id="run-button">查找香蕉</button>`
- 案例（JavaScript）：`for (const fruit of fruits) { if (fruit === '香蕉') break }`
- 完整示例：[11-for-of与break-continue](./JavaScript知识点示例/11-for-of与break-continue/index.html)

### 12. 函数、参数与 `return`

- 功能点：函数给一段可重复执行的代码取名字；参数接收外部数据；`return` 返回结果并结束函数。
- 解决的需求问题：相同的成绩判断可能使用很多次，不应该重复编写。
- 语法讲解：`function getLevel(score) { return '及格' }`；调用使用 `getLevel(86)`。
- 案例（HTML）：`<input id="score" type="number" />`
- 案例（JavaScript）：`function getLevel(score) { if (score >= 60) { return '及格' } return '不及格' }`
- 完整示例：[12-函数参数与return](./JavaScript知识点示例/12-函数参数与return/index.html)

### 13. 箭头函数与作用域

- 功能点：箭头函数是函数的另一种写法；作用域决定变量可以在哪些位置使用。
- 解决的需求问题：事件回调等短函数可以更简洁；局部变量不会污染函数外部。
- 语法讲解：`const add = (a, b) => { return a + b }`；函数内部声明的变量通常只能在函数内部使用。
- 案例（HTML）：`<button id="run-button">运行箭头函数</button>`
- 案例（JavaScript）：`const createMessage = (name) => { const lesson = 'DOM'; return name + lesson }`
- 完整示例：[13-箭头函数与作用域](./JavaScript知识点示例/13-箭头函数与作用域/index.html)

### 14. 数组下标与 `length`

- 功能点：数组按顺序保存多个数据；下标读取指定位置；`length` 获取元素数量。
- 解决的需求问题：管理水果、学生、成绩等一组同类数据。
- 语法讲解：`const fruits = ['苹果', '香蕉']`；第一个元素是 `fruits[0]`；数量是 `fruits.length`。
- 案例（HTML）：`<p id="output"></p>`
- 案例（JavaScript）：`output.textContent = fruits[0] + '，数量：' + fruits.length`
- 完整示例：[14-数组下标与length](./JavaScript知识点示例/14-数组下标与length/index.html)

### 15. 数组修改与 `push()`、`pop()`、`includes()`

- 功能点：修改已有元素，向末尾添加、删除末尾元素，以及检查数组是否包含某个值。
- 解决的需求问题：列表内容会随着用户操作发生变化，并且需要判断某项是否存在。
- 语法讲解：`items[0] = '新值'`；`push()` 添加；`pop()` 删除并返回末项；`includes()` 返回布尔值。
- 案例（HTML）：`<button id="add-button">添加香蕉</button>`
- 案例（JavaScript）：`fruits.push('香蕉'); const exists = fruits.includes('香蕉')`
- 完整示例：[15-数组修改与push-pop-includes](./JavaScript知识点示例/15-数组修改与push-pop-includes/index.html)

### 16. `forEach()`、`map()`、`filter()`、`find()`

- 功能点：依次处理、转换、筛选和查找数组元素。
- 解决的需求问题：对整组数据执行操作时，不必手动管理循环下标。
- 语法讲解：`forEach()` 不创建新数组；`map()` 返回转换后的数组；`filter()` 返回符合条件的数组；`find()` 返回第一个符合条件的元素。
- 案例（HTML）：`<button id="run-button">处理成绩</button>`
- 案例（JavaScript）：`const passed = scores.filter((score) => score >= 60)`
- 完整示例：[16-forEach-map-filter-find](./JavaScript知识点示例/16-forEach-map-filter-find/index.html)

### 17. 错误与 `console` 调试

- 功能点：利用控制台查看变量、执行过程、警告和错误。
- 解决的需求问题：页面结果不正确时，需要知道程序在哪一步出现问题。
- 语法讲解：`console.log()` 输出普通数据；`console.error()` 输出错误信息，但调用它本身不会停止代码。
- 案例（HTML）：`<button id="debug-button">输出调试信息</button>`
- 案例（JavaScript）：`console.log('当前成绩：', score); console.error('错误信息演示')`
- 完整示例：[17-错误与console调试](./JavaScript知识点示例/17-错误与console调试/index.html)

### 18. `try...catch` 与 `throw`

- 功能点：`try` 尝试执行可能出错的代码；`catch` 接住错误；`throw` 主动产生错误。
- 解决的需求问题：输入或数据不合法时，程序可以显示友好提示，而不是直接中断。
- 语法讲解：`try { ... } catch (error) { ... }`；`throw new Error('原因')` 主动抛出错误。
- 案例（HTML）：`<input id="score" type="number" />`
- 案例（JavaScript）：`if (score > 100) throw new Error('成绩无效')`
- 完整示例：[18-try-catch与throw](./JavaScript知识点示例/18-try-catch与throw/index.html)

### 19. DOM 与 `document`

- 功能点：DOM 把 HTML 页面表示成 JavaScript 可以操作的对象结构；`document` 代表当前整个网页文档。
- 解决的需求问题：JavaScript 需要找到页面标签，才能读取或修改网页内容。
- 语法讲解：从 `document` 开始调用查询方法，例如 `document.querySelector('#message')`。
- 案例（HTML）：`<p id="message">原始文字</p>`
- 案例（JavaScript）：`document.querySelector('#message').textContent = '新文字'`
- 关键结论：`document` 由浏览器提供，表示当前网页；修改 DOM 只改变浏览器当前显示的页面，不会改写硬盘中的 HTML 文件，刷新后会恢复原始 HTML。
- 完整示例：[19-DOM与document](./JavaScript知识点示例/19-DOM与document/index.html)

### 20. DOM 单元素查询

- 功能点：`getElementById()` 按 id 查找元素；`querySelector()` 按 CSS 选择器查找第一个匹配元素。
- 解决的需求问题：先准确找到一个 HTML 标签，后续才能对它绑定事件或修改内容。
- 语法讲解：`document.getElementById('message')` 的参数只写 id 名称，不写 `#`；`document.querySelector('#message')` 接收完整 CSS 选择器，因此 id 前写 `#`，class 前写 `.`，也可以直接写标签名。两种方法找不到元素时都返回 `null`。
- 案例（HTML）：`<p id="first-message" class="message">内容</p>`
- 案例（JavaScript）：`const byId = document.getElementById('first-message'); const byClass = document.querySelector('.message')`
- 完整示例：[20-DOM单元素查询](./JavaScript知识点示例/20-DOM单元素查询/index.html)

### 21. `querySelectorAll()` 与 `NodeList`

- 功能点：查找所有符合选择器的元素，返回一个可以依次处理的 `NodeList` 集合。
- 解决的需求问题：页面有多个相同 class 的元素时，需要一次找到并全部处理。
- 语法讲解：`const students = document.querySelectorAll('.student')` 返回 `NodeList`；`students.length` 获取数量；`students[0]` 获取第一个元素；`students.forEach((student) => { ... })` 逐个处理。没有匹配项时返回空的 `NodeList`，不是 `null`。
- 案例（HTML）：`<p class="student">小明</p><p class="student">小红</p><button id="change-button">修改所有学生</button>`
- 案例（JavaScript）：`const students = document.querySelectorAll('.student'); students.forEach((item, index) => { item.textContent = '第 ' + (index + 1) + ' 个学生' })`
- 关键区别：`querySelector()` 返回一个元素或 `null`；`querySelectorAll()` 返回多个元素组成的 `NodeList`，即使没有找到也返回空集合。
- 说明：`NodeList` 是返回结果的类型，不是页面内容，也不是需要单独调用的函数；实际使用的是它里面的元素、`length`、下标和 `forEach()`。
- 完整示例：[21-querySelectorAll与NodeList](./JavaScript知识点示例/21-querySelectorAll与NodeList/index.html)

### 22. `textContent`、`innerHTML` 与 `innerText`

- 功能点：读取或修改元素内容；`textContent` 按普通文字处理，`innerHTML` 会解析 HTML，`innerText` 关注页面上可见的文字。
- 解决的需求问题：程序既可能显示普通文字，也可能需要创建 HTML 结构，必须选择正确方式。
- 语法讲解：`element.textContent` 读取或写入纯文字；`element.innerHTML` 读取或写入 HTML 字符串；`element.innerText` 读取页面上用户能看到的文字。显示用户输入时优先使用 `textContent`，不要把不可信输入直接交给 `innerHTML`。
- 案例（HTML）：`<div id="target">原始内容</div><button id="text-button">使用 textContent</button><button id="html-button">使用 innerHTML</button>`
- 案例（JavaScript）：`target.textContent = '<strong>普通文字</strong>'; target.innerHTML = '<strong>粗体</strong>'`
- 关键区别：使用 `textContent` 写入 `<strong>文字</strong>` 时，尖括号会作为普通文字显示；使用 `innerHTML` 写入时，浏览器会把 `strong` 解析成 HTML 标签；`innerText` 主要用于读取当前可见文字。
- 完整示例：[22-textContent-innerHTML-innerText](./JavaScript知识点示例/22-textContent-innerHTML-innerText/index.html)

### 23. 表单元素的 `value`

- 功能点：读取或修改 `input`、`textarea`、`select` 等表单元素当前的值。
- 解决的需求问题：获取用户刚刚输入的姓名、成绩或意见，并且可以清空或预填输入框。
- 语法讲解：读取使用 `const value = input.value`；修改使用 `input.value = '新内容'`。`value` 是属性，不是方法，所以后面不写括号。
- 案例（HTML）：`<input id="student-name" type="text" value="小明" /><button id="read-button">读取 value</button>`
- 案例（JavaScript）：`const input = document.querySelector('#student-name'); const currentValue = input.value`
- 关键区别：HTML 中的 `value="小明"` 是初始属性；用户输入或 JavaScript 修改后，`input.value` 表示当前值。`input.getAttribute('value')` 读取的仍然是 HTML 初始属性值。
- 完整示例：[23-表单元素value](./JavaScript知识点示例/23-表单元素value/index.html)

### 24. `getAttribute()` 与 `setAttribute()`

- 功能点：`getAttribute()` 读取 HTML 属性；`setAttribute()` 修改已有属性或添加新属性。
- 解决的需求问题：页面运行时需要读取或改变链接地址、提示文字、自定义标记等属性。
- 语法讲解：读取写成 `element.getAttribute('属性名')`，只有一个参数；设置写成 `element.setAttribute('属性名', '属性值')`，有两个参数。两个方法后面都有 `()`，因为它们是方法，不是普通属性。
- 案例（HTML）：`<a id="dom-link" href="https://example.com" target="_blank">示例链接</a><button id="change-button">修改 href 和 title</button>`
- 案例（JavaScript）：`const oldHref = link.getAttribute('href'); link.setAttribute('href', 'https://developer.mozilla.org/'); link.setAttribute('title', '打开 MDN 文档')`
- 关键区别：`getAttribute()` 不修改页面，只返回属性值；`setAttribute()` 会修改已有属性或添加不存在的属性。读取不存在的属性时通常得到 `null`。
- 完整示例：[24-getAttribute与setAttribute](./JavaScript知识点示例/24-getAttribute与setAttribute/index.html)

### 25. `data-*` 属性与 `dataset`

- 功能点：`data-*` 在 HTML 标签上保存自定义数据；`dataset` 在 JavaScript 中读取或修改这些数据。
- 解决的需求问题：把学生编号、商品编号、状态等数据直接关联到对应元素。
- 语法讲解：HTML 的 `data-student-id` 在 JavaScript 中写成 `element.dataset.studentId`，短横线名称变为小驼峰；读取用 `dataset.属性名`，修改用 `dataset.属性名 = '新值'`。
- 案例（HTML）：`<button id="student" data-student-id="101" data-student-name="小明">学生</button>`
- 案例（JavaScript）：`const id = student.dataset.studentId; student.dataset.studentName = '小红'`
- 关键区别：`data-*` 是 HTML 中实际写出来的自定义属性；`dataset` 是元素对象上专门访问这些属性的对象。`dataset.studentId` 和 `getAttribute('data-student-id')` 都能读取同一份数据。
- 补充理解：简单场景下，`id` 中也可以包含编号，但 `id` 主要用于唯一找到元素，`data-*` 主要用于保存元素附带的数据；两者有时都能实现，但职责不同。
- 注意：通过 `dataset` 读取出来的值都是字符串，例如 `data-student-id="101"` 得到的是 `'101'`，需要计算时要使用 `Number(student.dataset.studentId)`。
- 完整示例：[25-data属性与dataset](./JavaScript知识点示例/25-data属性与dataset/index.html)

### 26. `classList`

- 功能点：对元素的单个 class 执行添加、删除、切换、检查和替换。
- 解决的需求问题：菜单展开、按钮选中等状态变化时，只改变一个 class，不覆盖元素原有的其他 class。
- 语法讲解：常用方法是 `add()`、`remove()`、`toggle()`、`contains()`、`replace()`；`toggle()` 返回切换后是否存在该 class。
- 案例（HTML）：`<p id="target" class="normal-text">目标元素</p>`
- 案例（JavaScript）：`target.classList.add('active'); const exists = target.classList.contains('active')`
- 关键区别：`className` 适合整体读取或覆盖 class 字符串；`classList` 适合对单个 class 执行添加、读取、替换和删除。两者在简单场景下都能实现类似效果，但职责不同。
- 完整示例：[26-classList](./JavaScript知识点示例/26-classList/index.html)

### 27. 创建、添加和删除元素

- 功能点：`createElement()` 创建元素，`appendChild()` 添加到页面，`remove()` 删除元素。
- 解决的需求问题：列表数量由用户或数据决定，无法提前把所有标签写死在 HTML 中。
- 语法讲解：先创建元素，再设置内容，最后添加到父元素：`const li = document.createElement('li')`、`li.textContent = '内容'`、`list.appendChild(li)`；删除使用 `li.remove()`。
- 案例（HTML）：`<input id="item-input" value="新知识" /><button id="add-button">添加列表项</button><ul id="item-list"><li>已有内容</li></ul>`
- 案例（JavaScript）：`const item = document.createElement('li'); item.textContent = input.value; list.appendChild(item)`
- 完整过程：HTML 先提供空位置或已有父元素；JavaScript 运行时创建新的 `li`，写入文字，把它放入 `ul`；删除时先取得最后一个子元素，再调用 `remove()`。
- 完整示例：[27-创建添加删除元素](./JavaScript知识点示例/27-创建添加删除元素/index.html)

### 28. 父元素、子元素与兄弟元素

- 功能点：从当前元素访问它的父元素、子元素、上一个或下一个兄弟元素。
- 解决的需求问题：操作页面结构时，经常需要从已经找到的元素继续找到附近元素。
- 语法讲解：`current.parentElement` 获取父元素；`parent.children` 获取直接子元素集合；`current.previousElementSibling` 获取上一个兄弟元素；`current.nextElementSibling` 获取下一个兄弟元素。没有父元素或没有对应兄弟元素时，相关结果可能是 `null`。
- 案例（HTML）：`<section id="parent"><p>第一个子元素</p><p id="current">当前元素</p><p>下一个兄弟元素</p></section>`
- 案例（JavaScript）：`const parent = current.parentElement; const count = parent.children.length; const previous = current.previousElementSibling`
- 结构关系：`section` 是三个 `p` 的父元素；三个 `p` 互相是兄弟元素；`current` 不是另外两个 `p` 的父元素。
- 父子通信：普通 DOM 没有专门叫“父子通信”的 API，通常是父元素通过 `parent.querySelector()` 找到子元素并修改它，或者子元素触发事件，事件向父元素冒泡，父元素通过 `event.target` 读取触发事件的子元素。
- 父传子案例（HTML）：`<section id="parent"><p id="child-message">等待父元素修改</p></section><button id="parent-button">父元素修改子元素</button>`
- 父传子案例（JavaScript）：`const childMessage = parent.querySelector('#child-message'); childMessage.textContent = '父元素修改了子元素'`
- 子传父案例（HTML）：`<input id="child-input" value="来自子元素的数据" /><button id="child-button">子元素发送数据</button>`
- 子传父案例（JavaScript）：`parent.addEventListener('click', (event) => { if (event.target.id === 'child-button') { output.textContent = childInput.value } })`
- 关键区别：`event.target` 是最初触发事件的子元素；`event.currentTarget` 是正在执行监听函数的父元素。
- 与 Vue 的区别：原生 JavaScript 需要自己查询 DOM、自己使用 `addEventListener()` 绑定事件、自己修改元素；Vue 通常在模板中写 `@click`，再通过数据变化更新页面。
- 完整示例：[28-父子兄弟元素](./JavaScript知识点示例/28-父子兄弟元素/index.html)

### 29. `addEventListener()` 点击事件

- 功能点：监听元素发生的事件，并在事件发生后执行函数。
- 解决的需求问题：用户点击按钮以后，页面才修改文字、添加列表或计算数据。
- 语法讲解：`element.addEventListener('事件名称', 事件处理函数)`；`addEventListener` 是方法，`'click'` 是浏览器规定的点击事件名称，`() => { ... }` 是事件发生后执行的函数。每点击一次，函数就执行一次。
- 案例（HTML）：`<button id="click-button" type="button">点击我</button><p id="output">等待点击。</p><p id="click-count">点击次数：0</p>`
- 案例（JavaScript）：`button.addEventListener('click', () => { count = count + 1; output.textContent = '按钮被点击了'; clickCount.textContent = '点击次数：' + count })`
- 语法拆解：`button` 是要监听的元素；`.` 表示访问元素对象的方法；`addEventListener` 表示添加事件监听；第一个参数是事件名称；第二个参数是事件发生后执行的函数。
- 与 HTML 内联事件的区别：`<button onclick="...">` 把 JavaScript 写进 HTML；`addEventListener()` 把事件处理代码写在 JavaScript 中，更适合统一管理，也可以给同一个元素添加多个监听函数。
- 完整示例：[29-addEventListener点击事件](./JavaScript知识点示例/29-addEventListener点击事件/index.html)

### 30. 事件对象与 `preventDefault()`

- 功能点：事件对象记录本次事件的信息；`preventDefault()` 阻止浏览器的默认行为。
- 解决的需求问题：需要知道用户点击了哪个元素，或者阻止链接跳转、表单刷新页面。
- 语法讲解：`element.addEventListener('click', (event) => { ... })` 中的 `event` 由浏览器自动传入；`event.target` 是真正触发事件的元素；`event.currentTarget` 是当前正在执行监听函数的元素；`event.preventDefault()` 阻止默认行为。
- 案例（HTML）：`<a id="link" href="https://example.com">默认会跳转的链接</a><p id="output"></p>`
- 案例（JavaScript）：`link.addEventListener('click', (event) => { event.preventDefault(); output.textContent = event.target.id })`
- 默认行为：`<a href="...">` 点击后浏览器默认跳转；`<form>` 提交后浏览器默认刷新或发送请求；调用 `preventDefault()` 后，默认动作被取消，但点击事件本身仍然发生。
- 完整示例：[30-事件对象与preventDefault](./JavaScript知识点示例/30-事件对象与preventDefault/index.html)

### 31. 表单提交与 `FormData`

- 功能点：监听表单的 `submit` 事件，并通过 `FormData` 按字段的 `name` 读取数据。
- 解决的需求问题：一次收集姓名、成绩等多个表单字段，进行校验、展示或发送给后端。
- 语法讲解：`form.addEventListener('submit', (event) => { ... })` 监听提交；`event.preventDefault()` 阻止刷新；`new FormData(form)` 收集表单；`formData.get('name')` 中的字符串必须对应 HTML 的 `name="name"`。
- 案例（HTML）：`<form id="student-form"><input id="student-name" name="name" /><input id="student-score" name="score" /><button type="submit">提交</button></form>`
- 案例（JavaScript）：`const formData = new FormData(form); const name = formData.get('name'); const score = formData.get('score')`
- 关键规则：有 `name` 的表单控件才会按字段被 `FormData` 收集；`id` 主要用于 DOM 查找，`name` 主要用于表单字段命名和数据读取。`FormData.get()` 返回的输入值通常是字符串。
- 完整示例：[31-表单提交与FormData](./JavaScript知识点示例/31-表单提交与FormData/index.html)

### 32. `fetch` 加载 JSON

- 功能点：从服务器或文件地址异步请求数据，并把 JSON 响应转换成 JavaScript 数据。
- 解决的需求问题：学生列表等数据通常来自后端，不能全部写死在 HTML 中。
- 语法讲解：`fetch(url)` 发起请求并返回 Promise；`await fetch('./students.json')` 得到响应对象；`response.ok` 判断 HTTP 是否成功；`await response.json()` 读取响应内容并解析成 JavaScript 数据。
- 案例（HTML）：`<button id="load-button">加载数据</button><p id="status"></p><ul id="student-list"></ul>`
- 案例（JavaScript）：`const response = await fetch('./students.json'); if (!response.ok) throw new Error('请求失败'); const students = await response.json()`
- 数据文件：`students.json` 保存 `[ { "name": "小明", "score": 86 } ]`；HTML 只提供显示位置，数据在运行时请求回来。
- 关键区别：`fetch()` 得到的不是最终学生数组，而是响应对象；必须继续调用 `response.json()` 才能得到 JavaScript 数组。请求失败或 JSON 解析失败时，需要用 `try...catch` 处理。
- 完整示例：[32-fetch加载JSON](./JavaScript知识点示例/32-fetch加载JSON/index.html)

### 33. Promise、`async` 与 `await`

- 功能点：Promise 表示以后才完成的异步任务；`async` 声明异步函数；`await` 等待 Promise 得到结果。
- 解决的需求问题：网络请求、定时任务不会立刻完成，程序需要等待结果又不能卡住整个页面。
- 语法讲解：`new Promise((resolve, reject) => { ... })` 创建异步任务；`resolve(value)` 表示成功；`reject(error)` 表示失败；`async` 写在函数前；`await` 等待 Promise 的结果，只能直接用于 `async` 函数中。
- 案例（HTML）：`<button id="load-button">等待异步结果</button><p id="output"></p>`
- 案例（JavaScript）：`const message = await loadMessage(); output.textContent = message`
- Promise 状态：刚创建时是 pending（等待中），调用 `resolve()` 后是 fulfilled（成功），调用 `reject()` 后是 rejected（失败）。状态一旦完成就不能再次改变。
- `then()` 对照：`loadMessage().then((message) => { output.textContent = message })` 也能处理成功结果；`async/await` 通常更接近同步代码的阅读顺序。
- 错误处理：异步代码应放进 `try...catch`，Promise 失败或 `await` 抛错时由 `catch` 接收。
- 完整示例：[33-Promise-async-await](./JavaScript知识点示例/33-Promise-async-await/index.html)

### 34. `import` 与 `export`

- 功能点：`export` 从一个模块导出变量、函数或 class；`import` 在另一个模块中引入它们。
- 解决的需求问题：代码变多后需要拆分文件，并在多个页面或模块中复用功能。
- 语法讲解：导出使用 `export function createGreeting() {}`；导入使用 `import { createGreeting } from './student-tools.js'`；HTML 的 script 需要 `type="module"`；导入名称必须和命名导出的名称一致。
- 案例（HTML）：`<button id="greet-button">调用导入的函数</button><script type="module">import { createGreeting } from './student-tools.js'</script>`
- 案例（JavaScript）：`export function createGreeting(name) { return '你好，' + name }`
- 文件关系：`student-tools.js` 是导出方，负责提供函数；`index.html` 是导入方，负责使用函数。`./` 表示从当前 HTML 所在文件夹查找模块。
- 关键规则：模块代码使用 `export` 或 `import`；普通 `<script>` 不能直接使用它们；模块通常通过 HTTP 服务器运行，不能依赖直接双击 HTML 的 `file://` 地址。
- 完整示例：[34-import与export](./JavaScript知识点示例/34-import与export/index.html)

### 35. `class`、`constructor` 与 `this`

- 功能点：`class` 定义一类对象的共同结构；`constructor` 初始化实例；`this` 表示当前实例。
- 解决的需求问题：多个学生拥有相同属性和方法，但每个学生保存自己的姓名和成绩。
- 语法讲解：`class Student { ... }` 定义模板；`constructor(name, score)` 在创建时接收数据；`this.name = name` 把参数保存到当前实例；`new Student('小明', 86)` 根据模板创建实例；实例可以调用 `describe()`。
- 案例（HTML）：`<button id="create-button">创建 Student 实例</button><button id="create-two-button">创建两个实例</button><p id="output"></p>`
- 案例（JavaScript）：`class Student { constructor(name) { this.name = name } describe() { return this.name } }; const student = new Student('小明')`
- 关键区别：`Student` 是 class 模板，不是某一个具体学生；`student` 是实例，是根据模板创建出来的具体对象。多个实例共用方法，但属性值互相独立。
- 完整示例：[35-class-constructor-this](./JavaScript知识点示例/35-class-constructor-this/index.html)

> 后续规则：每开始一个新知识点，就在 `JavaScript知识点示例` 下按顺序建立新文件夹，并在本节增加相同格式的笔记。


## 第一课：变量和输出

### 1. JavaScript 是什么

JavaScript 是一种编程语言，主要用来让网页具备数据处理和交互能力。

HTML 负责页面结构，CSS 负责页面样式，JavaScript 负责数据和行为。

```text
HTML        页面有什么
CSS         页面长什么样
JavaScript  页面怎么变化和响应操作
```

### 2. 变量是什么

变量可以理解为一个保存数据的名字。

```javascript
const studentName = '学习者'
let studyCount = 1
```

这里：

- `studentName` 是变量名。
- `'学习者'` 是保存的字符串数据。
- `studyCount` 是变量名。
- `1` 是保存的数字数据。
- `=` 表示把右边的数据交给左边的变量保存。

### 3. `const`

`const` 用来声明不会重新赋值的变量。

```javascript
const courseName = 'JavaScript 基础'
```

声明后不能再次给它赋值：

```javascript
const courseName = 'JavaScript 基础'
courseName = 'HTML 基础'
```

上面的代码会报错，因为 `courseName` 已经使用 `const` 声明。

### 4. `let`

`let` 用来声明后面可能改变的变量。

```javascript
let studyCount = 1
studyCount = 2
```

这里第一次保存 `1`，后来又把它改成了 `2`。

### 5. 字符串和数字

字符串是文字，通常放在引号中：

```javascript
const name = '小明'
const lesson = "JavaScript"
```

数字不需要引号：

```javascript
const age = 18
const score = 90
```

注意区别：

```javascript
const numberA = 10
const textA = '10'
```

- `numberA` 是数字。
- `textA` 是文字。
- 虽然看起来都像 `10`，但它们的数据类型不同。

### 6. `console.log()` 输出

`console.log()` 会把内容输出到浏览器开发者工具的 Console 中。

```javascript
console.log('你好，JavaScript')
console.log(100)
```

也可以输出变量：

```javascript
const studentName = '学习者'
console.log(studentName)
```

还可以把文字和变量连接起来：

```javascript
const studentName = '学习者'
const studyCount = 1

console.log('我的名字是：' + studentName)
console.log('今天学习了 ' + studyCount + ' 个知识点')
```

`+` 在这里表示把文字和变量连接起来。

### 7. JavaScript 在 HTML 中的位置

通常把 JavaScript 写在 `script` 标签中：

```html
<script>
  const message = '你好，JavaScript'
  console.log(message)
</script>
```

`script` 不是 JavaScript 语法，它是 HTML 标签，作用是告诉浏览器这里有 JavaScript 代码。

### 8. 第一课完整案例

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>JavaScript 学习第一课</title>
</head>
<body>
  <h1>JavaScript 学习第一课</h1>

  <script>
    const studentName = '学习者'
    let studyCount = 1

    console.log('我的名字是：' + studentName)
    console.log('今天学习了 ' + studyCount + ' 个知识点')
  </script>
</body>
</html>
```

完整练习文件：[javascript.html](./javascript.html)

## 第二课：布尔值、比较运算和条件判断

### 1. 布尔值

布尔值只有两种：`true` 表示是，`false` 表示不是。

```javascript
const isStudent = true
const hasFinished = false
```

布尔值不是字符串，所以不需要加引号：

```javascript
const resultA = true
const resultB = 'true'
```

- `resultA` 是布尔值。
- `resultB` 是字符串。

### 2. 比较运算

比较运算会得到一个布尔值：

```javascript
const age = 18

console.log(age >= 18) // true
console.log(age < 18) // false
```

常用的比较运算符：

| 运算符 | 含义 | 示例 |
| --- | --- | --- |
| `>` | 大于 | `age > 18` |
| `<` | 小于 | `age < 18` |
| `>=` | 大于或等于 | `age >= 18` |
| `<=` | 小于或等于 | `age <= 18` |
| `===` | 严格等于 | `age === 18` |
| `!==` | 严格不等于 | `age !== 18` |

推荐使用 `===` 判断相等。它会同时比较数据的值和类型：

```javascript
console.log(18 === 18) // true
console.log(18 === '18') // false
```

### 3. `if` 条件判断

`if` 的意思是“如果”。条件为 `true` 时，执行大括号中的代码：

```javascript
const age = 18

if (age >= 18) {
  console.log('已经成年')
}
```

语法结构：

```javascript
if (条件) {
  // 条件为 true 时执行
}
```

### 4. `else` 否则

`else` 的意思是“否则”。当 `if` 条件为 `false` 时，执行 `else` 中的代码：

```javascript
const score = 75

if (score >= 60) {
  console.log('考试及格')
} else {
  console.log('考试不及格')
}
```

### 5. 第二课完整案例

```javascript
const age = 18
const isAdult = age >= 18

console.log('年龄是否达到 18 岁：' + isAdult)

if (isAdult) {
  console.log('已经成年')
} else {
  console.log('还未成年')
}

const score = 75

if (score >= 60) {
  console.log('考试及格')
} else {
  console.log('考试不及格')
}
```

这段代码的执行过程：

1. 保存年龄 `18`。
2. 比较 `age >= 18`，得到 `true`。
3. 因为条件是 `true`，执行“已经成年”。
4. 比较成绩是否大于或等于 `60`，输出考试结果。

### 6. 练习

在 [javascript.html](./javascript.html) 中修改 `age` 和 `score` 的值，刷新页面后观察控制台：

```javascript
const age = 16
const score = 55
```

修改后应该看到“还未成年”和“考试不及格”。

## 第三课：算术运算

### 1. 加法、减法、乘法和除法

JavaScript 使用运算符进行数字计算：

```javascript
const price = 10
const count = 3

console.log(price + count) // 13，加法
console.log(price - count) // 7，减法
console.log(price * count) // 30，乘法
console.log(price / count) // 3.333..., 除法
```

常用算术运算符：

| 运算符 | 含义 | 示例 | 结果 |
| --- | --- | --- | --- |
| `+` | 加法 | `10 + 3` | `13` |
| `-` | 减法 | `10 - 3` | `7` |
| `*` | 乘法 | `10 * 3` | `30` |
| `/` | 除法 | `10 / 3` | `3.333...` |
| `%` | 取余数 | `10 % 3` | `1` |

### 2. 取余运算 `%`

`%` 不表示百分号，而是取除法后的余数：

```javascript
console.log(10 % 3) // 1
console.log(8 % 2) // 0
```

`8 % 2` 的结果是 `0`，说明 8 可以被 2 整除。

### 3. 运算顺序和括号

JavaScript 会先计算乘法和除法，再计算加法和减法。可以使用括号明确计算顺序：

```javascript
console.log(2 + 3 * 4) // 14
console.log((2 + 3) * 4) // 20
```

### 4. 使用 `let` 更新变量

使用 `let` 声明的变量可以重新赋值：

```javascript
let total = 30
total = total + 5

console.log(total) // 35
```

`total = total + 5` 的意思是：先取得旧的 `total`，加上 `5`，再把新结果保存回 `total`。

使用 `const` 声明的变量不能重新赋值：

```javascript
const price = 10
price = 20 // 报错
```

### 5. `+` 连接字符串

当 `+` 两边有字符串时，它还可以把内容连接起来：

```javascript
const name = '小明'
const age = 18

console.log('姓名：' + name)
console.log('年龄：' + age)
```

数字计算时，括号可以避免连接字符串和计算混在一起：

```javascript
const price = 10
const count = 3

console.log('总价：' + price * count) // 总价：30
console.log('计算结果：' + (price + count)) // 计算结果：13
```

### 6. 第三课完整案例

```javascript
const price = 10
const count = 3

console.log('加法：' + (price + count))
console.log('减法：' + (price - count))
console.log('乘法：' + (price * count))
console.log('除法：' + (price / count))
console.log('取余：' + (price % count))

let total = price * count
total = total + 5
console.log('更新后的总数：' + total)
```

### 7. 练习

在 [javascript.html](./javascript.html) 中修改：

```javascript
const price = 20
const count = 4
```

刷新页面，观察加法、乘法和总数的输出结果。然后把 `total = total + 5` 改成 `total = total + 10`，再次观察结果。

## JavaScript 基础语法总路线

下面是后续学习地图。带有“已学”的内容已经讲过，其他内容按照顺序学习。

### 第一部分：基本值和变量

1. **变量**：使用 `const` 和 `let` 保存数据。已学。
2. **字符串**：保存文字，例如 `'学习 JavaScript'`。已学。
3. **数字**：保存整数或小数，例如 `18`、`3.14`。已学。
4. **布尔值**：只有 `true` 和 `false`。已学。
5. **`undefined`**：变量已经存在，但还没有数据。
6. **`null`**：明确表示“这里没有值”。
7. **模板字符串**：使用反引号把变量放入文字中。

```javascript
const name = '小明'
const message = `你好，${name}`
console.log(message)
```

### 第二部分：运算符

1. **算术运算**：`+`、`-`、`*`、`/`、`%`。已学。
2. **赋值运算**：`=`、`+=`、`-=`、`*=`、`/=`。
3. **比较运算**：`>`、`<`、`>=`、`<=`、`===`、`!==`。已学。
4. **逻辑运算**：`&&`（并且）、`||`（或者）、`!`（取反）。
5. **递增和递减**：`++`、`--`。
6. **三元运算**：用一行代码根据条件选择两个结果。

```javascript
const age = 20
const message = age >= 18 ? '成年人' : '未成年人'
console.log(message)
```

### 第三部分：条件判断

1. **`if`**：条件成立时执行代码。已学。
2. **`else`**：条件不成立时执行代码。已学。
3. **`else if`**：依次判断多个条件。
4. **`switch`**：根据一个值匹配多个情况。

```javascript
const day = 1

if (day === 1) {
  console.log('星期一')
} else if (day === 2) {
  console.log('星期二')
} else {
  console.log('其他日期')
}
```

### 第四部分：数组

1. 创建数组：`const fruits = ['苹果', '香蕉']`。
2. 使用下标读取数据：`fruits[0]`。数组下标从 `0` 开始。
3. 使用 `length` 获取数组长度。
4. 使用 `push()` 添加数据，使用 `pop()` 删除最后一项。
5. 使用 `includes()` 判断数组是否包含某项。
6. 使用循环或 `forEach()` 依次处理数组中的数据。
7. 学习常见处理方法：`map()`、`filter()`、`find()`。

```javascript
const fruits = ['苹果', '香蕉', '橙子']

console.log(fruits[0])
console.log(fruits.length)
fruits.push('西瓜')
console.log(fruits)
```

### 第五部分：对象

1. 创建对象：用键和值描述一个事物。
2. 使用点语法读取属性：`student.name`。
3. 使用方括号语法读取属性：`student['name']`。
4. 修改、添加和删除属性。
5. 对象中保存数组，对象中也可以保存函数。
6. 学习对象解构和扩展语法。

```javascript
const student = {
  name: '小明',
  age: 18
}

console.log(student.name)
student.age = 19
```

### 第六部分：循环

1. **`for`**：知道循环次数时使用。
2. **`while`**：条件成立时持续循环。
3. **`do...while`**：先执行一次，再判断条件。
4. **`for...of`**：依次读取数组中的值。
5. **`break`**：立即结束循环。
6. **`continue`**：跳过本次循环，进入下一次。

```javascript
const fruits = ['苹果', '香蕉', '橙子']

for (const fruit of fruits) {
  console.log(fruit)
}
```

### 第七部分：函数

1. 函数是可以重复调用的一段代码。
2. 参数是函数接收的数据。
3. `return` 把结果返回给调用者。
4. 函数声明和函数表达式。
5. 箭头函数：更简洁的函数写法。
6. 变量作用域：变量在哪些代码范围内可以使用。

```javascript
function add(a, b) {
  return a + b
}

const result = add(10, 3)
console.log(result)
```

箭头函数：

```javascript
const add = (a, b) => a + b
console.log(add(10, 3))
```

### 第八部分：错误和调试

1. 认识语法错误和运行错误。
2. 使用 `console.log()` 查看变量的值。
3. 使用 `try...catch` 捕获可能发生的错误。
4. 使用浏览器开发者工具的 Console 和 Sources 调试代码。

```javascript
try {
  const result = 10 / 0
  console.log(result)
} catch (error) {
  console.log('代码执行出错')
}
```

### 第九部分：浏览器中的 JavaScript

这一部分不是纯粹的 JavaScript 语法，而是 JavaScript 操作网页的能力。

1. 使用 `getElementById()` 和 `querySelector()` 获取 HTML 元素。
2. 使用 `textContent` 修改文字。
3. 使用 `value` 读取输入框内容。
4. 使用 `classList` 添加或删除 class。
5. 使用 `setAttribute()` 读取或修改 HTML 属性。
6. 使用 `createElement()` 创建新元素。
7. 使用事件监听器响应点击、输入和提交。
8. 使用 `preventDefault()` 阻止表单默认提交。

```javascript
const messageElement = document.querySelector('#message')
messageElement.textContent = '这句话已经被 JavaScript 修改'
```

### 第十部分：加载数据和代码组织

1. 使用 `fetch()` 请求接口数据。
2. 使用 `response.json()` 读取 JSON 数据。
3. 认识 Promise。
4. 使用 `async` 和 `await` 编写异步代码。
5. 使用 `try...catch` 处理请求失败。
6. 使用 `import` 和 `export` 拆分 JavaScript 文件。
7. 了解 `class`，为后面的 Vue 组件和面向对象代码做准备。

```javascript
async function loadData() {
  const response = await fetch('https://example.com/data')
  const data = await response.json()
  console.log(data)
}
```

### 推荐学习顺序

```text
变量和数据类型
  -> 运算符
  -> 条件判断
  -> 数组
  -> 对象
  -> 循环
  -> 函数
  -> DOM
  -> 事件
  -> 表单
  -> fetch 和异步
  -> 模块和 class
```

当前已经学到对象、循环和函数，下一步补充数组基础。

## 第四课：对象

### 1. 对象是什么

对象用来描述一个具体的事物。它可以把这个事物的多个信息放在一起。

```javascript
const student = {
  name: '小明',
  age: 18,
  score: 86
}
```

这里：

- `student` 是对象变量名。
- `name`、`age`、`score` 是属性名。
- `'小明'`、`18`、`86` 是属性值。
- 属性名和属性值之间使用冒号 `:`。
- 多个属性之间使用逗号 `,`。

### 2. 读取对象属性

常用点语法：

```javascript
console.log(student.name)
```

也可以使用方括号语法：

```javascript
console.log(student['age'])
```

当属性名保存在变量中时，方括号语法很有用：

```javascript
const propertyName = 'score'
console.log(student[propertyName])
```

### 3. 修改、添加和删除属性

对象使用 `const` 声明后，不能让变量指向另一个对象，但可以修改对象里面的属性：

```javascript
student.score = 90
student.className = '一班'
delete student.age
```

- `student.score = 90`：修改已有属性。
- `student.className = '一班'`：添加新属性。
- `delete student.age`：删除属性。

### 4. 对象和数组的关系

数组适合保存多条数据，对象适合描述一条数据。实际开发中经常把它们组合起来：

```javascript
const students = [
  { name: '小明', score: 86 },
  { name: '小红', score: 92 }
]

console.log(students[0].name)
```

`students[0]` 先取得数组中的第一个对象，再通过 `.name` 取得姓名。

## 第五课：循环

### 1. 循环是什么

循环就是让一段代码重复执行。它适合处理多条数据，避免把相同代码写很多遍。

### 2. `for` 循环

`for` 通常用于已知循环次数，或者使用下标访问数组：

```javascript
const scores = [86, 90, 78]

for (let index = 0; index < scores.length; index++) {
  console.log(scores[index])
}
```

`for` 的三个部分：

```javascript
for (开始; 继续循环的条件; 每次循环后的变化) {
  // 重复执行的代码
}
```

- `let index = 0`：从下标 `0` 开始。
- `index < scores.length`：下标小于数组长度时继续。
- `index++`：每次循环结束后加 `1`。

### 3. `for...of` 循环

`for...of` 可以直接依次取得数组中的每一项：

```javascript
for (const score of scores) {
  console.log(score)
}
```

如果只需要数据本身，`for...of` 通常比手动使用下标更容易阅读。

### 4. `while` 循环

只要条件为 `true`，`while` 就会继续执行：

```javascript
let countDown = 3

while (countDown > 0) {
  console.log(countDown)
  countDown--
}
```

循环体中要改变条件相关的变量，否则可能一直循环下去。

### 5. `do...while` 循环

`do...while` 会先执行一次代码，再判断条件：

```javascript
let attempt = 0

do {
  console.log('至少执行一次')
  attempt++
} while (attempt < 1)
```

### 6. `break` 和 `continue`

`break` 立即结束整个循环：

```javascript
for (const score of scores) {
  if (score < 80) {
    break
  }

  console.log(score)
}
```

`continue` 跳过本次循环，继续下一次：

```javascript
for (const score of scores) {
  if (score < 80) {
    continue
  }

  console.log('合格成绩：' + score)
}
```

## 第六课：函数

### 1. 函数是什么

函数是一个可以重复调用的代码块。它可以接收数据，处理数据，再返回结果。

可以把函数理解为一个小工具：输入参数，执行逻辑，得到结果。

### 2. 函数声明

```javascript
function add(a, b) {
  return a + b
}
```

- `function`：声明函数的关键字。
- `add`：函数名。
- `a`、`b`：参数，表示函数需要接收的数据。
- `return`：返回计算结果。
- 大括号中的代码是函数功能。

### 3. 调用函数

函数声明后不会自动执行，需要使用函数名加括号调用：

```javascript
const result = add(10, 3)
console.log(result) // 13
```

`10` 和 `3` 是调用函数时传入的实际数据。

### 4. 函数中的条件判断

```javascript
function getLevel(score) {
  if (score >= 90) {
    return '优秀'
  }

  if (score >= 60) {
    return '及格'
  }

  return '不及格'
}

console.log(getLevel(86)) // 及格
```

执行 `return` 后，当前函数会立即结束。

### 5. 箭头函数

箭头函数是函数的另一种写法：

```javascript
const add = (a, b) => {
  return a + b
}
```

如果函数体只有一个返回结果，可以简写：

```javascript
const add = (a, b) => a + b
```

初学阶段先掌握普通函数，之后在 Vue 和数组方法中会经常看到箭头函数。

### 6. 变量作用域

在函数内部使用 `const` 或 `let` 声明的变量，只能在函数内部使用：

```javascript
function createMessage() {
  const message = '你好'
  return message
}

console.log(createMessage())
// console.log(message) // 报错，函数外不能直接使用
```

### 7. 三个知识组合案例

下面的案例把对象、数组、循环和函数组合在一起：

```javascript
const student = {
  name: '小明',
  age: 18,
  score: 86
}

const scores = [86, 90, 78]

for (let index = 0; index < scores.length; index++) {
  console.log('第 ' + (index + 1) + ' 个成绩：' + scores[index])
}

function getLevel(score) {
  if (score >= 90) {
    return '优秀'
  }

  if (score >= 60) {
    return '及格'
  }

  return '不及格'
}

function calculateAverage(scoreList) {
  let sum = 0

  for (const score of scoreList) {
    sum = sum + score
  }

  return sum / scoreList.length
}

console.log('学生姓名：' + student.name)
console.log('学生等级：' + getLevel(student.score))
console.log('平均成绩：' + calculateAverage(scores))
```

这个案例的关系是：

1. `student` 对象保存一个学生的信息。
2. `scores` 数组保存多次成绩。
3. 循环依次读取成绩。
4. `getLevel()` 判断成绩等级。
5. `calculateAverage()` 计算平均成绩。

## 第七课：数组

### 1. 数组是什么

数组用来一次保存多个数据。数组中的每一项叫作元素。

```javascript
const fruits = ['苹果', '香蕉', '橙子']
```

- 数组使用方括号 `[]`。
- 元素之间使用逗号 `,`。
- 数组可以保存字符串、数字、布尔值、对象等数据。
- 数组下标从 `0` 开始，不是从 `1` 开始。

### 2. 读取数组元素

```javascript
const fruits = ['苹果', '香蕉', '橙子']

console.log(fruits[0]) // 苹果
console.log(fruits[1]) // 香蕉
console.log(fruits[2]) // 橙子
```

下标 `0` 表示第一项，下标 `1` 表示第二项。

### 3. `length` 获取数组长度

`length` 表示数组中有多少项：

```javascript
const fruits = ['苹果', '香蕉', '橙子']

console.log(fruits.length) // 3
console.log(fruits[fruits.length - 1]) // 橙子
```

最后一项的下标是 `length - 1`。

### 4. 修改数组元素

数组使用 `const` 声明后，仍然可以修改数组里面的元素：

```javascript
const fruits = ['苹果', '香蕉', '橙子']
fruits[1] = '葡萄'

console.log(fruits) // ['苹果', '葡萄', '橙子']
```

这里修改的是数组内容，不是让 `fruits` 变量指向另一个数组。

### 5. `push()` 和 `pop()`

`push()` 把数据添加到数组末尾：

```javascript
const fruits = ['苹果', '香蕉']
fruits.push('橙子')

console.log(fruits) // ['苹果', '香蕉', '橙子']
```

`pop()` 删除并返回数组末尾的数据：

```javascript
const lastFruit = fruits.pop()
console.log(lastFruit) // 橙子
```

### 6. `includes()` 判断是否包含

`includes()` 会返回布尔值：

```javascript
const fruits = ['苹果', '香蕉', '橙子']

console.log(fruits.includes('苹果')) // true
console.log(fruits.includes('西瓜')) // false
```

### 7. `forEach()` 依次处理元素

`forEach()` 会把数组中的每一项交给函数处理：

```javascript
const fruits = ['苹果', '香蕉', '橙子']

fruits.forEach((fruit, index) => {
  console.log(index + '：' + fruit)
})
```

回调函数中的参数：

- `fruit`：当前元素。
- `index`：当前元素的下标。

### 8. `map()` 转换数组

`map()` 会根据每一项生成一个新数组：

```javascript
const fruits = ['苹果', '香蕉', '橙子']

const messages = fruits.map((fruit) => {
  return '我喜欢' + fruit
})

console.log(messages)
```

### 9. `filter()` 筛选数组

`filter()` 会返回所有符合条件的元素：

```javascript
const scores = [86, 90, 58, 75]

const passingScores = scores.filter((score) => {
  return score >= 60
})

console.log(passingScores) // [86, 90, 75]
```

### 10. `find()` 查找第一个元素

`find()` 返回第一个符合条件的元素。如果没有找到，结果是 `undefined`：

```javascript
const scores = [86, 90, 58, 75]

const firstHighScore = scores.find((score) => {
  return score >= 90
})

console.log(firstHighScore) // 90
```

### 11. 数组和对象组合

实际项目中经常使用“对象数组”：

```javascript
const students = [
  { name: '小明', score: 86 },
  { name: '小红', score: 92 },
  { name: '小刚', score: 58 }
]

const passingStudents = students.filter((student) => {
  return student.score >= 60
})

console.log(passingStudents)
```

这里先使用数组保存多名学生，再使用对象保存每名学生的姓名和成绩。

### 12. 第七课完整案例

```javascript
const fruits = ['苹果', '香蕉', '橙子']

console.log('第一个水果：' + fruits[0])
console.log('水果数量：' + fruits.length)

fruits[1] = '葡萄'
fruits.push('西瓜')

const lastFruit = fruits.pop()
console.log('删除的水果：' + lastFruit)
console.log('是否包含苹果：' + fruits.includes('苹果'))

fruits.forEach((fruit, index) => {
  console.log('水果 ' + (index + 1) + '：' + fruit)
})

const allScores = [86, 90, 58, 75]
const passingScores = allScores.filter((score) => score >= 60)
const firstHighScore = allScores.find((score) => score >= 90)

console.log('及格成绩：' + passingScores)
console.log('第一个 90 分及以上的成绩：' + firstHighScore)
```

练习文件：[javascript.html](./javascript.html)

## 第八课：错误和调试

### 1. 错误是什么

错误表示代码没有按照预期执行。常见情况有：

- **语法错误**：代码写法不符合 JavaScript 语法，例如少写一个括号。
- **运行错误**：语法正确，但运行时出现问题，例如读取不存在的属性。
- **逻辑错误**：代码可以运行，但计算结果不符合需求。

### 2. 使用 `console.log()` 调试

调试就是找出代码为什么没有按预期工作。最简单的方法是输出关键变量：

```javascript
const score = 86
console.log('当前成绩：', score)
```

在浏览器按 `F12` 打开开发者工具，再查看 Console。

### 3. 使用 `try...catch` 处理错误

可能出错的代码可以放到 `try` 中。如果发生错误，就执行 `catch`：

```javascript
try {
  const result = JSON.parse('错误的 JSON')
  console.log(result)
} catch (error) {
  console.error('解析失败：', error.message)
}
```

- `try`：尝试执行代码。
- `catch`：捕获错误并处理。
- `error.message`：错误信息。
- `console.error()`：以错误形式输出到 Console。

### 4. 主动抛出错误

使用 `throw new Error()` 可以主动终止当前处理并交给 `catch`：

```javascript
const score = 120

try {
  if (score < 0 || score > 100) {
    throw new Error('成绩必须在 0 到 100 之间')
  }
} catch (error) {
  console.log(error.message)
}
```

## 第九课：DOM 网页操作

### 1. DOM 是什么

DOM 是浏览器根据 HTML 创建的页面对象树。JavaScript 可以通过 DOM 找到 HTML 元素并修改它们。

```text
HTML 文件 -> 浏览器解析 -> DOM 对象树 -> JavaScript 操作
```

### 2. 获取元素

根据 `id` 获取元素：

```javascript
const messageElement = document.getElementById('message')
```

使用 CSS 选择器获取元素：

```javascript
const messageElement = document.querySelector('#message')
const buttons = document.querySelectorAll('button')
```

- `querySelector()`：返回第一个符合条件的元素。
- `querySelectorAll()`：返回所有符合条件的元素集合。

### 3. 修改页面文字

使用 `textContent` 修改元素中的文字：

```javascript
const messageElement = document.querySelector('#message')
messageElement.textContent = '文字已经被 JavaScript 修改'
```

如果内容来自用户输入，优先使用 `textContent`，不要直接把不可信内容放进 `innerHTML`。

### 4. 读取和修改表单值

输入框的内容在 `value` 属性中：

```javascript
const nameInput = document.querySelector('#student-name')
console.log(nameInput.value)
nameInput.value = '小明'
```

### 5. 创建和添加元素

```javascript
const listItem = document.createElement('li')
listItem.textContent = '小明：86 分'
document.querySelector('#student-list').appendChild(listItem)
```

- `createElement()`：创建元素。
- `textContent`：设置文字。
- `appendChild()`：把元素添加到页面中。

### 6. DOM 的完整工作过程

DOM 操作可以总结成四步：

```text
找到元素 -> 读取或准备数据 -> 修改 DOM -> 浏览器更新页面
```

例如：

```html
<p id="message">原来的文字</p>
```

```javascript
const messageElement = document.querySelector('#message')
messageElement.textContent = '新的文字'
```

第一行找到元素，第二行修改元素内容。浏览器会把页面显示成“新的文字”。

### 7. `document` 和元素对象

`document` 代表当前 HTML 文档：

```javascript
console.log(document)
```

从 `document` 中找到的结果是元素对象：

```javascript
const messageElement = document.querySelector('#message')
console.log(messageElement)
```

`messageElement` 不是字符串，而是代表页面中那个 `p` 标签的对象，所以它拥有 `textContent`、`classList`、`setAttribute()` 等能力。

### 8. 获取元素的三种常用方法

#### `getElementById()`

根据 id 获取元素：

```javascript
const messageElement = document.getElementById('message')
```

id 应该在页面中唯一，因此这个方法最多返回一个元素。如果找不到，会返回 `null`。

#### `querySelector()`

使用 CSS 选择器获取第一个匹配的元素：

```javascript
const messageElement = document.querySelector('#message')
const firstButton = document.querySelector('button')
const firstInput = document.querySelector('input[name="name"]')
```

常见选择器：

| 选择器 | 含义 | 示例 |
| --- | --- | --- |
| `#id` | 根据 id 查找 | `#message` |
| `.class` | 根据 class 查找 | `.item` |
| `tag` | 根据标签查找 | `button` |
| `[属性]` | 根据属性查找 | `input[name="name"]` |
| `父元素 子元素` | 查找后代元素 | `#student-list li` |

#### `querySelectorAll()`

获取所有匹配的元素：

```javascript
const buttons = document.querySelectorAll('button')

buttons.forEach((button) => {
  console.log(button.textContent)
})
```

- `querySelector()`：只返回第一个匹配结果。
- `querySelectorAll()`：返回所有匹配结果。

### 9. 找不到元素时的处理

找不到元素时，结果是 `null`：

```javascript
const element = document.querySelector('#not-found')
console.log(element) // null
```

下面的代码会报错：

```javascript
element.textContent = '修改文字'
```

因为 `null` 没有 `textContent`。可以先判断：

```javascript
if (element !== null) {
  element.textContent = '修改文字'
}
```

### 10. 修改文字：`textContent`

```javascript
const messageElement = document.querySelector('#message')
messageElement.textContent = '这句话已经被 JavaScript 修改'
```

`textContent` 会把内容当成普通文字：

```javascript
messageElement.textContent = '<strong>这不是标签</strong>'
```

页面会显示尖括号文字，不会生成真正的 `strong` 标签。处理用户输入时，优先使用 `textContent`。

### 11. `innerHTML` 修改 HTML

`innerHTML` 会把字符串当成 HTML 解析：

```javascript
messageElement.innerHTML = '<strong>加粗文字</strong>'
```

这时页面会生成真正的 `strong` 元素。但不要把不可信的用户输入直接放入 `innerHTML`：

```javascript
// 不要这样直接处理用户输入
element.innerHTML = userInput
```

初学阶段记住：只修改文字使用 `textContent`，确实需要插入可信 HTML 时才使用 `innerHTML`。

### 12. 读取和修改输入框：`value`

普通标签的文字使用 `textContent`，输入框填写的内容使用 `value`：

```javascript
const input = document.querySelector('#dom-input')

console.log(input.value)
input.value = 'JavaScript'
```

```javascript
element.textContent // 普通标签内部的文字
input.value        // 输入框当前填写的内容
```

### 13. 读取和修改 HTML 属性

```javascript
const link = document.querySelector('#dom-link')

console.log(link.getAttribute('href'))
link.setAttribute('href', 'https://developer.mozilla.org/')
```

常用方法：

- `getAttribute('属性名')`：读取属性。
- `setAttribute('属性名', '属性值')`：设置属性。
- `removeAttribute('属性名')`：删除属性。
- `hasAttribute('属性名')`：判断属性是否存在。

也可以直接读写一些常用属性：

```javascript
console.log(link.href)
link.target = '_blank'
```

### 14. 操作 class：`classList`

```javascript
const target = document.querySelector('#dom-class-target')

target.classList.add('highlight-text')
target.classList.remove('highlight-text')
target.classList.toggle('highlight-text')
console.log(target.classList.contains('highlight-text'))
```

- `add()`：添加 class。
- `remove()`：删除 class。
- `toggle()`：有就删除，没有就添加。
- `contains()`：判断是否有某个 class。

DOM 只负责把 class 加到元素上，真正的颜色和大小要由 CSS 决定。本练习页面没有添加 CSS，所以切换 class 时主要观察输出的 class 名称。

### 15. 创建元素：`createElement()`

```javascript
const listItem = document.createElement('li')
listItem.textContent = '新列表项'
```

此时元素只存在于 JavaScript 内存中，还没有显示到页面。

### 16. 添加元素：`appendChild()`

```javascript
const list = document.querySelector('#dom-item-list')
list.appendChild(listItem)
```

`appendChild()` 会把元素添加到父元素的最后面。

其他常用方法：

```javascript
list.prepend(listItem) // 放到最前面
listItem.before(otherElement) // 放到它前面
listItem.after(otherElement) // 放到它后面
```

### 17. 删除元素

```javascript
listItem.remove()
```

删除列表最后一项：

```javascript
const lastItem = list.lastElementChild

if (lastItem !== null) {
  lastItem.remove()
}
```

### 18. 获取父子元素

```javascript
const list = document.querySelector('#dom-item-list')

console.log(list.children.length)
console.log(list.firstElementChild)
console.log(list.lastElementChild)
console.log(list.parentElement)
```

- `children`：所有子元素。
- `firstElementChild`：第一个子元素。
- `lastElementChild`：最后一个子元素。
- `parentElement`：父元素。

### 19. DOM 综合案例

```javascript
const input = document.querySelector('#dom-item-input')
const list = document.querySelector('#dom-item-list')

const itemText = input.value.trim()

if (itemText !== '') {
  const listItem = document.createElement('li')
  listItem.textContent = itemText
  list.appendChild(listItem)
  input.value = ''
}
```

执行过程：

1. 找到输入框和列表。
2. 读取输入框的 `value`。
3. 使用 `trim()` 删除首尾空格。
4. 创建 `li` 元素。
5. 使用 `textContent` 写入文字。
6. 使用 `appendChild()` 放入列表。
7. 清空输入框。

### 20. 本项目中的 DOM 练习

打开 [javascript.html](./javascript.html)，可以练习：

- “修改 DOM 文字”：修改 `textContent`。
- “读取输入框”：读取 `input.value`。
- “修改链接属性”：使用 `getAttribute()` 和 `setAttribute()`。
- “切换 class”：使用 `classList.toggle()`。
- “添加列表项”：使用 `createElement()` 和 `appendChild()`。
- “删除最后一项”：使用 `lastElementChild` 和 `remove()`。

## 第十课：事件和表单

### 1. 事件是什么

事件表示浏览器中发生了某件事，例如点击按钮、输入文字、提交表单。

### 2. 监听点击事件

使用 `addEventListener()` 监听事件：

```javascript
const button = document.querySelector('#change-message')
const messageElement = document.querySelector('#message')

button.addEventListener('click', () => {
  messageElement.textContent = '按钮被点击了'
})
```

- `'click'`：事件名称。
- 第二个参数：事件发生后要执行的函数。

### 3. 详细拆解事件监听语法

```javascript
element.addEventListener('click', () => {
  // 点击后执行
})
```

通用结构：

```javascript
对象.方法('事件名称', 事件处理函数)
```

这段代码中：

- `element`：一个 HTML 元素对象。
- `.`：访问对象中的属性或方法。
- `addEventListener`：浏览器提供的事件监听方法。
- `'click'`：浏览器 DOM 规范规定的点击事件名称。
- `() => {}`：箭头函数，表示事件发生后要执行的代码。

`'click'` 不是你自己定义的变量，也不是元素属性。它是浏览器认识的标准事件类型名称。常见标准事件名称还有：

```javascript
'input'   // 输入框内容变化
'change'  // 表单值确认变化
'submit'  // 表单提交
'keydown' // 键盘按下
```

箭头函数的语法：

```javascript
(参数) => {
  // 函数体
}
```

当前例子中的 `()` 表示没有参数。它等价于：

```javascript
function () {
  // 点击后执行
}
```

箭头函数不会立即执行，而是交给 `addEventListener()`，等点击发生后再执行。

注意函数名和函数调用的区别：

```javascript
element.addEventListener('click', handleClick)
element.addEventListener('click', handleClick())
```

第一行是把函数交给浏览器之后执行，第二行会在注册监听时立即执行 `handleClick()`。

### 4. 当前演示文件中的点击事件

HTML：

```html
<p id="dom-message">这段文字还没有被 JavaScript 修改。</p>
<button id="change-message-button">修改文字</button>
```

JavaScript：

```javascript
const domMessage = document.querySelector('#dom-message')

document.querySelector('#change-message-button').addEventListener('click', () => {
  domMessage.textContent = 'querySelector 找到我，textContent 修改了我。'
})
```

执行顺序：

1. 使用 `querySelector()` 找到文字元素。
2. 使用 `querySelector()` 找到按钮。
3. 使用 `addEventListener()` 监听按钮的 `click` 事件。
4. 用户点击按钮。
5. 箭头函数执行。
6. 使用 `textContent` 修改文字。

### 3. 监听表单提交

```javascript
const form = document.querySelector('#student-form')

form.addEventListener('submit', (event) => {
  event.preventDefault()
  console.log('表单提交了')
})
```

`preventDefault()` 会阻止浏览器默认刷新页面和提交行为，让 JavaScript 自己处理表单。

### 4. 使用 `FormData` 读取表单

表单控件要有 `name` 属性，`FormData` 才能根据字段名读取数据：

```html
<input name="name" type="text" />
<input name="score" type="number" />
```

```javascript
const formData = new FormData(form)
const name = formData.get('name')
const score = Number(formData.get('score'))
```

### 5. 表单处理的一般流程

```text
监听 submit
  -> 阻止默认刷新
  -> 读取表单数据
  -> 校验数据
  -> 保存或发送数据
  -> 更新页面提示
```

### 6. 当前演示文件中的 `input.value`

HTML：

```html
<input id="dom-input" type="text" value="初始内容" />
<button id="read-input-button">读取输入框</button>
<p id="dom-input-output"></p>
```

JavaScript：

```javascript
const domInput = document.querySelector('#dom-input')
const domInputOutput = document.querySelector('#dom-input-output')

document.querySelector('#read-input-button').addEventListener('click', () => {
  domInputOutput.textContent = 'input.value：' + domInput.value
})
```

这里：

- `domInput`：保存输入框元素对象。
- `domInput.value`：读取输入框当前填写的内容。
- `domInputOutput.textContent`：把结果写入页面上的 `p` 标签。

### 7. `value` 和 `textContent` 的区别

普通标签使用 `textContent`：

```javascript
const message = document.querySelector('#dom-message')
console.log(message.textContent)
```

输入框使用 `value`：

```javascript
const input = document.querySelector('#dom-input')
console.log(input.value)
```

记忆：

```text
p、div、span 等普通元素 -> textContent
input、textarea、select  -> value
```

`value` 也可以重新赋值：

```javascript
input.value = 'JavaScript'
input.value = ''
```

第一行把输入框改成 `JavaScript`，第二行清空输入框。

在 [javascript.html](./javascript.html) 中修改“输入内容”，再点击“读取输入框”，观察 `domInput.value` 的结果。

## 第十一课：`fetch` 加载数据

### 1. `fetch()` 是什么

`fetch()` 用来请求网络或本地服务器上的数据。它返回一个 Promise，因为数据需要等待一段时间才能返回。

```javascript
const response = await fetch('./students.json')
const data = await response.json()
console.log(data)
```

### 2. 检查响应是否成功

HTTP 请求即使返回了错误状态，也可能正常得到一个响应对象，所以要检查 `response.ok`：

```javascript
const response = await fetch('./students.json')

if (!response.ok) {
  throw new Error('数据请求失败')
}

const data = await response.json()
```

### 3. 加载本地 JSON 数据

之前的多文件示例使用过 `students.json`。当前单文件演示页会在运行时生成同样结构的 JSON 数据：

```json
[
  { "name": "小明", "score": 86 },
  { "name": "小红", "score": 92 },
  { "name": "小刚", "score": 58 }
]
```

JavaScript 加载后，再把普通对象转换成 `Student` 实例，并渲染到页面。

### 4. `fetch` 的错误处理

```javascript
async function loadData() {
  try {
    const response = await fetch('./students.json')

    if (!response.ok) {
      throw new Error('请求失败')
    }

    return await response.json()
  } catch (error) {
    console.error('加载失败：', error)
    return []
  }
}
```

## 第十二课：Promise、`async`、`await`

### 1. Promise 是什么

Promise 表示一个现在还没有完成、以后会得到结果的任务。常见状态有：

- `pending`：等待中。
- `fulfilled`：成功完成。
- `rejected`：失败。

### 2. 使用 `then()` 和 `catch()`

```javascript
fetch('./students.json')
  .then((response) => response.json())
  .then((data) => {
    console.log(data)
  })
  .catch((error) => {
    console.error('加载失败：', error)
  })
```

- `then()`：上一步成功后继续处理。
- `catch()`：任意一步失败后处理错误。

### 3. `async` 和 `await`

`async` 声明一个异步函数，`await` 等待 Promise 的结果：

```javascript
async function loadStudents() {
  const response = await fetch('./students.json')
  const data = await response.json()
  return data
}
```

`await` 只能在异步函数中使用，或者在支持顶层 await 的模块中使用。

### 4. 异步函数的返回值

即使函数中 `return` 一个普通数据，`async` 函数返回的仍然是 Promise：

```javascript
async function getMessage() {
  return '加载完成'
}

getMessage().then((message) => {
  console.log(message)
})
```

### 5. `async/await` 和 `try/catch`

```javascript
async function loadData() {
  try {
    const response = await fetch('./students.json')
    return await response.json()
  } catch (error) {
    console.error(error)
    return []
  }
}
```

## 第十三课：`import` 和 `export`

### 1. 为什么拆分文件

把所有代码写在一个文件中会越来越难维护。可以按功能拆分：

```text
javascript-main.js  页面入口和事件
student-tools.js    学生相关工具函数
student.js          Student 类
```

### 2. 导出函数

在 `student-tools.js` 中：

```javascript
export function calculateAverage(scores) {
  // 计算平均值
}
```

`export` 表示允许其他文件使用这个函数。

### 3. 导入函数

在 `javascript-main.js` 中：

```javascript
import { calculateAverage } from './student-tools.js'
```

`./` 表示当前目录，导入本地文件时要写文件扩展名 `.js`。

### 4. 导出和导入类

在 `student.js` 中：

```javascript
export class Student {
  constructor(name, score) {
    this.name = name
    this.score = score
  }
}
```

在入口文件中：

```javascript
import { Student } from './student.js'

const student = new Student('小明', 86)
```

### 5. HTML 中使用模块

```html
<script type="module" src="./javascript-main.js"></script>
```

`type="module"` 告诉浏览器这个脚本需要使用 ES Module 的 `import/export` 语法。

## 第十四课：`class`

### 1. `class` 是什么

`class` 是创建对象的一种模板。它把相同类型对象共有的数据和功能集中起来。

```javascript
class Student {
  constructor(name, score) {
    this.name = name
    this.score = score
  }

  describe() {
    return this.name + '：' + this.score + ' 分'
  }
}
```

### 2. `constructor`

`constructor` 是创建实例时自动执行的方法，用来初始化对象属性：

```javascript
const student = new Student('小明', 86)
console.log(student.name)
```

- `new Student(...)`：根据模板创建一个新实例。
- `this.name`：当前实例的 `name` 属性。

### 3. class 方法

class 中可以定义方法，让实例拥有共同的功能：

```javascript
class Student {
  getLevel() {
    if (this.score >= 60) {
      return '及格'
    }

    return '不及格'
  }
}
```

调用方法：

```javascript
console.log(student.getLevel())
```

### 4. 本项目中的 `Student`

当前单文件演示页中的 `Student` 类包含：

- `name`：学生姓名。
- `score`：学生成绩。
- `getLevel()`：获取成绩等级。
- `describe()`：生成学生描述文字。

## 六部分综合关系

```text
错误和调试：发现并处理问题
  -> DOM：找到并修改 HTML
  -> 事件和表单：响应用户操作
  -> fetch：请求外部或本地数据
  -> Promise/async/await：等待异步结果
  -> import/export：拆分代码文件
  -> class：创建结构清晰的对象
```

## 综合练习文件

现在只保留一个演示文件：[javascript.html](./javascript.html)

## DOM 完整主线

### 1. DOM 在网页中的位置

```text
HTML        决定页面有什么
CSS         决定页面长什么样
JavaScript  决定页面如何变化
DOM         是 JavaScript 操作 HTML 的对象模型
```

浏览器读取 HTML 后，不是让 JavaScript 直接修改 HTML 文件，而是把 HTML 解析成 DOM 对象树。JavaScript 修改的是浏览器内存中的 DOM，页面随之重新显示。

例如：

```html
<p id="message">你好</p>
```

```javascript
const messageElement = document.querySelector('#message')
messageElement.textContent = '你好，DOM'
```

### 2. DOM 树

HTML：

```html
<body>
  <h1>标题</h1>
  <ul>
    <li>第一项</li>
    <li>第二项</li>
  </ul>
</body>
```

可以理解成：

```text
document
└── html
    └── body
        ├── h1
        └── ul
            ├── li
            └── li
```

DOM 树中的关系：

- `body` 是 `h1` 和 `ul` 的父元素。
- `ul` 是两个 `li` 的父元素。
- 两个 `li` 互相是兄弟元素。
- `h1` 和 `ul` 都是 `body` 的子元素。

### 3. 节点和元素

“节点”范围比较大：

- 元素节点：`div`、`p`、`button` 等标签。
- 文本节点：标签中的普通文字。
- 注释节点：`<!-- 注释 -->`。
- 文档节点：整个 `document`。

初学时最常操作的是元素节点：

```javascript
const paragraph = document.querySelector('p')
```

`paragraph` 是一个元素对象，所以可以使用 `textContent`、`classList`、`remove()` 等能力。

### 4. `document` 和 `window`

```javascript
window    // 浏览器窗口
document  // 当前 HTML 文档
```

`document` 实际上属于 `window`：

```javascript
window.document === document // true
```

平时直接写 `document` 即可。

### DOM 主线：查找元素

```javascript
document.querySelector('#message')
```

拆开看：

```text
document       当前 HTML 文档对象
.              访问对象中的属性或方法
querySelector  查找元素的方法
(              调用方法
'#message'     传入的选择器字符串
)              调用结束
```

通用结构是：

```javascript
对象.方法(参数)
```

`querySelector()` 使用 CSS 选择器：

```javascript
document.querySelector('#message')
document.querySelector('.item')
document.querySelector('button')
document.querySelector('input[name="name"]')
document.querySelector('#student-list li')
```

- `#message`：查找 id。
- `.item`：查找 class。
- `button`：查找标签。
- `input[name="name"]`：查找指定属性。
- `#student-list li`：查找某个区域里面的 li。

```javascript
const one = document.querySelector('p')
const many = document.querySelectorAll('p')
```

- `querySelector()`：返回第一个匹配元素。
- `querySelectorAll()`：返回所有匹配元素的 `NodeList`。

查找失败时：

```javascript
const element = document.querySelector('#not-found')

if (element !== null) {
  element.textContent = '查找成功'
}
```

`querySelector()` 找不到时返回 `null`，所以不能直接对 `null` 使用 `textContent`。

### DOM 主线：修改内容

假设页面中有：

```html
<p id="message">原来的文字</p>
```

```javascript
const message = document.querySelector('#message')
message.textContent = '新的文字'
```

`textContent` 读写元素里面的普通文字：

```javascript
console.log(message.textContent)
message.textContent = '<strong>这不是标签</strong>'
```

上面的字符串会按普通文字显示，不会生成真正的 `strong` 标签。

如果确实要设置 HTML：

```javascript
message.innerHTML = '<strong>加粗文字</strong>'
```

区别：

```text
textContent  把内容当普通文字
innerHTML    把内容当 HTML 解析
```

用户输入优先使用 `textContent`，不要把不可信的用户输入直接放入 `innerHTML`。

### DOM 主线：读取输入框

普通标签使用 `textContent`，输入框使用 `value`：

```html
<input id="name-input" value="初始值" />
```

```javascript
const input = document.querySelector('#name-input')
console.log(input.value)
input.value = '小明'
```

`input.value` 是用户当前填写的值。

### DOM 主线：读取和修改属性

```javascript
element.getAttribute('title')
element.setAttribute('title', '提示文字')
element.hasAttribute('title')
element.removeAttribute('title')
```

例如：

```javascript
const link = document.querySelector('#dom-link')
const oldHref = link.getAttribute('href')
link.setAttribute('href', 'https://developer.mozilla.org/')
console.log(oldHref)
```

有些常用属性也可以直接读写：

```javascript
link.href = 'https://developer.mozilla.org/'
link.target = '_blank'
```

### DOM 主线：修改 class 和样式

使用 `classList` 管理 class：

```javascript
element.classList.add('active')
element.classList.remove('active')
element.classList.toggle('active')
element.classList.contains('active')
```

DOM 只负责添加或删除 class，CSS 决定 class 的视觉效果。

也可以直接修改内联样式：

```javascript
element.style.color = 'red'
element.style.backgroundColor = 'lightyellow'
element.style.padding = '8px'
```

CSS 属性名要从短横线改成小驼峰，例如 `background-color` 写成 `backgroundColor`。

恢复内联样式：

```javascript
element.style.color = ''
```

## DOM 方法速查：不同对象有哪些方法

### 1. 先理解“赋值后是什么对象”

```javascript
const one = document.querySelector('#message')
const many = document.querySelectorAll('p')
const created = document.createElement('p')
const classes = one.classList
```

这四个变量不是同一种对象：

| 变量 | 结果类型 | 代表什么 |
| --- | --- | --- |
| `one` | `Element` 或 `null` | 一个 HTML 元素，或者没有找到 |
| `many` | `NodeList` | 多个查询结果组成的集合 |
| `created` | `HTMLElement` | 一个刚创建的 HTML 元素 |
| `classes` | `DOMTokenList` | 管理 class 的对象 |

变量能使用哪些方法，取决于它保存的对象类型。

### 2. 属性和方法的区别

属性是对象中的数据，通常直接读取或赋值：

```javascript
element.textContent = '新的文字'
input.value = '小明'
```

方法是对象提供的功能，调用时要写小括号：

```javascript
element.remove()
element.setAttribute('title', '提示文字')
```

通用语法：

```javascript
对象.属性 = 数据
对象.方法(参数)
```

例如：

```javascript
element.classList.add('active')
```

这里 `classList` 是属性，`add()` 是 `classList` 对象的方法。

### 3. `document` 的常用方法

`document` 代表当前 HTML 页面，主要负责查找和创建：

```javascript
document.getElementById('message')
document.querySelector('#message')
document.querySelectorAll('p')
document.createElement('li')
document.createTextNode('普通文字')
```

根据标签或 class 查找：

```javascript
document.getElementsByTagName('button')
document.getElementsByClassName('item')
```

初学阶段优先使用：

```javascript
document.querySelector()
document.querySelectorAll()
```

`document` 还有常用属性：

```javascript
document.body
document.head
document.title = '新的页面标题'
```

`document.title` 是属性，不是方法。

### 4. `Element` 的常用属性

假设：

```javascript
const element = document.querySelector('#message')
```

常用属性：

```javascript
element.textContent
element.innerHTML
element.id
element.className
element.classList
element.tagName
element.parentElement
element.children
```

输入框对象还常用：

```javascript
const input = document.querySelector('#dom-input')
input.value
input.disabled
input.required
input.type
```

### 5. `Element` 的文字和内容方法

修改普通文字：

```javascript
element.textContent = '新的文字'
```

修改 HTML：

```javascript
element.innerHTML = '<strong>加粗文字</strong>'
```

清空内容：

```javascript
element.textContent = ''
```

`textContent` 会把内容当作普通文字，更适合处理用户输入。`innerHTML` 会解析 HTML，不能直接放入不可信的用户输入。

### 6. `Element` 的属性方法

```javascript
element.getAttribute('title')
element.setAttribute('title', '提示文字')
element.hasAttribute('title')
element.removeAttribute('title')
```

示例：

```javascript
const link = document.querySelector('#dom-link')
link.setAttribute('href', 'https://developer.mozilla.org/')
console.log(link.getAttribute('href'))
```

### 7. `classList` 对象的方法

```javascript
element.classList.add('active')
element.classList.remove('active')
element.classList.toggle('active')
element.classList.contains('active')
element.classList.replace('old-class', 'new-class')
```

- `add()`：添加 class。
- `remove()`：删除 class。
- `toggle()`：有就删除，没有就添加。
- `contains()`：判断是否有某个 class。
- `replace()`：把旧 class 换成新 class。

### 8. `Element` 的结构方法

创建元素后，先放在变量中：

```javascript
const listItem = document.createElement('li')
listItem.textContent = '新列表项'
```

添加到页面：

```javascript
const list = document.querySelector('#dom-item-list')
list.appendChild(listItem)
list.append(listItem)
list.prepend(listItem)
```

- `appendChild()`：添加一个节点到末尾。
- `append()`：添加节点或文字到末尾。
- `prepend()`：添加到最前面。

在元素前后添加、替换和删除：

```javascript
element.before(otherElement)
element.after(otherElement)
element.replaceWith(otherElement)
element.remove()
```

### 9. 从元素内部继续查找

查找不一定从 `document` 开始，也可以从某个元素内部开始：

```javascript
const list = document.querySelector('#dom-item-list')
const firstItem = list.querySelector('li')
const allItems = list.querySelectorAll('li')
```

`list.querySelector('li')` 只在 `list` 内部查找。

查找最近的祖先元素：

```javascript
const item = document.querySelector('li')
const list = item.closest('ul')
```

判断元素是否符合选择器：

```javascript
if (item.matches('li')) {
  console.log('这是一个 li 元素')
}
```

### 10. `NodeList` 的方法

```javascript
const elements = document.querySelectorAll('p')
```

`elements` 是多个元素组成的 `NodeList`，不是单个元素：

```javascript
elements.length
elements[0]
elements.item(0)
elements.forEach((element) => {
  console.log(element.textContent)
})
```

不能这样一次修改所有元素：

```javascript
// 错误思路：NodeList 不是一个元素
elements.textContent = '统一修改'
```

应该循环每一个元素：

```javascript
elements.forEach((element) => {
  element.textContent = '统一修改'
})
```

### 11. 父元素和子元素

```javascript
const list = document.querySelector('#dom-item-list')

list.children
list.firstElementChild
list.lastElementChild
list.parentElement
```

- `children`：子元素集合。
- `firstElementChild`：第一个子元素。
- `lastElementChild`：最后一个子元素。
- `parentElement`：父元素。

删除最后一项：

```javascript
const lastItem = list.lastElementChild

if (lastItem !== null) {
  lastItem.remove()
}
```

### 12. 表单元素常用方法

```javascript
const form = document.querySelector('#student-form')
const input = document.querySelector('#dom-input')
```

表单：

```javascript
form.reset()
form.requestSubmit()
```

输入框：

```javascript
input.focus()
input.blur()
input.select()
```

### 13. 元素的事件方法

```javascript
element.addEventListener('click', () => {
  console.log('元素被点击')
})
```

如果要移除事件，需要保存原来的函数：

```javascript
function handleClick() {
  console.log('点击')
}

element.addEventListener('click', handleClick)
element.removeEventListener('click', handleClick)
```

### 14. 根据返回值判断对象类型

```javascript
const one = document.querySelector('#message')
// one 是 Element 或 null

const many = document.querySelectorAll('p')
// many 是 NodeList

const created = document.createElement('p')
// created 是 HTMLElement

const classes = one.classList
// classes 是 DOMTokenList
```

所以这些变量能调用的方法不同：

```javascript
one.remove()
many.forEach(() => {})
created.setAttribute('data-type', 'message')
classes.add('active')
```

### 15. 初学阶段最需要记住的方法

```javascript
// document：查找和创建
document.querySelector()
document.querySelectorAll()
document.getElementById()
document.createElement()

// Element：修改和操作一个元素
element.textContent
element.value
element.setAttribute()
element.getAttribute()
element.classList.add()
element.classList.remove()
element.classList.toggle()
element.appendChild()
element.remove()
element.addEventListener()
```

记忆方式：

```text
document 找元素
Element 改元素
NodeList 管多个元素
classList 管 class
```

## DOM 完整主线（续）

### 1. 父元素、子元素和兄弟元素

假设 HTML 是：

```html
<div id="root">
  <p id="first">第一项</p>
  <p id="second">第二项</p>
</div>
```

获取第一个 `p`：

```javascript
const first = document.querySelector('#first')
```

获取它的父元素：

```javascript
console.log(first.parentElement)
```

获取父元素的所有子元素：

```javascript
const root = first.parentElement
console.log(root.children)
console.log(root.children.length)
```

获取兄弟元素：

```javascript
console.log(first.nextElementSibling)
console.log(first.previousElementSibling)
```

常用关系属性：

- `parentElement`：父元素。
- `children`：所有子元素。
- `firstElementChild`：第一个子元素。
- `lastElementChild`：最后一个子元素。
- `nextElementSibling`：下一个兄弟元素。
- `previousElementSibling`：上一个兄弟元素。

### 2. 创建元素的三个步骤

```javascript
const listItem = document.createElement('li')
listItem.textContent = '新列表项'
document.querySelector('#dom-item-list').appendChild(listItem)
```

执行过程：

1. `createElement()` 在内存中创建元素。
2. 设置元素的文字、属性和 class。
3. 使用 `appendChild()` 放入 DOM 树。

只创建、不添加，页面上不会显示：

```javascript
const item = document.createElement('li')
item.textContent = '还没有放进页面'
```

### 3. 添加、移动、复制、替换和删除

常用结构方法：

```javascript
parent.appendChild(child)
parent.append(child)
parent.prepend(child)
child.before(otherElement)
child.after(otherElement)
child.replaceWith(otherElement)
child.remove()
```

同一个元素再次 `appendChild()` 时不会复制，而是移动：

```javascript
firstList.appendChild(item)
secondList.appendChild(item)
```

执行后，`item` 会从 `firstList` 移到 `secondList`。

复制元素：

```javascript
const copy = child.cloneNode(true)
```

`true` 表示同时复制子元素，`false` 只复制当前元素。

### 4. `NodeList` 和多个元素

```javascript
const paragraphs = document.querySelectorAll('p')
```

`paragraphs` 是多个元素组成的 `NodeList`，不是一个 `p` 元素：

```javascript
console.log(paragraphs.length)
console.log(paragraphs[0])

paragraphs.forEach((paragraph) => {
  paragraph.textContent = '统一修改'
})
```

不能把集合当作单个元素：

```javascript
// 错误思路
paragraphs.textContent = '统一修改'
```

要修改所有元素，必须循环处理每个元素。

### 5. 从元素内部继续查找

查找不一定从 `document` 开始，也可以从某个元素内部开始：

```javascript
const list = document.querySelector('#dom-item-list')
const firstItem = list.querySelector('li')
const allItems = list.querySelectorAll('li')
```

`list.querySelector('li')` 只在 `list` 内部查找。

查找最近的祖先元素：

```javascript
const item = document.querySelector('li')
const listElement = item.closest('ul')
```

判断元素是否符合选择器：

```javascript
if (item.matches('li')) {
  console.log('这是一个 li 元素')
}
```

### 6. DOM 和事件的关系

DOM 负责找到元素，事件负责知道用户做了什么：

```javascript
const button = document.querySelector('#change-message')
const message = document.querySelector('#message')

button.addEventListener('click', () => {
  message.textContent = '按钮被点击了'
})
```

执行过程：

1. DOM 找到按钮和文字元素。
2. `addEventListener()` 监听点击。
3. 用户点击按钮。
4. 回调函数执行。
5. DOM 修改文字。

### 7. 事件对象和事件目标

```javascript
button.addEventListener('click', (event) => {
  console.log(event)
  console.log(event.target)
  console.log(event.currentTarget)
})
```

- `event`：这次事件的详细信息。
- `event.target`：真正触发事件的元素。
- `event.currentTarget`：当前正在监听事件的元素。

### 8. 页面加载时机

HTML 结构解析完成时会触发 `DOMContentLoaded`：

```javascript
document.addEventListener('DOMContentLoaded', () => {
  const message = document.querySelector('#message')
  message.textContent = 'DOM 已经准备好'
})
```

如果 JavaScript 写在 `body` 最后，通常也能保证元素已经存在。当前练习使用模块脚本，也可以直接查找页面元素。

### 9. DOM 综合案例

```javascript
const input = document.querySelector('#dom-item-input')
const list = document.querySelector('#dom-item-list')

const itemText = input.value.trim()

if (itemText !== '') {
  const listItem = document.createElement('li')
  listItem.textContent = itemText
  listItem.dataset.source = 'user'
  list.appendChild(listItem)
  input.value = ''
}
```

这段代码同时使用了：

- `querySelector()`：查找输入框和列表。
- `value`：读取输入框。
- `trim()`：清理首尾空格。
- `createElement()`：创建元素。
- `textContent`：写入普通文字。
- `dataset`：保存自定义数据。
- `appendChild()`：加入 DOM 树。
- `value = ''`：清空输入框。

### 10. DOM 安全规则

1. 用户输入显示到页面时优先使用 `textContent`。
2. 不要把不可信的用户输入直接赋值给 `innerHTML`。
3. 使用 `querySelector()` 后考虑找不到元素的情况。
4. 修改多个元素时使用循环，不要把集合当成单个元素。
5. DOM 负责页面对象和结构，CSS 负责样式，事件负责用户操作。

### 11. 当前页面的 DOM 练习

打开 [javascript.html](./javascript.html)，可以观察：

- 修改文字：`textContent`。
- 读取输入框：`value`。
- 修改链接：`getAttribute()`、`setAttribute()`。
- 切换 class：`classList.toggle()`。
- 创建列表项：`createElement()`、`appendChild()`。
- 删除列表项：`lastElementChild`、`remove()`。
- 查看对象类型：`Element`、`NodeList`、`HTMLElement`、`DOMTokenList`。
- 查看父子关系：`parentElement`、`children`。
- 读取自定义数据：`dataset`。
- 修改和恢复内联样式：`style`。
## 统一格式知识点总表

下面是当前 JavaScript 学习路线的统一版索引。每个知识点都按“是什么、作用、解决的问题、语法、案例”整理。

### 1. JavaScript
- 是什么：运行在浏览器中的编程语言。
- 作用：处理数据、判断流程、响应操作、修改页面。
- 解决的问题：让固定的 HTML 页面具备计算和交互能力。
- 语法：`<script>console.log('你好')</script>`。
- 案例：`const message = '你好'; console.log(message)`。

### 2. 注释
- 是什么：给开发者看的说明，浏览器不会执行。
- 作用：解释代码、标记区域、暂时关闭代码。
- 解决的问题：代码变多后难以理解和维护。
- 语法：`// 单行注释`；`/* 多行注释 */`。
- 案例：`// 计算总价`。

### 3. 变量、`const` 和 `let`
- 是什么：变量是保存数据的名字；`const` 和 `let` 用来声明变量。
- 作用：给数据起有意义的名字；`const` 不重新赋值，`let` 可以重新赋值。
- 解决的问题：避免重复书写具体数据，方便更新。
- 语法：`const name = '小明'`；`let count = 1; count = 2`。
- 案例：`let studyCount = 1; studyCount += 1`。

### 4. 数据类型
- 是什么：数据的种类，包括字符串、数字、布尔值、`undefined` 和 `null`。
- 作用：决定数据可以进行什么操作。
- 解决的问题：避免把文字、数字和判断结果混淆。
- 语法：`const text = '文字'; const number = 18; const ok = true`。
- 案例：`typeof number` 可以查看类型。

### 5. `undefined` 和 `null`
- 是什么：`undefined` 表示存在但未赋值；`null` 表示明确没有值。
- 作用：表示数据暂时不存在或被主动清空。
- 解决的问题：区分未赋值和明确为空。
- 语法：`let user; const selected = null`。
- 案例：`if (user === undefined) console.log('还未加载')`。

### 6. 模板字符串
- 是什么：用反引号包裹、可以嵌入变量的字符串。
- 作用：清楚地拼接文字和变量。
- 解决的问题：减少多个 `+` 拼接造成的混乱。
- 语法：`` `你好，${name}` ``。
- 案例：`const message = `学生${name}的成绩是${score}分``。

### 7. 算术运算
- 是什么：对数字进行加、减、乘、除和取余。
- 作用：计算价格、数量、成绩和平均值。
- 解决的问题：让程序自动完成计算。
- 语法：`+`、`-`、`*`、`/`、`%`。
- 案例：`const total = price * count`。

### 8. 赋值运算
- 是什么：把数据或计算结果保存到变量。
- 作用：更新数量、总价和状态。
- 解决的问题：让变量可以根据旧值变化。
- 语法：`=`、`+=`、`-=`、`*=`、`/=`。
- 案例：`let score = 80; score += 5`。

### 9. 比较运算
- 是什么：比较两个值，结果是 `true` 或 `false`。
- 作用：判断大小、相等和不相等。
- 解决的问题：为条件判断提供结果。
- 语法：`>`、`<`、`>=`、`<=`、`===`、`!==`。
- 案例：`age >= 18` 判断是否成年。

### 10. 逻辑运算
- 是什么：组合或反转多个布尔条件。
- 作用：表达并且、或者、不是。
- 解决的问题：处理复杂条件。
- 语法：`&&` 表示并且，`||` 表示或者，`!` 表示取反。
- 案例：`age >= 18 && hasTicket`。

### 11. 递增、递减和三元运算
- 是什么：`++`、`--` 改变数字 1；三元运算在两个结果中选择一个。
- 作用：简化计数和简单条件结果。
- 解决的问题：减少重复的更新和短条件代码。
- 语法：`count++`；`condition ? valueA : valueB`。
- 案例：`const text = age >= 18 ? '成年' : '未成年'`。

### 12. `if`、`else if` 和 `else`
- 是什么：根据条件决定执行哪段代码。
- 作用：处理不同情况。
- 解决的问题：实现如果、否则和多个条件判断。
- 语法：`if (condition) {} else if (other) {} else {}`。
- 案例：根据 `score` 输出优秀、及格或不及格。

### 13. `switch`
- 是什么：根据一个值匹配多个固定情况。
- 作用：集中处理同一个变量的多个选项。
- 解决的问题：避免围绕同一个值写很多重复比较。
- 语法：`switch (value) { case option: break; default: }`。
- 案例：根据 `day` 输出星期几。

### 14. 数组
- 是什么：按顺序保存多个数据的结构。
- 作用：保存水果、成绩、学生等多条数据。
- 解决的问题：不用为每条数据单独创建变量。
- 语法：`const fruits = ['苹果', '香蕉']`；下标从 `0` 开始。
- 案例：`fruits[0]` 读取第一项，`fruits.length` 读取长度。

### 15. 数组方法
- 是什么：数组对象提供的数据处理功能。
- 作用：添加、删除、遍历、转换和筛选数组。
- 解决的问题：减少重复循环代码。
- 语法：`push()`、`pop()`、`includes()`、`forEach()`、`map()`、`filter()`、`find()`。
- 案例：`const passed = scores.filter(score => score >= 60)`。

### 16. 对象
- 是什么：用属性名和属性值描述一个事物。
- 作用：把同一个学生的姓名、年龄和成绩放在一起。
- 解决的问题：组织有关联的数据。
- 语法：`const student = { name: '小明', score: 86 }`。
- 案例：`student.score = 90; student.className = '一班'`。

### 17. 对象解构和展开
- 是什么：从对象中直接取值，或复制、合并对象。
- 作用：简化属性读取和生成新对象。
- 解决的问题：减少重复的点语法，避免直接破坏旧对象。
- 语法：`const { name } = student`；`const copy = { ...student }`。
- 案例：`const updated = { ...student, score: 95 }`。

### 18. 循环
- 是什么：让一段代码重复执行。
- 作用：依次处理数组或重复执行指定次数。
- 解决的问题：避免复制相同代码。
- 语法：`for (...) {}`；`for (const item of items) {}`；`while (condition) {}`。
- 案例：`for (const fruit of fruits) console.log(fruit)`。

### 19. `break` 和 `continue`
- 是什么：控制循环过程的关键字。
- 作用：`break` 结束循环，`continue` 跳过本次循环。
- 解决的问题：遇到特殊数据时提前停止或跳过。
- 语法：`if (condition) break`；`if (condition) continue`。
- 案例：发现不及格成绩时 `break` 停止检查。

### 20. 函数
- 是什么：可以重复调用的一段代码。
- 作用：接收参数、处理数据并返回结果。
- 解决的问题：避免重复编写逻辑，让代码按功能组织。
- 语法：`function add(a, b) { return a + b }`。
- 案例：`const result = add(10, 3)`。

### 21. 箭头函数
- 是什么：函数的简写形式。
- 作用：在数组方法和事件监听中传入简短函数。
- 解决的问题：减少回调函数的书写量。
- 语法：`const add = (a, b) => a + b`；`(参数) => { 函数体 }`。
- 案例：`button.addEventListener('click', () => { message.textContent = '点击了' })`。

### 22. 变量作用域
- 是什么：变量可以被访问的代码范围。
- 作用：控制变量可见范围。
- 解决的问题：避免不同代码区域之间的变量冲突。
- 语法：在函数或代码块中使用 `const`、`let`。
- 案例：函数内部的 `const message` 不能在函数外直接访问。

### 23. 错误和调试
- 是什么：错误是代码没有按预期执行；调试是寻找问题的过程。
- 作用：查看变量、流程和错误信息。
- 解决的问题：定位代码为什么没有得到预期结果。
- 语法：`console.log()`、`console.warn()`、`console.error()`。
- 案例：`console.log('当前成绩：', score)`。

### 24. `try...catch` 和 `throw`
- 是什么：捕获运行时错误，并可以主动抛出错误。
- 作用：显示友好提示或执行备用逻辑。
- 解决的问题：避免错误直接中断并让用户看不懂。
- 语法：`try { ... } catch (error) { ... }`；`throw new Error('说明')`。
- 案例：捕获 `JSON.parse()` 解析错误并显示 `error.message`。

### 25. DOM
- 是什么：浏览器把 HTML 解析成的对象模型。
- 作用：让 JavaScript 查找、读取、修改、创建和删除页面元素。
- 解决的问题：让固定 HTML 页面动态变化。
- 语法：`const element = document.querySelector('#message')`。
- 案例：`element.textContent = '新的文字'`。

### 26. `document` 和 DOM 树
- 是什么：`document` 代表当前 HTML 文档；DOM 树表示元素父子关系。
- 作用：从整个页面开始定位元素，并沿结构查找。
- 解决的问题：理解标签不是孤立的，而是有父子和兄弟关系。
- 语法：`document.body`；`element.parentElement`；`element.children`。
- 案例：通过 `child.parentElement` 找到子元素的父元素。

### 27. DOM 查找方法
- 是什么：从页面或元素内部取得 DOM 对象的方法。
- 作用：在修改前先找到目标元素。
- 解决的问题：JavaScript 必须先拿到 DOM 对象才能操作页面。
- 语法：`getElementById()`、`querySelector()`、`querySelectorAll()`。
- 案例：`const button = document.querySelector('#change-message-button')`。

### 28. DOM 对象的属性和方法
- 是什么：元素对象既有属性，也有方法。
- 作用：属性保存数据，方法执行操作。
- 解决的问题：区分直接读写和调用功能。
- 语法：`object.property`；`object.property = value`；`object.method()`。
- 案例：`element.textContent = '文字'` 是属性赋值，`element.remove()` 是方法调用。

### 29. `textContent`、`innerHTML` 和 `innerText`
- 是什么：读取或修改元素内容的属性。
- 作用：分别处理普通文字、HTML 内容和视觉文字。
- 解决的问题：根据内容类型选择正确方式，并避免不安全 HTML。
- 语法：`element.textContent`；`element.innerHTML`；`element.innerText`。
- 案例：用户输入显示到页面时使用 `textContent`。

### 30. 输入框的 `value`
- 是什么：表单控件当前填写的值。
- 作用：读取或修改 `input`、`textarea`、`select` 的数据。
- 解决的问题：普通元素的文字用 `textContent`，输入框当前内容要用 `value`。
- 语法：`const value = input.value`；`input.value = '新内容'`。
- 案例：当前文件点击“读取输入框”后显示 `domInput.value`。

### 31. `getAttribute()` 和 `setAttribute()`
- 是什么：DOM 标准提供的 HTML 属性读取和设置方法。
- 作用：读取、修改、添加或删除 `href`、`title` 等属性。
- 解决的问题：页面运行时需要动态改变链接地址、提示和自定义属性。
- 语法：`element.getAttribute('属性名')`；`element.setAttribute('属性名', '属性值')`。
- 案例：下面的 HTML 提供链接，JavaScript 读取旧 `href`，再把它改成 MDN 地址。

HTML：

```html
<a id="dom-link" href="https://example.com">示例链接</a>
<button id="change-attribute-button">修改链接</button>
<p id="attribute-output"></p>
```

JavaScript：

```javascript
const link = document.querySelector('#dom-link')
const button = document.querySelector('#change-attribute-button')
const output = document.querySelector('#attribute-output')

button.addEventListener('click', () => {
  const oldHref = link.getAttribute('href')

  link.setAttribute('href', 'https://developer.mozilla.org/')
  link.textContent = 'MDN 文档链接'

  const newHref = link.getAttribute('href')
  output.textContent = '旧地址：' + oldHref + '；新地址：' + newHref
})
```

点击按钮后，`getAttribute('href')` 读取地址，`setAttribute('href', '新地址')` 修改地址，`textContent` 修改链接文字。

### 32. `data-*` 和 `dataset`
- 是什么：HTML 保存自定义数据的属性格式和 JavaScript 读取对象。
- 作用：把编号、类型、状态等数据和页面元素关联。
- 解决的问题：不需要额外变量就能在元素上保存业务标记。
- 语法：`<p data-student-id='101'>`；`element.dataset.studentId`。
- 案例：当前文件通过 `classTarget.dataset.studentId` 读取学生编号。

### 33. `classList`
- 是什么：元素上的 `classList` 是一个专门管理 class 名称的对象。它不是普通字符串，而是可以调用方法的对象。
- 作用：添加、删除、切换、检查和替换 class。
- 解决的问题：按钮点击、菜单展开、选中状态变化时，只修改需要变化的 class，不必手动拼接整串 class。
- 语法：`element.classList.add('class名')`、`remove('class名')`、`toggle('class名')`、`contains('class名')`、`replace('旧class', '新class')`。
- 案例：当前文件通过按钮操作 `#class-target`，分别演示五个方法。

#### `classList` 和 `className` 的区别

```javascript
const element = document.querySelector('#class-target')

element.className = 'normal-text active'
// className 是一个字符串，可以一次性覆盖整个 class 属性。

element.classList.add('selected')
// classList 是对象，只添加 selected，不会覆盖原来的 class。
```

HTML 中的 `class` 是属性，JavaScript 中常用 `element.className` 读取它的完整字符串，常用 `element.classList` 对其中的单个 class 进行操作。

#### `add()` 添加 class

```javascript
element.classList.add('active')
```

如果 `active` 不存在，就添加它；如果已经存在，不会重复添加。

#### `remove()` 删除 class

```javascript
element.classList.remove('active')
```

删除 `active`。即使它原本不存在，也不会报错。

#### `toggle()` 切换 class

```javascript
const isActive = element.classList.toggle('active')
```

如果没有 `active` 就添加，如果已经有 `active` 就删除。返回值是布尔值：添加后返回 `true`，删除后返回 `false`。

也可以强制指定最终状态：

```javascript
element.classList.toggle('active', true)  // 强制添加
element.classList.toggle('active', false) // 强制删除
```

#### `contains()` 检查 class

```javascript
const hasActive = element.classList.contains('active')
```

检查元素是否有 `active`，返回 `true` 或 `false`，不会修改页面。

#### `replace()` 替换 class

```javascript
const replaced = element.classList.replace('normal-text', 'highlight-text')
```

把 `normal-text` 替换成 `highlight-text`。返回值表示旧 class 是否成功找到并替换。

#### 完整案例

```html
<p id="class-target" class="normal-text">学习状态</p>
<button id="add-class-button">添加 active</button>
<button id="remove-class-button">删除 active</button>
<button id="toggle-class-button">切换 active</button>
<button id="contains-class-button">检查 active</button>
<button id="replace-class-button">替换 class</button>
```

```javascript
const classTarget = document.querySelector('#class-target')

classTarget.classList.add('active')
classTarget.classList.remove('active')
const isActive = classTarget.classList.toggle('active')
const hasActive = classTarget.classList.contains('active')
const replaced = classTarget.classList.replace('normal-text', 'highlight-text')
```

这里 JavaScript 只负责改变 class，具体显示成什么样要由 CSS 选择器决定。例如 `.active { color: red; }` 会让拥有 `active` 的元素显示为红色。

注意：class 名称是一个个独立的名称，不能把带空格的整串内容当成一个 class 使用；需要添加多个 class 时，分别传入多个参数或分别调用方法。

### 34. `style`
- 是什么：元素的内联样式对象。
- 作用：通过 JavaScript 直接修改 CSS 样式。
- 解决的问题：根据操作或数据即时改变颜色、背景和间距。
- 语法：`element.style.color = 'red'`；CSS 短横线写成小驼峰。
- 案例：当前文件通过 `style.backgroundColor` 修改背景色。

### 35. 创建、添加和删除元素
- 是什么：DOM 在运行时创建和管理 HTML 元素的能力。
- 作用：动态生成列表、消息、表格行和其他内容。
- 解决的问题：页面内容数量不固定时不用把所有 HTML 写死。
- 语法：`document.createElement('li')`；`parent.appendChild(child)`；`child.remove()`。
- 案例：读取输入框后创建 `li`，写入文字并加入 `ul`。

### 36. 父子、兄弟和集合
- 是什么：DOM 元素之间的结构关系和多个元素组成的集合。
- 作用：沿 DOM 树查找相关元素，批量处理多个元素。
- 解决的问题：需要从子找到父，或同时处理多个匹配元素。
- 语法：`parentElement`、`children`、`nextElementSibling`、`NodeList.forEach()`。
- 案例：`list.lastElementChild` 找到并删除最后一项。

### 37. 事件监听语法
- 是什么：浏览器发生事情时执行指定函数的机制。
- 作用：响应点击、输入、提交、键盘和鼠标操作。
- 解决的问题：让用户操作真正改变页面。
- 语法：`element.addEventListener('事件名称', 事件处理函数)`。
- 案例：`element.addEventListener('click', () => { ... })`。
- 补充：`'click'` 是浏览器 DOM 标准规定的事件名称，不是自定义变量；`() => {}` 是箭头函数。

### 38. 事件对象和默认行为
- 是什么：事件处理函数接收的对象，记录本次事件信息。
- 作用：获取触发元素、当前监听元素并阻止默认行为。
- 解决的问题：表单默认刷新、事件代理和复杂交互需要事件详情。
- 语法：`event.target`、`event.currentTarget`、`event.preventDefault()`。
- 案例：提交事件中调用 `event.preventDefault()` 接管表单。

### 39. 表单和 `FormData`
- 是什么：表单收集用户数据，`FormData` 按 `name` 读取字段。
- 作用：统一读取姓名、成绩等表单数据。
- 解决的问题：不必分别处理每个输入框，便于校验和发送。
- 语法：`const data = new FormData(form)`；`data.get('name')`。
- 案例：当前文件用 `FormData(studentForm)` 读取姓名和成绩。

### 40. 表单校验和重置
- 是什么：判断输入是否有效，并把表单恢复初始状态。
- 作用：阻止空值、错误范围和无效数据。
- 解决的问题：避免错误数据进入程序。
- 语法：`if (...) throw new Error(...)`；`form.reset()`。
- 案例：成绩不在 0 到 100 时显示提交失败。

### 41. `DOMContentLoaded`
- 是什么：HTML 文档结构解析完成时触发的事件。
- 作用：确保查找 DOM 元素时结构已经准备好。
- 解决的问题：避免脚本执行太早导致查找不到元素。
- 语法：`document.addEventListener('DOMContentLoaded', () => {})`。
- 案例：页面准备完成后再查找 `#message`。

### 42. `fetch`
- 是什么：浏览器提供的数据请求函数。
- 作用：加载 JSON、接口和其他网络资源。
- 解决的问题：页面可以在运行时获得外部或动态数据。
- 语法：`const response = await fetch(url)`；`const data = await response.json()`。
- 案例：当前文件生成 JSON Blob，再用 `fetch()` 请求学生数据。

### 43. Promise
- 是什么：表示未来会成功或失败的异步任务。
- 作用：管理请求、定时器等异步结果。
- 解决的问题：网络数据不能立刻得到，需要先表示等待状态。
- 语法：`promise.then(success).catch(error).finally(cleanup)`。
- 案例：`loadStudents().then(students => console.log(students))`。

### 44. `async` 和 `await`
- 是什么：编写异步代码的语法。
- 作用：按接近同步代码的顺序等待异步结果。
- 解决的问题：多个 `then()` 嵌套时阅读困难。
- 语法：`async function load() { const data = await promise }`。
- 案例：当前文件用 `await fetch()` 和 `await response.json()` 加载数据。

### 45. `export`
- 是什么：把模块中的内容暴露给其他模块。
- 作用：共享函数、变量和 class。
- 解决的问题：代码拆分后仍能复用功能。
- 语法：`export function add() {}`；`export const name = 'JavaScript'`。
- 案例：模块导出 `createGreeting` 函数。

### 46. `import`
- 是什么：从其他模块引入已经导出的内容。
- 作用：使用其他文件的函数、变量和 class。
- 解决的问题：避免复制代码，建立清晰的文件依赖。
- 语法：`import { add } from './math.js'`；动态写法 `import(url)`。
- 案例：当前单文件点击模块按钮后动态导入 Blob 模块。

### 47. `class`
- 是什么：创建一类对象的模板。
- 作用：集中定义同类对象共有的属性和方法。
- 解决的问题：避免给大量相似对象重复写相同功能。
- 语法：`class Student { constructor(name) { this.name = name } }`。
- 案例：`const student = new Student('小明', 86)`。

### 48. `constructor`、`this` 和实例
- 是什么：`constructor` 初始化实例；`this` 表示当前实例；实例是根据 class 创建的对象。
- 作用：给每个对象保存自己的数据，并调用共有方法。
- 解决的问题：让多个学生对象拥有相同结构但保存不同数据。
- 语法：`new Student(name, score)`；`this.name = name`。
- 案例：当前文件创建 `Student` 实例并调用 `student.describe()`。

## 单文件演示对应关系

[javascript.html](./javascript.html) 当前按页面顺序演示：

1. 基础语法：变量、数据类型、条件、数组、对象、循环、函数。
2. DOM：查找元素、`textContent`、`value`、属性、`dataset`、`classList`、`style`、创建删除和父子关系。
3. 事件和表单：`click`、`submit`、`addEventListener`、`FormData` 和校验。
4. 异步：`fetch`、Promise、`async`、`await`。
5. 模块：`export` 和动态 `import`。
6. 对象模板：`class` 和 `Student` 实例。
7. 错误：`try...catch` 和 `console.error`。

这份总表已经覆盖当前 JavaScript 基础学习路线；之后学习 Vue 时，会把 DOM 的手动操作对照 Vue 的模板语法和响应式数据。
