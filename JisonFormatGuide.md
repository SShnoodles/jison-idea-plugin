# Jison Format Guide

## 1. Basic Syntax

### 1.1 Key-Value Form

- Basic form: `key:value`
- Neither `key` nor `value` requires quotes
- Only the first `:` is used to split key and value
- `value` may be empty

```jison
key1:value1
key2:
key3:https://www.google.com
```

### 1.2 Newline Rules

- No commas are needed between items
- Each new config item must be on a new line

```jison
root:{
  key1:value1
  key2:value2
}
```

## 2. Comment Rules

### 2.1 Default Comment Flag

- Default comment flag is `#`
- After an unescaped comment flag, the rest of the line is treated as comment

```jison
# full-line comment
key1:value1 # trailing comment
```

### 2.2 Escaping Comment Flags

- If you need comment symbols in key/value text, escape them (for example: `\#`)

```jison
key2\#xxx:value2\#xxx
```

### 2.3 Switching Comment Flag Dynamically

- Use `JISON_COMMENT_FLAG` to switch the comment flag
- Supported values: `#` and `//`
- The directive line itself is not a config item

```jison
# default is #
root:{
  key1:value1 # comment
}

JISON_COMMENT_FLAG://
// comment flag is now //
other:{
  key1#xxx:value // comment
}

JISON_COMMENT_FLAG:#
# switched back to #
```

## 3. Structural Rules

- `{` must appear at end of line
- `}` must be on its own line

Correct:

```jison
root:{
  key1:value1
}
```

Incorrect:

```jison
root:{key1:value1}
```

## 4. Indentation and Whitespace

- Indentation is flexible (spaces and tabs are both acceptable)
- Blank lines are allowed
- Unlike YAML, strict indentation is not required

```jison
root:{
key1:value1
	# comment
  key2:value2

  key3:value3
}
```

## 5. Best Practices

- Keep one config item per line for readability and diagnostics
- Keep keys simple; put complex content (for example URLs) in values
- Put `JISON_COMMENT_FLAG` on a dedicated line when switching comment style


