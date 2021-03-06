# 校验项目名称

校验规则：

1. 项目名称不能为空
2. 项目名称只支持英文字母、数字、中划线(-)、下划线(_)、点(.)等字符
3. 登录用户下是否已存在该项目名

```text
POST /projects/check-name
```

## Parameters

Form data

| Name    | Type     | Description              |
| ------- | -------- | ------------------------ |
| `owner` | `string` | **Required**. 用户登录名 |
| `name`  | `string` | **Required**. 项目名称   |

## Response

校验未通过

```text
Status: 422 Unprocessable Entity
```

```json
{
    "errors": {
        "name": ["${filedErrorMessage}"]
    }
}
```

`filedErrorMessage` 的值为：

1. 当项目名称为空时返回 `项目名称不能为空`
2. 当项目名称中存在非法字符时返回 `只允许字母、数字、中划线(-)、下划线(_)、点(.)`
3. 登录用户下是否已存在该项目名时返回 `{owner}下已存在<strong>{name}</strong>项目`

校验通过

```text
Status: 200 OK
```

不返回任何内容。