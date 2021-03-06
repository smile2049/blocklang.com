# `PROJECT_DEPENDENCE` - 项目依赖

项目依赖可规范三类：API、Dev、Build。在此表中只存储 Dev 和 Build 依赖。

## 字段

| 字段名                    | 注释                    | 类型 | 长度 | 默认值 | 主键 | 可空 |
| ------------------------- | ----------------------- | ---- | ---- | ------ | ---- | ---- |
| dbid                      | 主键                    | int  |      |        | 是   | 否   |
| project_id                | 项目标识                | int  |      |        |      | 否   |
| component_repo_version_id | 组件仓库的版本标识      | int  |      |        |      | 否   |
| project_build_profile_id  | 项目构建的 Profile 标识 | int  |      |        |      | 是   |

## 约束

* 主键：`PK_PROJECT_DEPENDENCE`
* 外键：(*未设置*)`FK_PROJECT_DEPENDENCE_PROJECT`，`PROJECT_ID` 对应 `PROJECT` 表的 `dbid`
* 索引：`IDX_PROJECT_DEPENDENCE_ON_PROJECT_ID`(普通索引)，对应字段 `project_id`

## 说明

1. `component_repo_version_id` 只能存储组件仓库的版本号标识，不能存储 API 仓库的版本号标识
2. `project_build_profile_id` 仅用于组件仓库不是 dev 的情况，即只用于 build 专用的组件仓库；会为每个 build 组件仓库设置一个默认的 Profile
3. `project_build_profile_id` 如果组件仓库是 dev，则此字段的值为 null；如果组件仓库为 build，则此字段的值不能为 null，至少有一个默认的 Profile
