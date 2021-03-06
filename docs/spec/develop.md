# 项目研发

## 创建项目

1. 项目基本信息有效性校验
   1. 项目名是否有效：只允许字母、数字、中划线(-)、下划线(_)、点(.)等字符
   2. 项目名是否被占用：一个用户下的项目不能重名
2. 在 `PROJECT` 数据库表中存储项目基本信息
3. 在新建的项目下创建一个入口程序模块，名称为 `Main`
4. 在新建的项目下创建一个 `README.md` 文件
5. 在 `APP` 数据库表中存储 APP 基本信息，其中包括 `Registration Token`
6. 在 `BlockLang/gitRepo/{owner}/{project_name}` 文件夹下初始化一个 git 仓库
7. 在新建的 git 仓库下创建并提交 `Main.json` 文件
8. 在新建的 git 仓库下创建并提交 `README.md` 文件

## 浏览项目

项目状态分为：编辑 -> 提交

1. 在浏览模式下，可为文件名显示不同的颜色，了解模块的当前状态：新增，修改和已提交；
2. 在提交模式下，可查看所有修改的模块，并支持选定哪些模块提交；
3. 显示项目最近提交信息
4. 如果程序模块处于提交状态，则显示提交信息；如果模块已被修改（包含未提交的内容），也显示提交信息
5. 提供进入提交历史、发布版本和贡献者等页面的入口
6. 注意，要为三种状态选好不同的颜色

## 创建资源

git 仓库中的文件命名规范：

1. 页面：
   1. web: `{key}.page.web.json`
2. 页面模板：`{key}.page.tmpl.json`
3. 服务：`{key}.api.json`
4. 文件：`{name}`
5. 分组: `{key}`

## 项目部署

提示信息

```text
部署到您的主机(help icon) Linux/Windows

1. 下载并<安装> <blocklang-installer>
2. 执行 `./blocklang-installer register` 命令注册服务器
    1. 指定URL 为 `https://blocklang.com`
    2. 指定注册 token 为 `xxxxxdsx`
    3. 设置运行端口 <port>
3. 执行 `./blocklang-installer run --port <port>` 命令启动服务
4. 在浏览器中访问 http://<ip>:<port>
```

## 在页面中添加 UI 部件

1. 制定 UI 部件集成规范
2. 使用 dojo 开发 UI 部件，支持设计器版和发布版，支持一到多个设备
3. 将 UI 部件发布到 Block Lang 市场
4. 在项目的依赖文件中添加 Block Lang 市场中的 UI 部件
5. 在页面设计器中使用设计器版 UI 部件
6. 页面发布后，使用发布版 UI 部件

### UI 部件发布流程

支持基于 git 的源码托管平台发布。

1. 将 UI 部件项目托管到 github 或 gitee 等源代码托管平台上；
2. 在 Block Lang 市场中填写 git 仓库的 https 地址和版本号来发布 UI 部件；
3. 在发布时，对部件进行校验，确定是一个有效的可集成到 Block Lang 的 UI 部件库；
4. 校验通过后，在 Block Lang 中登记部件信息；
5. 发布完成。
